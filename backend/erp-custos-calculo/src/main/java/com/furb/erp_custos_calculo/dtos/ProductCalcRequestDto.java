package com.furb.erp_custos_calculo.dtos;

import java.time.LocalDate;
import java.util.List;

public record ProductCalcRequestDto(
        LocalDate dataInicio,
        LocalDate dataFim,
        List<RubricDto> rubrics,
        List<ProductDto> products,
        List<ProductionAppointmentsDto> appointments
) {}
