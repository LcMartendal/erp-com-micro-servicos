package com.furb.erp_custos.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductionAppointmentsDto(
        UUID id,
        String code,
        BigDecimal totalQuantity,
        Integer totalHours,
        LocalDateTime dateTime
) implements Serializable {}
