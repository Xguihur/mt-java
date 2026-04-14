package com.mtjava.smsadminlite.service;

import com.mtjava.smsadminlite.dto.CreateRedPacketRequest;
import com.mtjava.smsadminlite.model.RedPacket;
import com.mtjava.smsadminlite.model.RedPacketRecord;

import java.util.List;

/**
 * 红包业务层接口。
 */
public interface RedPacketService {

    /** 创建红包，拆分金额并写入 Redis。 */
    RedPacket createRedPacket(CreateRedPacketRequest request);

    /** 抢红包，返回本次抢到的记录。 */
    RedPacketRecord grabRedPacket(Long redPacketId, Long userId);

    /** 查询红包详情。 */
    RedPacket getRedPacket(Long id);

    /** 查询某个红包的抢包记录列表。 */
    List<RedPacketRecord> listRecords(Long redPacketId);
}
