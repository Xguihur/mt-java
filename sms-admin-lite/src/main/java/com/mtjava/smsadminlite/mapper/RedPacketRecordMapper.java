package com.mtjava.smsadminlite.mapper;

import com.mtjava.smsadminlite.model.RedPacketRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 抢红包记录 Mapper。
 */
@Mapper
public interface RedPacketRecordMapper {

    /** 插入抢包记录，id 由数据库自增后回填。 */
    void insert(RedPacketRecord record);

    /** 查询某红包下的所有抢包记录，按时间倒序。 */
    List<RedPacketRecord> selectByRedPacketId(@Param("redPacketId") Long redPacketId);
}
