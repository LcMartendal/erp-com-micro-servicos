package com.furb.erp_custos_calculo.utils.consumer;

import com.furb.erp_custos_calculo.dtos.ProductCalcRequestDto;
import com.furb.erp_custos_calculo.dtos.RubricCalcRequestDto;
import com.furb.erp_custos_calculo.services.ProductValueCalculatorService;
import com.furb.erp_custos_calculo.services.RubricValueCalculatorService;
import com.furb.erp_custos_calculo.utils.constants.RabbitMQConstansts;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConsumerComponent {

    @Autowired
    private ProductValueCalculatorService productPriceCalculatorService;

    @Autowired
    private RubricValueCalculatorService rubricValueCalculatorService;

    @RabbitListener(queues = RabbitMQConstansts.CALC_PRODUCT_QUEUE, errorHandler = RabbitMQConstansts.RETRY_ERROR_HANDLER)
    private void consumptionCalcProduct(ProductCalcRequestDto payload){
        productPriceCalculatorService.calculate(payload);
    }

    @RabbitListener(queues = RabbitMQConstansts.CALC_RUBRIC_QUEUE, errorHandler = RabbitMQConstansts.RETRY_ERROR_HANDLER)
    private void consumptionCalcRubric(RubricCalcRequestDto payload){
        rubricValueCalculatorService.calculate(payload);
    }

}
