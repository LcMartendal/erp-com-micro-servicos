package com.furb.erp_custos_calculo.utils.retry;

import com.furb.erp_custos_calculo.utils.constants.RabbitMQConstansts;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.stereotype.Component;

import static com.furb.erp_custos_calculo.utils.constants.RabbitMQConstansts.*;

@Component(RabbitMQConstansts.RETRY_ERROR_HANDLER)
@Slf4j
public class RetryErrorHandler implements RabbitListenerErrorHandler {

    private final RabbitTemplate rabbitTemplate;

    public RetryErrorHandler(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public @Nullable Object handleError(Message amqpMessage, @Nullable Channel channel, org.springframework.messaging.@Nullable Message<?> message, org.springframework.amqp.listener.ListenerExecutionFailedException exception) throws Exception {
        MessageProperties properties = amqpMessage.getMessageProperties();
        String queueName = properties.getConsumerQueue();
        int retries = currentRetries(properties);
        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

        if (retries >= MAX_RETRIES) {
            // Ponto de alerta: a mensagem está indo para a DLQ e precisa de intervenção manual
            log.error("[ALERTA-DLQ] Mensagem da fila {} esgotou {} tentativas e será enviada para a DLQ. Causa: {}",
                    queueName, MAX_RETRIES, cause.toString(), cause);
            throw new AmqpRejectAndDontRequeueException(
                    "Tentativas esgotadas na fila " + queueName, cause);
        }

        String retryQueue = retryQueueName(queueName, retries);
        Message retryMessage = MessageBuilder.fromClonedMessage(amqpMessage)
                .setHeader(RETRY_COUNT_HEADER, retries + 1)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();

        log.warn("Falha ao processar mensagem da fila {} (tentativa {}/{}). Reenviando para {}. Causa: {}",
                queueName, retries + 1, MAX_RETRIES, retryQueue, cause.toString());

        // Exchange padrão: a routing key é o nome da fila de retry
        rabbitTemplate.send("", retryQueue, retryMessage);
        return null;
    }

    private int currentRetries(MessageProperties properties) {
        Object value = properties.getHeader(RETRY_COUNT_HEADER);
        return value instanceof Number number ? number.intValue() : 0;
    }
}
