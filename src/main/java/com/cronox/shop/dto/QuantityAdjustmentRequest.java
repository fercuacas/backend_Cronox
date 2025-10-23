package com.cronox.shop.dto;

import jakarta.validation.constraints.NotNull;

public class QuantityAdjustmentRequest {

    @NotNull
    private Integer delta;

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }
}
