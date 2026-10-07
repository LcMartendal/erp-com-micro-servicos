package com.furb.erp_custos_calculo.utils.constants;

public class RabbitMQConstansts {

    // Exchanges
    public static final String DIRECT_EXCHANGE_NAME = "custo.calculo.evt";
    public static final String DLX_EXCHANGE_NAME = "custo.calculo.dlx";

    // Filas de trabalho e de resultado
    public static final String CALC_PRODUCT_QUEUE = "custo.calculo.produto.q";
    public static final String CALC_RUBRIC_QUEUE = "custo.calculo.rubrica.q";
    public static final String RESULT_CALC_PRODUCT_QUEUE = "custo.resultado.produto.q";
    public static final String RESULT_CALC_RUBRIC_QUEUE = "custo.resultado.rubrica.q";

    // Routing keys / bindings
    public static final String RK_CALC_PRODUCT = "produto.calcular";
    public static final String RK_CALC_RUBRIC = "rubrica.calcular";
    public static final String RK_RESULT_PRODUCT = "resultado.produto";
    public static final String RK_RESULT_RUBRIC = "resultado.rubrica";

    // Dead letter queues
    public static final String DLQ_PRODUCT = "custo.calculo.produto.dlq";
    public static final String DLQ_RUBRIC = "custo.calculo.rubrica.dlq";

    // Retry: 3 tentativas com TTL crescente (5s, 30s, 2min)
    public static final String RETRY_ERROR_HANDLER = "retryErrorHandler";
    public static final String RETRY_COUNT_HEADER = "x-retry-count";
    public static final int[] RETRY_DELAYS_MS = {5_000, 30_000, 120_000};
    public static final String[] RETRY_DELAY_LABELS = {"5s", "30s", "2m"};
    public static final int MAX_RETRIES = RETRY_DELAYS_MS.length;

    /** Nome da fila de retry usada na tentativa {@code attempt} (0 = primeira nova tentativa). */
    public static String retryQueueName(String queueName, int attempt) {
        return queueName + ".retry." + RETRY_DELAY_LABELS[attempt];
    }
}
