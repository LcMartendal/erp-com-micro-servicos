package com.furb.erp_custos.service;

import com.furb.erp_custos.dto.CalculationRequestedDto;
import com.furb.erp_custos.dto.PeriodDto;
import com.furb.erp_custos.dto.ProductCalcRequestDto;
import com.furb.erp_custos.dto.ProductDto;
import com.furb.erp_custos.dto.ProductionAppointmentsDto;
import com.furb.erp_custos.dto.RubricDto;
import com.furb.erp_custos.entities.ProductEntity;
import com.furb.erp_custos.repositories.ProductRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RubricRepository rubricRepository;
    private final ProductionAppointmentService appointmentService;
    private final ProducerComponent producerComponent;

    /**
     * Busca os apontamentos do período e envia o pedido de cálculo dos custos ao erp-custos-calculo.
     * As taxas das rubricas precisam existir antes (o custo do produto usa as taxas).
     */
    @Transactional(readOnly = true)
    public CalculationRequestedDto requestCalculation(PeriodDto period) {
        List<ProductionAppointmentsDto> appointments = appointmentService.findByPeriod(period);

        List<RubricDto> rubrics = rubricRepository.findAll(Sort.by("code")).stream()
                .map(r -> new RubricDto(r.getId(), r.getCode(), r.getValue()))
                .toList();
        if (rubrics.isEmpty() || rubrics.stream().anyMatch(r -> r.value() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "As taxas das rubricas ainda não foram calculadas. Execute POST /rubrica antes de calcular os produtos");
        }

        Set<String> codes = appointments.stream()
                .map(ProductionAppointmentsDto::code)
                .collect(Collectors.toSet());
        List<ProductDto> products = productRepository.findByCodeIn(codes).stream()
                .map(p -> new ProductDto(p.getId(), p.getCode(), p.getValue()))
                .toList();
        if (products.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhum produto cadastrado para os apontamentos do período");
        }

        producerComponent.sendMessage(RabbitMQConstansts.RK_CALC_PRODUCT,
                new ProductCalcRequestDto(period.dataInicio(), period.dataFim(), rubrics, products, appointments));

        return new CalculationRequestedDto("Custos dos produtos serão calculados",
                period.dataInicio(), period.dataFim(), appointments.size());
    }

    /** Grava os custos calculados (idempotente: reaplicar o mesmo resultado não muda nada). */
    @Transactional
    public void persistData(List<ProductDto> productDtos) {
        if (productDtos == null || productDtos.isEmpty()) {
            return;
        }
        Map<UUID, BigDecimal> valuesById = new HashMap<>();
        for (ProductDto dto : productDtos) {
            valuesById.put(dto.id(), dto.value());
        }

        List<ProductEntity> entities = productRepository.findAllById(valuesById.keySet());
        for (ProductEntity entity : entities) {
            entity.setValue(valuesById.get(entity.getId()));
        }
        productRepository.saveAll(entities);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> findAll() {
        return productRepository.findAll(Sort.by("code")).stream()
                .map(p -> new ProductDto(p.getId(), p.getCode(), p.getValue()))
                .toList();
    }
}
