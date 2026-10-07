package com.furb.erp_custos_calculo.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record RubricInputDto(
        UUID rubricId,
        String code,
        BigDecimal totalValue
) {}
