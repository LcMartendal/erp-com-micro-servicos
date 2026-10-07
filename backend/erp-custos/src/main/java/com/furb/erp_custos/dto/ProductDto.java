package com.furb.erp_custos.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto (
        UUID id,
        String code,
        BigDecimal value
) implements Serializable {}
