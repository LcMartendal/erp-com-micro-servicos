-- =====================================================================================
-- Dados de exemplo. É recarregado a cada inicialização, porque o schema é recriado
-- (ddl-auto=create). As colunas de valor de rubric e product ficam NULL: são o resultado
-- que o cálculo grava.
--
-- CONJUNTO 1 - pequeno, para conferir na mão (setembro/2026: 2026-09-01 a 2026-09-30)
--   5 apontamentos, 3 produtos (PROD-001..003), soma de 500 h-máquina.
--   Taxa por rubrica (R$/h) = total da conta / horas-máquina do período
--       ENERGIA      = 12.000 / 500 = 24,00
--       MOD          = 30.000 / 500 = 60,00
--       DEPRECIACAO  =  8.000 / 500 = 16,00      (soma das taxas = 100,00 R$/h)
--   Custo por unidade = (horas do produto x soma das taxas) / quantidade do produto
--       PROD-001 = 180 h x 100 / 1800 un =  10,00
--       PROD-002 = 220 h x 100 /  440 un =  50,00
--       PROD-003 = 100 h x 100 /   50 un = 200,00
--
-- CONJUNTO 2 - em massa, para dar trabalho ao cálculo (agosto/2026: 2026-08-01 a 2026-08-30)
--   297 produtos (PROD-004..PROD-300) x 30 dias = 8.910 apontamentos.
--   Os valores são gerados por fórmula (sem aleatoriedade), então são sempre os mesmos.
--   Para aumentar o volume, mude os limites dos SYSTEM_RANGE abaixo.
--   Atenção: o total das contas é fixo (a conta não tem período), então no conjunto 2
--   as taxas por hora saem bem menores que no conjunto 1.
--
-- Períodos para testar: setembro (conjunto 1), agosto (conjunto 2) ou 2026-08-01 a 2026-09-30 (os dois).
-- =====================================================================================

INSERT INTO account (id, name, total_value) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Energia elétrica',  12000.00),
    ('a0000000-0000-0000-0000-000000000002', 'Mão de obra direta', 30000.00),
    ('a0000000-0000-0000-0000-000000000003', 'Depreciação',        8000.00);

INSERT INTO rubric (id, code, account_id, rate_value) VALUES
    ('b0000000-0000-0000-0000-000000000001', 'ENERGIA',     'a0000000-0000-0000-0000-000000000001', NULL),
    ('b0000000-0000-0000-0000-000000000002', 'MOD',         'a0000000-0000-0000-0000-000000000002', NULL),
    ('b0000000-0000-0000-0000-000000000003', 'DEPRECIACAO', 'a0000000-0000-0000-0000-000000000003', NULL);

-- ---------- Conjunto 1 ----------
INSERT INTO product (id, code, cost_value) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'PROD-001', NULL),
    ('c0000000-0000-0000-0000-000000000002', 'PROD-002', NULL),
    ('c0000000-0000-0000-0000-000000000003', 'PROD-003', NULL);

INSERT INTO production_appointment (id, code, total_quantity, total_hours, date_time) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'PROD-001', 1000, 100, '2026-09-03 08:00:00'),
    ('d0000000-0000-0000-0000-000000000002', 'PROD-001',  800,  80, '2026-09-17 08:00:00'),
    ('d0000000-0000-0000-0000-000000000003', 'PROD-002',  240, 120, '2026-09-05 08:00:00'),
    ('d0000000-0000-0000-0000-000000000004', 'PROD-002',  200, 100, '2026-09-19 08:00:00'),
    ('d0000000-0000-0000-0000-000000000005', 'PROD-003',   50, 100, '2026-09-24 08:00:00');

-- ---------- Conjunto 2 ----------
INSERT INTO product (id, code, cost_value)
SELECT RANDOM_UUID(), 'PROD-' || LPAD(CAST(X AS VARCHAR), 3, '0'), NULL
FROM SYSTEM_RANGE(4, 300);

INSERT INTO production_appointment (id, code, total_quantity, total_hours, date_time)
SELECT RANDOM_UUID(),
       p.code,
       50 + MOD(p.n * 7 + X * 13, 450),
       2 + MOD(p.n * 3 + X * 5, 14),
       DATEADD(HOUR, 6 + MOD(p.n, 12), DATEADD(DAY, X - 1, TIMESTAMP '2026-08-01 00:00:00'))
FROM (SELECT code, CAST(SUBSTRING(code, 6) AS INT) AS n FROM product) p
CROSS JOIN SYSTEM_RANGE(1, 30)
WHERE p.n >= 4;
