package com.furb.erp_custos.utils.rabbitmqUtils;

import com.furb.erp_custos.dto.ProductDto;
import com.furb.erp_custos.dto.RubricDto;
import com.furb.erp_custos.service.ProductService;
import com.furb.erp_custos.service.RubricService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConsumerComponent {

    @Autowired
    private ProductService productService;

    @Autowired
    private RubricService rubricService;

    @RabbitListener(queues = RabbitMQConstansts.RESULT_CALC_PRODUCT_QUEUE, errorHandler = RabbitMQConstansts.RETRY_ERROR_HANDLER)
    private void consumptionCalcProduct(List<ProductDto> payload){
        productService.persistData(payload);
    }

    @RabbitListener(queues = RabbitMQConstansts.RESULT_CALC_RUBRIC_QUEUE, errorHandler = RabbitMQConstansts.RETRY_ERROR_HANDLER)
    private void consumptionCalcRubric(List<RubricDto> payload){
        rubricService.persistData(payload);
    }

}
