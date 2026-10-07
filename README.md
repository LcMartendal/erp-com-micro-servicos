# ERP Custos – cálculo assíncrono com RabbitMQ

Módulo de Custos de um ERP industrial dividido em dois serviços que se comunicam **somente pelo RabbitMQ**:

| Serviço | Papel | Porta padrão |
|---|---|---|
| `erp-custos` | API HTTP, banco H2, busca os apontamentos do período, publica o pedido de cálculo e grava o resultado | 8080 |
| `erp-custos-calculo` | Sem banco. Consome o pedido, faz o cálculo e publica o resultado | 8081 (veja [Como subir](#como-subir)) |

Stack: Java, Spring Boot 4.1.1, Spring AMQP, Spring Data JPA, H2 (arquivo), RabbitMQ.

---

## Como funciona

```
Cliente --POST /rubrica {periodo}--> erp-custos
                                        |  1. busca apontamentos do periodo (H2)
                                        |  2. publica CalcularTaxaRubrica
                                        v
                          exchange custo.calculo.evt (direct)
                                        |  rk: rubrica.calcular
                                        v
                          fila custo.calculo.rubrica.q --> erp-custos-calculo
                                                              |  3. calcula as taxas
                                                              v  rk: resultado.rubrica
                          fila custo.resultado.rubrica.q --> erp-custos
                                                              4. grava no H2

Cliente --GET /rubrica--> erp-custos (le do H2)
```

O fluxo de produto é igual, com as chaves `produto.calcular` e `resultado.produto`.

O `POST` responde **202 Accepted** assim que o pedido é publicado. O cálculo acontece depois, no outro serviço, e o resultado aparece no `GET`.

### Regras de cálculo

- **Taxa da rubrica (R$/h)** = total da conta vinculada ÷ soma das horas (`totalHours`) de todos os apontamentos do período.
- **Custo do produto (por unidade)** = horas do produto no período × soma das taxas das rubricas ÷ quantidade do produto no período.
- As taxas precisam ser calculadas **antes** dos custos, porque o custo do produto usa as taxas.
- Resultados com 4 casas decimais, arredondamento `HALF_UP`.

---

## Mensageria

### Exchanges

| Exchange | Tipo | Uso |
|---|---|---|
| `custo.calculo.evt` | direct | comandos e resultados |
| `custo.calculo.dlx` | direct | dead-letter exchange |

### Filas e bindings

| Fila | Routing key | Quem publica | Quem consome |
|---|---|---|---|
| `custo.calculo.rubrica.q` | `rubrica.calcular` | erp-custos | erp-custos-calculo |
| `custo.calculo.produto.q` | `produto.calcular` | erp-custos | erp-custos-calculo |
| `custo.resultado.rubrica.q` | `resultado.rubrica` | erp-custos-calculo | erp-custos |
| `custo.resultado.produto.q` | `resultado.produto` | erp-custos-calculo | erp-custos |

### Retry e DLQ

Todas as filas acima têm tratamento de falha:

1. Se o processamento falha, a mensagem é republicada em uma fila de retry com TTL crescente: **5 s → 30 s → 2 min** (`<fila>.retry.5s`, `<fila>.retry.30s`, `<fila>.retry.2m`). Ao expirar, a própria fila de retry devolve a mensagem à fila principal. O header `x-retry-count` guarda a tentativa.
2. Esgotadas as 3 tentativas, a mensagem é rejeitada sem reentrega. A fila de origem a envia para `custo.calculo.dlx`, que a roteia pela routing key original:

| DLQ | Recebe as routing keys |
|---|---|
| `custo.calculo.rubrica.dlq` | `rubrica.calcular`, `resultado.rubrica` |
| `custo.calculo.produto.dlq` | `produto.calcular`, `resultado.produto` |

3. Quando uma mensagem vai para a DLQ, o log registra `[ALERTA-DLQ]`. Depois de corrigir a causa, reprocesse manualmente (por exemplo, movendo a mensagem da DLQ para a fila principal pelo painel do RabbitMQ).

Mensagens que não podem ser convertidas (JSON inválido) vão direto para a DLQ, sem retry.

As mensagens trafegam em **JSON**. Como os DTOs ficam em pacotes diferentes em cada serviço, só os nomes dos campos precisam coincidir.

---

## Como subir

### Pré-requisitos

- Docker (ou Java + Maven para rodar fora de container)
- RabbitMQ acessível pelos dois serviços, com usuário e senha `admin` / `admin` (valores do `application.properties`)

### 1. Opção recomendada: tudo em containers na mesma rede

Dentro de um container, `localhost` é o próprio container. Por isso o RabbitMQ não é encontrado se você rodar a aplicação em Docker sem informar o host. Coloque tudo na mesma rede e use o nome do container do RabbitMQ.

```powershell
docker network create erp-net

# RabbitMQ (com painel de gerenciamento)
docker run -d --name rabbitmq --network erp-net `
  -p 5672:5672 -p 15672:15672 `
  -e RABBITMQ_DEFAULT_USER=admin -e RABBITMQ_DEFAULT_PASS=admin `
  rabbitmq:management

# erp-custos  (http://localhost:8080)
docker run --network erp-net -p 8080:8080 `
  -e SPRING_RABBITMQ_HOST=rabbitmq `
  <imagem-erp-custos>

# erp-custos-calculo  (porta 8081 no seu PC, para não conflitar com a 8080)
docker run --network erp-net -p 8081:8080 `
  -e SPRING_RABBITMQ_HOST=rabbitmq `
  <imagem-erp-custos-calculo>
```

Troque `<imagem-erp-custos>` e `<imagem-erp-custos-calculo>` pelas imagens que você construiu de cada projeto.

Painel do RabbitMQ: <http://localhost:15672> (usuário `admin`, senha `admin`). Lá você vê as filas, as filas de retry e as DLQs.

### 2. Alternativa: RabbitMQ no PC e aplicações em container

Se o RabbitMQ está rodando no Windows (ou em container com a porta `5672` publicada), aponte as aplicações para o host da máquina:

```powershell
docker run -p 8080:8080 -e SPRING_RABBITMQ_HOST=host.docker.internal <imagem-erp-custos>
docker run -p 8081:8080 -e SPRING_RABBITMQ_HOST=host.docker.internal <imagem-erp-custos-calculo>
```

### 3. Alternativa: rodar direto com Maven (sem container das aplicações)

Com o RabbitMQ em `localhost:5672`, em dois terminais:

```powershell
# terminal 1
cd backend\erp-custos
.\mvnw spring-boot:run

# terminal 2  (porta diferente, os dois usam 8080 por padrão)
cd backend\erp-custos-calculo
.\mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Requisitos de Java: `erp-custos` usa Java 22 e `erp-custos-calculo` usa Java 25 (conforme os `pom.xml`).

### Variáveis de ambiente úteis

| Variável | Para quê |
|---|---|
| `SPRING_RABBITMQ_HOST` | host do RabbitMQ (padrão `localhost`) |
| `SPRING_RABBITMQ_USERNAME` / `SPRING_RABBITMQ_PASSWORD` | credenciais (padrão `admin` / `admin`) |
| `SERVER_PORT` | porta HTTP da aplicação |

### Como saber que subiu certo

No log de cada serviço não deve haver `Connection refused` nem `ACCESS_REFUSED`. No painel do RabbitMQ devem existir as 4 filas principais, as 12 filas de retry e as 2 DLQs.

### Banco H2

- Console: <http://localhost:8080/h2-console>
- JDBC URL: `jdbc:h2:file:./data/erp-custos` · usuário `admin` · senha `admin`
- O schema é recriado a cada inicialização (`ddl-auto=create`) e o `data.sql` recarrega os dados. O que o cálculo gravar é perdido ao reiniciar.

---

## Dados de exemplo (`data.sql`)

| Conjunto | Período | Conteúdo |
|---|---|---|
| Pequeno (conferível na mão) | `2026-09-01` a `2026-09-30` | 3 produtos (`PROD-001..003`), 5 apontamentos, 500 h-máquina |
| Em massa | `2026-08-01` a `2026-08-30` | 297 produtos (`PROD-004..PROD-300`) × 30 dias = 8.910 apontamentos |

Contas e rubricas (um para um):

| Conta | Total | Rubrica |
|---|---|---|
| Energia elétrica | 12.000 | `ENERGIA` |
| Mão de obra direta | 30.000 | `MOD` |
| Depreciação | 8.000 | `DEPRECIACAO` |

Resultado esperado no conjunto pequeno (setembro):

| Item | Valor |
|---|---|
| Taxa `ENERGIA` | 24,00 R$/h |
| Taxa `MOD` | 60,00 R$/h |
| Taxa `DEPRECIACAO` | 16,00 R$/h |
| Custo `PROD-001` | 10,00 |
| Custo `PROD-002` | 50,00 |
| Custo `PROD-003` | 200,00 |

O total das contas não tem período, então no conjunto em massa as taxas por hora saem bem menores (há muito mais horas para ratear os mesmos totais).

Períodos para testar: setembro (conjunto pequeno), agosto (conjunto em massa) ou `2026-08-01` a `2026-09-30` (os dois juntos).

---

## Endpoints (erp-custos, porta 8080)

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/rubrica` | Dispara o cálculo das taxas das rubricas para o período |
| `GET` | `/rubrica` | Lista as rubricas e as taxas já calculadas |
| `POST` | `/produto` | Dispara o cálculo dos custos dos produtos para o período |
| `GET` | `/produto` | Lista os produtos e os custos já calculados |

### Body dos `POST`

Só o período. As datas são inclusivas e no formato `yyyy-MM-dd`.

```json
{
  "dataInicio": "2026-09-01",
  "dataFim": "2026-09-30"
}
```

### Passo a passo

**1. Calcular as rubricas**

```bash
curl -X POST http://localhost:8080/rubrica \
  -H "Content-Type: application/json" \
  -d '{"dataInicio":"2026-09-01","dataFim":"2026-09-30"}'
```

Resposta `202 Accepted`:

```json
{
  "message": "Taxas das rubricas serão calculadas",
  "dataInicio": "2026-09-01",
  "dataFim": "2026-09-30",
  "appointmentsSent": 5
}
```

**2. Conferir as taxas** (espere alguns segundos)

```bash
curl http://localhost:8080/rubrica
```

Resposta `200 OK`:

```json
[
  { "id": "b0000000-0000-0000-0000-000000000003", "code": "DEPRECIACAO", "value": 16.0000 },
  { "id": "b0000000-0000-0000-0000-000000000001", "code": "ENERGIA",     "value": 24.0000 },
  { "id": "b0000000-0000-0000-0000-000000000002", "code": "MOD",         "value": 60.0000 }
]
```

Enquanto o cálculo não termina, `value` vem `null`.

**3. Calcular os produtos** (mesmo período)

```bash
curl -X POST http://localhost:8080/produto \
  -H "Content-Type: application/json" \
  -d '{"dataInicio":"2026-09-01","dataFim":"2026-09-30"}'
```

Resposta `202 Accepted`:

```json
{
  "message": "Custos dos produtos serão calculados",
  "dataInicio": "2026-09-01",
  "dataFim": "2026-09-30",
  "appointmentsSent": 5
}
```

**4. Conferir os custos**

```bash
curl http://localhost:8080/produto
```

Resposta `200 OK` (trecho; os produtos sem apontamento no período seguem com `null`):

```json
[
  { "id": "c0000000-0000-0000-0000-000000000001", "code": "PROD-001", "value": 10.0000 },
  { "id": "c0000000-0000-0000-0000-000000000002", "code": "PROD-002", "value": 50.0000 },
  { "id": "c0000000-0000-0000-0000-000000000003", "code": "PROD-003", "value": 200.0000 },
  { "id": "...", "code": "PROD-004", "value": null }
]
```

### Postman / Insomnia

1. Método `POST`, URL `http://localhost:8080/rubrica` (ou `/produto`).
2. Aba **Body** → **raw** → **JSON**.
3. Cole o JSON com `dataInicio` e `dataFim`.
4. Envie o `GET` no mesmo endereço para ver o resultado.

No PowerShell, use `curl.exe` (o `curl` é um alias de `Invoke-WebRequest`) ou:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/rubrica `
  -ContentType "application/json" `
  -Body '{"dataInicio":"2026-09-01","dataFim":"2026-09-30"}'
```

### Erros

| Status | Causa |
|---|---|
| `400` | falta `dataInicio` ou `dataFim`, ou `dataFim` é anterior a `dataInicio` |
| `404` | nenhum apontamento no período (ou nenhum produto cadastrado para eles, no `POST /produto`) |
| `409` | `POST /produto` antes das taxas das rubricas serem calculadas, ou nenhuma rubrica com conta vinculada |

Para ver o texto do erro na resposta, adicione ao `application.properties` do `erp-custos`:

```properties
server.error.include-message=always
```

---

## Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Connection refused` em `localhost:5672` no log | aplicação em container usando `localhost` | defina `SPRING_RABBITMQ_HOST` (veja [Como subir](#como-subir)) |
| `ACCESS_REFUSED` | usuário/senha diferentes de `admin/admin` (o `compose.yaml` do projeto cria `myuser/secret`) | crie o usuário `admin` ou ajuste as credenciais |
| `POST /produto` retorna `409` | rubricas ainda sem taxa | `POST /rubrica`, aguarde e confira no `GET /rubrica` |
| `GET` continua com `value: null` | `erp-custos-calculo` fora do ar ou mensagem na DLQ | veja o log do calculo e o painel do RabbitMQ |
| `Port 8080 was already in use` | os dois serviços usam 8080 por padrão | `SERVER_PORT=8081` ou `-p 8081:8080` no calculo |
| `PRECONDITION_FAILED ... inequivalent arg` | filas já existiam no broker com argumentos diferentes | apague as filas antigas no painel (ou `rabbitmqctl delete_queue <fila>`) e suba de novo |

| Erro de conversão após atualizar o projeto | mensagens antigas (serialização Java) ainda na fila | limpe as filas (purge) |

---

## Limitações conhecidas

- O total da conta não tem período: é usado como está, qualquer que seja o período consultado.
- Há um único valor por rubrica e por produto. Um novo cálculo sobrescreve o anterior (não há histórico por período).
- O pedido de cálculo vai em uma única mensagem com todos os dados do período. Dividir em uma mensagem por produto/rubrica permitiria paralelizar entre instâncias e reprocessar só o item que falhou.
