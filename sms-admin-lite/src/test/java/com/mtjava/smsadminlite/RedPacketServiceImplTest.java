package com.mtjava.smsadminlite;

import com.mtjava.smsadminlite.mapper.RedPacketMapper;
import com.mtjava.smsadminlite.mapper.RedPacketRecordMapper;
import com.mtjava.smsadminlite.mapper.UserMapper;
import com.mtjava.smsadminlite.model.RedPacketRecord;
import com.mtjava.smsadminlite.model.User;
import com.mtjava.smsadminlite.service.impl.RedPacketServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedPacketServiceImplTest {

    @Mock
    private RedPacketMapper redPacketMapper;

    @Mock
    private RedPacketRecordMapper recordMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RedPacketServiceImpl redPacketService;

    @Test
    void shouldRejectDuplicateGrabWhenLuaReturnsDuplicate() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("rp:1:grabbed", "rp:1:amounts")), eq("2")))
                .thenReturn("DUPLICATE");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> redPacketService.grabRedPacket(1L, 2L)
        );

        assertEquals("您已经抢过这个红包了", ex.getMessage());
        verify(recordMapper, never()).insert(any());
        verify(redPacketMapper, never()).decrementRemain(anyLong(), anyInt());
    }

    @Test
    void shouldRejectEmptyRedPacketWhenLuaReturnsEmpty() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("rp:1:grabbed", "rp:1:amounts")), eq("2")))
                .thenReturn("EMPTY");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> redPacketService.grabRedPacket(1L, 2L)
        );

        assertEquals("手慢了，红包已被抢完", ex.getMessage());
        verify(recordMapper, never()).insert(any());
        verify(redPacketMapper, never()).decrementRemain(anyLong(), anyInt());
    }

    @Test
    void shouldPersistRecordWhenLuaReturnsAmount() {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("rp:1:grabbed", "rp:1:amounts")), eq("2")))
                .thenReturn("88");
        when(userMapper.selectById(2L)).thenReturn(new User(2L, "小龙", "13800000000", null));

        RedPacketRecord record = redPacketService.grabRedPacket(1L, 2L);

        assertEquals(88, record.getAmountCents());
        assertEquals("小龙", record.getUserName());

        ArgumentCaptor<RedPacketRecord> captor = ArgumentCaptor.forClass(RedPacketRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals(88, captor.getValue().getAmountCents());
        assertEquals(1L, captor.getValue().getRedPacketId());
        assertEquals(2L, captor.getValue().getUserId());
        verify(redPacketMapper).decrementRemain(1L, 88);
    }
}
