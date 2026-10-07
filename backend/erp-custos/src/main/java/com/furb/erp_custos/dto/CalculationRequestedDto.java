package com.furb.erp_custos.dto;

import java.time.LocalDate;

/** Resposta (202) dos endpoints que disparam um cálculo. */
public record CalculationRequestedDto(
        String message,
        LocalDate dataInicio,
        LocalDate dataFim,
        int appointmentsSent
) {}
