package com.furb.erp_custos_calculo.services;

import com.furb.erp_custos_calculo.dtos.ProductCalcRequestDto;
import com.furb.erp_custos_calculo.dtos.ProductDto;
import com.furb.erp_custos_calculo.dtos.ProductionAppointmentsDto;
import com.furb.erp_custos_calculo.dtos.RubricDto;
import com.furb.erp_custos_calculo.utils.constants.RabbitMQConstansts;
import com.furb.erp_custos_calculo.utils.producer.ProducerComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductValueCalculatorService {

    private static final int SCALE = 4;

    @Autowired
    private ProducerComponent producerComponent;

    /**
     * Custo por unidade = (horas do produto no período x soma das taxas das rubricas) / quantidade do produto.
     * Produtos sem quantidade produzida no período não têm custo unitário e ficam fora do resultado.
     */
    public void calculate(ProductCalcRequestDto request) {
        BigDecimal hourlyRate = request.rubrics().stream()
                .map(RubricDto::value)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, ProductDto> productsByCode = request.products().stream()
                .collect(Collectors.toMap(ProductDto::code, Function.identity(), (a, b) -> a));
        Map<String, List<ProductionAppointmentsDto>> appointmentsByCode = request.appointments().stream()
                .collect(Collectors.groupingBy(ProductionAppointmentsDto::code));

        List<ProductDto> results = appointmentsByCode.entrySet().stream()
                .filter(entry -> productsByCode.containsKey(entry.getKey()))
                .map(entry -> toResult(productsByCode.get(entry.getKey()), entry.getValue(), hourlyRate))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProductDto::code))
                .toList();

        log.info("Custos de {} produtos calculados ({} apontamentos, taxa total {} R$/h, período {} a {})",
                results.size(), request.appointments().size(), hourlyRate, request.dataInicio(), request.dataFim());
        producerComponent.sendMessage(RabbitMQConstansts.RK_RESULT_PRODUCT, results);
    }

    private ProductDto toResult(ProductDto product, List<ProductionAppointmentsDto> appointments, BigDecimal hourlyRate) {
        long hours = appointments.stream()
                .map(ProductionAppointmentsDto::totalHours)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();
        BigDecimal quantity = appointments.stream()
                .map(ProductionAppointmentsDto::totalQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (quantity.signum() <= 0) {
            log.warn("Produto {} sem quantidade produzida no período, custo não calculado", product.code());
            return null;
        }
        BigDecimal unitCost = hourlyRate.multiply(BigDecimal.valueOf(hours)).divide(quantity, SCALE, RoundingMode.HALF_UP);
        return new ProductDto(product.id(), product.code(), unitCost);
    }

}
