package com.furb.erp_custos.utils.rabbitmqUtils;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProducerComponent {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String routingKey, Object payload) {
        this.rabbitTemplate.convertAndSend(RabbitMQConstansts.DIRECT_EXCHANGE_NAME, routingKey, payload);
    }

}
