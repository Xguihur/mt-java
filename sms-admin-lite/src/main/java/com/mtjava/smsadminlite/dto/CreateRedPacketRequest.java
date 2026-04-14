package com.mtjava.smsadminlite.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建红包的请求参数。
 *
 * totalAmountCents：红包总金额，单位"分"。100 = 1元。
 * totalCount：红包个数，必须 >= 1。
 * 校验规则：总金额 >= 个数（保证每个红包至少有 1 分）。
 */
public class CreateRedPacketRequest {

    @NotBlank(message = "红包标题不能为空")
    private String title;

    @NotNull(message = "红包金额不能为空")
    @Min(value = 1, message = "红包金额至少 1 分")
    private Integer totalAmountCents;

    @NotNull(message = "红包个数不能为空")
    @Min(value = 1, message = "红包个数至少 1 个")
    private Integer totalCount;

    public CreateRedPacketRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTotalAmountCents() {
        return totalAmountCents;
    }

    public void setTotalAmountCents(Integer totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}
