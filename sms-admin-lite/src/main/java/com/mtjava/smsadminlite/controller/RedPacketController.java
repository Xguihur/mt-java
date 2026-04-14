package com.mtjava.smsadminlite.controller;

import com.mtjava.smsadminlite.common.ApiResponse;
import com.mtjava.smsadminlite.dto.CreateRedPacketRequest;
import com.mtjava.smsadminlite.model.RedPacket;
import com.mtjava.smsadminlite.model.RedPacketRecord;
import com.mtjava.smsadminlite.service.RedPacketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 红包接口层。
 *
 * POST /api/red-packets              创建红包
 * GET  /api/red-packets/{id}         查询红包详情
 * POST /api/red-packets/{id}/grab    抢红包
 * GET  /api/red-packets/{id}/records 查看抢包记录
 */
@RestController
@RequestMapping("/api/red-packets")
public class RedPacketController {

    private final RedPacketService redPacketService;

    public RedPacketController(RedPacketService redPacketService) {
        this.redPacketService = redPacketService;
    }

    @PostMapping
    public ApiResponse<RedPacket> create(@Valid @RequestBody CreateRedPacketRequest request) {
        return ApiResponse.success("红包创建成功", redPacketService.createRedPacket(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<RedPacket> detail(@PathVariable Long id) {
        return ApiResponse.success(redPacketService.getRedPacket(id));
    }

    /**
     * 抢红包。
     *
     * userId 通过请求参数传入（真实项目中通常从 JWT Token 解析，这里简化处理）。
     * 示例：POST /api/red-packets/1/grab?userId=2
     */
    @PostMapping("/{id}/grab")
    public ApiResponse<RedPacketRecord> grab(@PathVariable Long id,
                                             @RequestParam Long userId) {
        return ApiResponse.success("恭喜抢到红包", redPacketService.grabRedPacket(id, userId));
    }

    @GetMapping("/{id}/records")
    public ApiResponse<List<RedPacketRecord>> records(@PathVariable Long id) {
        return ApiResponse.success(redPacketService.listRecords(id));
    }
}
