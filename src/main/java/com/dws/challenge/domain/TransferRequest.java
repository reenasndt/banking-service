package com.dws.challenge.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull
    private String accountFromId;

    @NotNull
    private String accountToId;

    @NotNull
    @Min(value = 0, message = "Transfer amount must be positive.")
    private BigDecimal amount;
}

