package com.furb.erp_custos.dto;

import java.time.LocalDate;
import java.util.List;

/** Mensagem CalcularCustoProduto: taxas das rubricas + produtos + apontamentos do período. */
public record ProductCalcRequestDto(
        LocalDate dataInicio,
        LocalDate dataFim,
        List<RubricDto> rubrics,
        List<ProductDto> products,
        List<ProductionAppointmentsDto> appointments
) {}
