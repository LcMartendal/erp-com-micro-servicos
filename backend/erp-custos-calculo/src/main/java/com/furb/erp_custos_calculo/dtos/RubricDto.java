package com.furb.erp_custos_calculo.dtos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record RubricDto (
    UUID id,
    String code,
    BigDecimal value
) implements Serializable {}
