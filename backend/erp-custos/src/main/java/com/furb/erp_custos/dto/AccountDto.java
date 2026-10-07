package com.furb.erp_custos.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDto(
    UUID id,
    String name,
    BigDecimal totalValue
) {}
