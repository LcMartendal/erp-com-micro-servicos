package com.furb.erp_custos_calculo.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDto(
    UUID id,
    String name,
    BigDecimal totalValue
) {}
