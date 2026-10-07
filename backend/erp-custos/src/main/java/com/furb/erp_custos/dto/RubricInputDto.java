package com.furb.erp_custos.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Rubrica + total da conta vinculada, enviados ao erp-custos-calculo para gerar a taxa. */
public record RubricInputDto(
        UUID rubricId,
        String code,
        BigDecimal totalValue
) {}
