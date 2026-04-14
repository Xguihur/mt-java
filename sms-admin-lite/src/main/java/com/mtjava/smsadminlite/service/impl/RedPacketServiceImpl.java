package com.mtjava.smsadminlite.service.impl;

import com.mtjava.smsadminlite.dto.CreateRedPacketRequest;
import com.mtjava.smsadminlite.mapper.RedPacketMapper;
import com.mtjava.smsadminlite.mapper.RedPacketRecordMapper;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.model.RedPacket;
import com.mtjava.smsadminlite.model.RedPacketRecord;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.RedPacketService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class RedPacketServiceImpl implements RedPacketService {

    /**
     * Redis Key 说明：
     *   rp:{id}:amounts  → List，存放每个红包金额字符串（单位：分），LPOP 原子弹出
     *   rp:{id}:grabbed  → Set，存放已抢过的 userId，SADD 防重复
     */
    private static final String KEY_AMOUNTS = "rp:%d:amounts";
    private static final String KEY_GRABBED = "rp:%d:grabbed";

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

        if (total < count) {
            throw new IllegalArgumentException("红包总金额（分）不能少于红包个数，每个红包至少 1 分");
        }

        // 1. 二倍均值法拆金额
        List<Integer> amounts = splitAmounts(total, count);

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

        return redPacket;
    }

    @Override
    @Transactional
    public RedPacketRecord grabRedPacket(Long redPacketId, Long userId) {
        String grabbedKey = String.format(KEY_GRABBED, redPacketId);
        String amountsKey = String.format(KEY_AMOUNTS, redPacketId);

        // 1. SADD：把 userId 加入"已抢集合"
        //    返回 1 = 第一次抢（成功），返回 0 = 已经抢过（拦截）
        Long added = redisTemplate.opsForSet().add(grabbedKey, userId.toString());
        if (added == null || added == 0L) {
            throw new IllegalArgumentException("您已经抢过这个红包了");
        }

        // 2. LPOP：原子弹出一个金额，天然防超发
        String amountStr = redisTemplate.opsForList().leftPop(amountsKey);
        if (amountStr == null) {
            // 红包已抢完，把刚加的 userId 从 Set 里撤回，下次请求能看到"已抢完"提示
            redisTemplate.opsForSet().remove(grabbedKey, userId.toString());
            throw new IllegalArgumentException("手慢了，红包已被抢完");
        }

        int amountCents = Integer.parseInt(amountStr);

        // 3. 查用户（记录里冗余用户名，方便展示）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在，userId=" + userId);
        }

        // 4. 写抢包记录到 MySQL
        RedPacketRecord record = new RedPacketRecord();
        record.setRedPacketId(redPacketId);
        record.setUserId(userId);
        record.setUserName(user.getName());
        record.setAmountCents(amountCents);
        record.setGrabbedAt(LocalDateTime.now());
        recordMapper.insert(record);

        // 5. 原子更新 MySQL 里的剩余数量（展示用）
        redPacketMapper.decrementRemain(redPacketId, amountCents);

        return record;
    }

    @Override
    public RedPacket getRedPacket(Long id) {
        RedPacket rp = redPacketMapper.selectById(id);
        if (rp == null) {
            throw new IllegalArgumentException("红包不存在，id=" + id);
        }
        return rp;
    }

    @Override
    public List<RedPacketRecord> listRecords(Long redPacketId) {
        return recordMapper.selectByRedPacketId(redPacketId);
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
