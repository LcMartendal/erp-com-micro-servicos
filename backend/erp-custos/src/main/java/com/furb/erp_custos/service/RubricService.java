package com.furb.erp_custos.service;

import com.furb.erp_custos.dto.CalculationRequestedDto;
import com.furb.erp_custos.dto.PeriodDto;
import com.furb.erp_custos.dto.ProductionAppointmentsDto;
import com.furb.erp_custos.dto.RubricCalcRequestDto;
import com.furb.erp_custos.dto.RubricDto;
import com.furb.erp_custos.dto.RubricInputDto;
import com.furb.erp_custos.entities.RubricEntity;
import com.furb.erp_custos.repositories.RubricRepository;
import com.furb.erp_custos.utils.rabbitmqUtils.ProducerComponent;
import com.furb.erp_custos.utils.rabbitmqUtils.RabbitMQConstansts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RubricService {

    private final RubricRepository rubricRepository;
    private final ProductionAppointmentService appointmentService;
    private final ProducerComponent producerComponent;

    /** Busca os apontamentos do período e envia o pedido de cálculo das taxas ao erp-custos-calculo. */
    @Transactional(readOnly = true)
    public CalculationRequestedDto requestCalculation(PeriodDto period) {
        List<ProductionAppointmentsDto> appointments = appointmentService.findByPeriod(period);

        List<RubricInputDto> rubrics = rubricRepository.findAllWithAccount().stream()
                .map(r -> new RubricInputDto(r.getId(), r.getCode(), r.getAccount().getTotalValue()))
                .toList();
        if (rubrics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nenhuma rubrica com conta vinculada");
        }

        producerComponent.sendMessage(RabbitMQConstansts.RK_CALC_RUBRIC,
                new RubricCalcRequestDto(period.dataInicio(), period.dataFim(), rubrics, appointments));

        return new CalculationRequestedDto("Taxas das rubricas serão calculadas",
                period.dataInicio(), period.dataFim(), appointments.size());
    }

    /** Grava as taxas calculadas (idempotente: reaplicar o mesmo resultado não muda nada). */
    @Transactional
    public void persistData(List<RubricDto> rubricDtos) {
        if (rubricDtos == null || rubricDtos.isEmpty()) {
            return;
        }
        Map<UUID, BigDecimal> valuesById = new HashMap<>();
        for (RubricDto dto : rubricDtos) {
            valuesById.put(dto.id(), dto.value());
        }

        List<RubricEntity> entities = rubricRepository.findAllById(valuesById.keySet());
        for (RubricEntity entity : entities) {
            entity.setValue(valuesById.get(entity.getId()));
        }
        rubricRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<RubricDto> findAll() {
        return rubricRepository.findAll(Sort.by("code")).stream()
                .map(r -> new RubricDto(r.getId(), r.getCode(), r.getValue()))
                .toList();
    }
}
