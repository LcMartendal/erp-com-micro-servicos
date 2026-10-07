package com.furb.erp_custos.dto;

import java.time.LocalDate;

/** Corpo das requisições de cálculo: apenas o período a considerar. */
public record PeriodDto(
        LocalDate dataInicio,
        LocalDate dataFim
) {}
