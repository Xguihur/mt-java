package com.mtjava.smsadminlite.service.impl;

import com.mtjava.smsadminlite.common.BusinessException;
import com.mtjava.smsadminlite.dto.CreateRedPacketRequest;
import com.mtjava.smsadminlite.mapper.RedPacketMapper;
import com.mtjava.smsadminlite.mapper.RedPacketRecordMapper;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.model.RedPacket;
import com.mtjava.smsadminlite.model.RedPacketRecord;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.RedPacketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class RedPacketServiceImpl implements RedPacketService {

    /**
     * Redis Key 说明：
     *   rp:{id}:amounts  → List，存放每个红包金额字符串（单位：分），LPOP 原子弹出
     *   rp:{id}:grabbed  → Set，存放已抢过的 userId，SADD 防重复
     */
    private static final String KEY_AMOUNTS = "rp:%d:amounts";
    private static final String KEY_GRABBED = "rp:%d:grabbed";
    /** Lua 返回码：当前用户已经抢过，Java 层据此抛出"不可重复抢"提示。 */
    private static final String LUA_DUPLICATE = "DUPLICATE";
    /** Lua 返回码：金额列表已空，Java 层据此抛出"红包已抢完"提示。 */
    private static final String LUA_EMPTY = "EMPTY";
    private static final DefaultRedisScript<String> GRAB_SCRIPT = new DefaultRedisScript<>();

    static {
        /*
         * 把"判重 + 抢金额 + 抢完回滚资格"放进一段 Lua 脚本里，
         * 让 Redis 在服务端一次性执行完整流程。
         *
         * KEYS[1] = rp:{id}:grabbed，已抢用户 Set
         * KEYS[2] = rp:{id}:amounts，待抢金额 List
         * ARGV[1] = 当前用户 userId
         *
         * 逐行含义：
         * 1. SADD grabbed userId
         *    - 返回 1：第一次抢，可以继续
         *    - 返回 0：已经抢过，直接返回 DUPLICATE
         * 2. LPOP amounts
         *    - 弹出一个金额，谁先执行到这里谁先拿到
         *    - 如果返回 nil，说明红包已经抢完
         * 3. 如果抢完，则 SREM 回滚前面写入的抢资格记录
         *    - 否则这个用户会被错误地标记为"已经抢过"
         * 4. 返回金额字符串给 Java 层继续落库
         *
         * 对比旧版两步式实现：
         * - 旧版：Java 先调一次 SADD，再调一次 LPOP
         * - 新版：Java 只发一次 EVAL，Redis 内部把两步连续做完
         *
         * 这样做的价值不是"写法更炫"，而是把多个相关动作收敛成
         * 一个不可被其他并发请求插入打断的原子过程。
         */
        GRAB_SCRIPT.setScriptText(
                "local added = redis.call('SADD', KEYS[1], ARGV[1])\n"
                        + "if added == 0 then\n"
                        + "    return '" + LUA_DUPLICATE + "'\n"
                        + "end\n"
                        + "local amount = redis.call('LPOP', KEYS[2])\n"
                        + "if not amount then\n"
                        + "    redis.call('SREM', KEYS[1], ARGV[1])\n"
                        + "    return '" + LUA_EMPTY + "'\n"
                        + "end\n"
                        + "return amount"
        );
        GRAB_SCRIPT.setResultType(String.class);
    }

    private final RedPacketMapper       redPacketMapper;
    private final RedPacketRecordMapper recordMapper;
    private final UserMapper            userMapper;
    private final StringRedisTemplate   redisTemplate;
    private final Random                random = new Random();

    public RedPacketServiceImpl(RedPacketMapper redPacketMapper,
                                RedPacketRecordMapper recordMapper,
                                UserMapper userMapper,
                                StringRedisTemplate redisTemplate) {
        this.redPacketMapper = redPacketMapper;
        this.recordMapper    = recordMapper;
        this.userMapper      = userMapper;
        this.redisTemplate   = redisTemplate;
    }

    @Override
    @Transactional
    public RedPacket createRedPacket(CreateRedPacketRequest request) {
        int total = request.getTotalAmountCents();
        int count = request.getTotalCount();
        log.info("开始创建红包，title={}, totalAmountCents={}, totalCount={}",
                request.getTitle(), total, count);

        if (total < count) {
            log.warn("创建红包失败，总金额小于红包个数，title={}, totalAmountCents={}, totalCount={}",
                    request.getTitle(), total, count);
            throw BusinessException.badRequest("红包总金额（分）不能少于红包个数，每个红包至少 1 分");
        }

        // 1. 二倍均值法拆金额
        List<Integer> amounts = splitAmounts(total, count);
        log.debug("红包拆分完成，title={}, amounts={}", request.getTitle(), amounts);

        // 2. 写入 MySQL
        RedPacket redPacket = new RedPacket();
        redPacket.setTitle(request.getTitle());
        redPacket.setTotalAmountCents(total);
        redPacket.setTotalCount(count);
        redPacket.setRemainAmountCents(total);
        redPacket.setRemainCount(count);
        redPacket.setCreatedAt(LocalDateTime.now());
        redPacketMapper.insert(redPacket); // insert 后 id 被回填

        // 3. 把拆好的金额列表写入 Redis List，后续 LPOP 原子获取
        String amountsKey = String.format(KEY_AMOUNTS, redPacket.getId());
        String[] amountArr = amounts.stream().map(String::valueOf).toArray(String[]::new);
        redisTemplate.opsForList().rightPushAll(amountsKey, amountArr);
        log.info("创建红包成功，redPacketId={}, amountsKey={}, totalCount={}",
                redPacket.getId(), amountsKey, count);

        return redPacket;
    }

    @Override
    @Transactional
    public RedPacketRecord grabRedPacket(Long redPacketId, Long userId) {
        String grabbedKey = String.format(KEY_GRABBED, redPacketId);
        String amountsKey = String.format(KEY_AMOUNTS, redPacketId);
        log.info("开始抢红包，redPacketId={}, userId={}", redPacketId, userId);

        // 1. 用 Lua 脚本把"判重 + 弹金额 + 抢完回滚资格"合成一次 Redis 原子执行
        //    返回 DUPLICATE：已抢过
        //    返回 EMPTY：红包已空
        //    返回数字字符串：抢到的金额（单位：分）
        String grabResult = redisTemplate.execute(
                GRAB_SCRIPT,
                List.of(grabbedKey, amountsKey),
                userId.toString()
        );
        if (grabResult == null) {
            log.error("Redis 抢红包脚本返回空结果，redPacketId={}, userId={}", redPacketId, userId);
            throw new IllegalStateException("Redis 抢红包脚本执行失败");
        }
        if (LUA_DUPLICATE.equals(grabResult)) {
            log.warn("抢红包失败，用户重复抢红包，redPacketId={}, userId={}", redPacketId, userId);
            throw BusinessException.conflict("您已经抢过这个红包了");
        }
        if (LUA_EMPTY.equals(grabResult)) {
            log.warn("抢红包失败，红包已抢完，redPacketId={}, userId={}", redPacketId, userId);
            throw BusinessException.conflict("手慢了，红包已被抢完");
        }

        int amountCents = Integer.parseInt(grabResult);
        log.info("Redis 抢红包成功，redPacketId={}, userId={}, amountCents={}",
                redPacketId, userId, amountCents);

        // 2. 查用户（记录里冗余用户名，方便展示）
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("抢红包失败，用户不存在，redPacketId={}, userId={}", redPacketId, userId);
            throw BusinessException.notFound("用户不存在，userId=" + userId);
        }

        // 3. 写抢包记录到 MySQL
        RedPacketRecord record = new RedPacketRecord();
        record.setRedPacketId(redPacketId);
        record.setUserId(userId);
        record.setUserName(user.getName());
        record.setAmountCents(amountCents);
        record.setGrabbedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 4. 原子更新 MySQL 里的剩余数量（展示用）
        redPacketMapper.decrementRemain(redPacketId, amountCents);
        log.info("抢红包落库完成，recordId={}, redPacketId={}, userId={}, amountCents={}",
                record.getId(), redPacketId, userId, amountCents);

        return record;
    }

    @Override
    public RedPacket getRedPacket(Long id) {
        RedPacket rp = redPacketMapper.selectById(id);
        if (rp == null) {
            log.warn("查询红包失败，红包不存在，redPacketId={}", id);
            throw BusinessException.notFound("红包不存在，id=" + id);
        }
        log.info("查询红包详情成功，redPacketId={}, remainCount={}, remainAmountCents={}",
                id, rp.getRemainCount(), rp.getRemainAmountCents());
        return rp;
    }

    @Override
    public List<RedPacketRecord> listRecords(Long redPacketId) {
        List<RedPacketRecord> records = recordMapper.selectByRedPacketId(redPacketId);
        log.info("查询红包记录完成，redPacketId={}, recordCount={}", redPacketId, records.size());
        return records;
    }

    /**
     * 二倍均值法拆红包。
     *
     * 每次从剩余金额里随机取 [1, 剩余均值×2] 范围内的值，
     * 保证后面每人至少还能拿到 1 分。最后一份直接取剩余所有金额。
     */
    private List<Integer> splitAmounts(int totalCents, int count) {
        List<Integer> result = new ArrayList<>(count);
        int remain = totalCents;
        for (int i = count; i > 1; i--) {
            int max    = (remain / i) * 2;
            int amount = Math.max(1, random.nextInt(max) + 1);
            amount = Math.min(amount, remain - (i - 1));
            result.add(amount);
            remain -= amount;
        }
        result.add(remain);
        Collections.shuffle(result);
        return result;
    }
}
