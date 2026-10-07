package com.furb.erp_custos_calculo.connections;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static com.furb.erp_custos_calculo.utils.constants.RabbitMQConstansts.*;

/**
 * Topologia do RabbitMQ (declarada pelo RabbitAdmin ao conectar). Os dois serviços declaram
 * a mesma topologia, então os argumentos das filas precisam ser idênticos nos dois.
 *
 * Fluxo de falha: fila de origem -> custo.calculo.dlx -> DLQ (rubrica ou produto, pela routing key).
 * Fluxo de retry: o consumidor publica na fila de retry (TTL 5s/30s/2min) e, ao expirar, a própria
 * fila devolve a mensagem à exchange custo.calculo.evt com a routing key original.
 */
@Configuration
public class RabbitMQConnection {

    @Bean
    public Declarables rabbitTopology(
            @Value("${app.rabbitmq.queue-message-ttl-ms:0}") int queueMessageTtlMs) {

        DirectExchange eventExchange = new DirectExchange(DIRECT_EXCHANGE_NAME, true, false);
        DirectExchange deadLetterExchange = new DirectExchange(DLX_EXCHANGE_NAME, true, false);

        Queue rubricDlq = QueueBuilder.durable(DLQ_RUBRIC).build();
        Queue productDlq = QueueBuilder.durable(DLQ_PRODUCT).build();

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(eventExchange);
        declarables.add(deadLetterExchange);
        declarables.add(rubricDlq);
        declarables.add(productDlq);

        // DLX -> DLQ: a routing key original da mensagem é preservada e decide a DLQ
        declarables.add(bindDlq(rubricDlq, deadLetterExchange, RK_CALC_RUBRIC));
        declarables.add(bindDlq(rubricDlq, deadLetterExchange, RK_RESULT_RUBRIC));
        declarables.add(bindDlq(productDlq, deadLetterExchange, RK_CALC_PRODUCT));
        declarables.add(bindDlq(productDlq, deadLetterExchange, RK_RESULT_PRODUCT));

        addWorkQueue(declarables, eventExchange, CALC_RUBRIC_QUEUE, RK_CALC_RUBRIC, queueMessageTtlMs);
        addWorkQueue(declarables, eventExchange, CALC_PRODUCT_QUEUE, RK_CALC_PRODUCT, queueMessageTtlMs);
        addWorkQueue(declarables, eventExchange, RESULT_CALC_RUBRIC_QUEUE, RK_RESULT_RUBRIC, queueMessageTtlMs);
        addWorkQueue(declarables, eventExchange, RESULT_CALC_PRODUCT_QUEUE, RK_RESULT_PRODUCT, queueMessageTtlMs);

        return new Declarables(declarables);
    }

    /** Mensagens em JSON (os DTOs ficam em pacotes diferentes em cada serviço). */
    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    private Binding bindDlq(Queue dlq, DirectExchange dlx, String routingKey) {
        return BindingBuilder.bind(dlq).to(dlx).with(routingKey);
    }

    /** Fila principal (com DLX) + binding na exchange + filas de retry com TTL crescente. */
    private void addWorkQueue(List<Declarable> declarables, DirectExchange exchange,
                              String queueName, String routingKey, int messageTtlMs) {
        QueueBuilder builder = QueueBuilder.durable(queueName)
                .deadLetterExchange(DLX_EXCHANGE_NAME);
        if (messageTtlMs > 0) {
            builder = builder.ttl(messageTtlMs); // mensagem expirada na fila principal vai para a DLX
        }
        Queue queue = builder.build();
        declarables.add(queue);
        declarables.add(BindingBuilder.bind(queue).to(exchange).with(routingKey));

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            declarables.add(QueueBuilder.durable(retryQueueName(queueName, attempt))
                    .ttl(RETRY_DELAYS_MS[attempt])
                    .deadLetterExchange(DIRECT_EXCHANGE_NAME)
                    .deadLetterRoutingKey(routingKey)
                    .build());
        }
    }
}
