package com.furb.erp_custos.dto;

import java.time.LocalDate;
import java.util.List;

/** Mensagem CalcularTaxaRubrica: rubricas com o total da conta + apontamentos do período. */
public record RubricCalcRequestDto(
        LocalDate dataInicio,
        LocalDate dataFim,
        List<RubricInputDto> rubrics,
        List<ProductionAppointmentsDto> appointments
) {}
