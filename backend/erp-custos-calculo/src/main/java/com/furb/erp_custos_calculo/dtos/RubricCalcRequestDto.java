package com.furb.erp_custos_calculo.dtos;

import java.time.LocalDate;
import java.util.List;

public record RubricCalcRequestDto(
        LocalDate dataInicio,
        LocalDate dataFim,
        List<RubricInputDto> rubrics,
        List<ProductionAppointmentsDto> appointments
) {}
