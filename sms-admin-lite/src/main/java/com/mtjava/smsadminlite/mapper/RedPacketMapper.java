package com.mtjava.smsadminlite.mapper;

import com.mtjava.smsadminlite.model.RedPacket;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 红包 Mapper。
 */
@Mapper
public interface RedPacketMapper {

    /** 插入红包，id 由数据库自增后回填。 */
    void insert(RedPacket redPacket);

    RedPacket selectById(@Param("id") Long id);

    /**
     * 直接用 SQL 原子更新剩余数量，比"先查再改再存"更安全。
     * UPDATE red_packet SET remain_count = remain_count - 1,
     *   remain_amount_cents = remain_amount_cents - #{amountCents}
     * WHERE id = #{id}
     */
    void decrementRemain(@Param("id") Long id, @Param("amountCents") int amountCents);
}
