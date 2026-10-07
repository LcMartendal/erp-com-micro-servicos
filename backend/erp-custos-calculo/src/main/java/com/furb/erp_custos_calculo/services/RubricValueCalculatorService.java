package com.furb.erp_custos_calculo.services;

import com.furb.erp_custos_calculo.dtos.ProductionAppointmentsDto;
import com.furb.erp_custos_calculo.dtos.RubricCalcRequestDto;
import com.furb.erp_custos_calculo.dtos.RubricDto;
import com.furb.erp_custos_calculo.utils.constants.RabbitMQConstansts;
import com.furb.erp_custos_calculo.utils.producer.ProducerComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class RubricValueCalculatorService {

    private static final int SCALE = 4;

    @Autowired
    private ProducerComponent producerComponent;

    /** Taxa da rubrica (R$/h) = total da conta / horas-máquina de todos os apontamentos do período. */
    public void calculate(RubricCalcRequestDto request) {
        long totalHours = request.appointments().stream()
                .map(ProductionAppointmentsDto::totalHours)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        if (totalHours <= 0) {
            throw new IllegalStateException("Período " + request.dataInicio() + " a " + request.dataFim()
                    + " sem horas-máquina apontadas");
        }

        BigDecimal hours = BigDecimal.valueOf(totalHours);
        List<RubricDto> rubrics = request.rubrics().stream()
                .map(r -> new RubricDto(r.rubricId(), r.code(), r.totalValue().divide(hours, SCALE, RoundingMode.HALF_UP)))
                .toList();

        log.info("Taxas de {} rubricas calculadas ({} apontamentos, {} h-máquina, período {} a {})",
                rubrics.size(), request.appointments().size(), totalHours, request.dataInicio(), request.dataFim());
        producerComponent.sendMessage(RabbitMQConstansts.RK_RESULT_RUBRIC, rubrics);
    }

}
