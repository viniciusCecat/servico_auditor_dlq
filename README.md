# Servico Auditor DLQ

Servico independente responsavel por consumir mensagens da fila `DLQ`, classificar a severidade do erro e persistir um registro de auditoria em banco de dados.

## Objetivo

Quando uma mensagem vai para a Dead Letter Queue, significa que ela nao conseguiu ser processada normalmente. O objetivo deste servico e escutar essa fila, registrar a mensagem com falha no banco e deixar essas informacoes salvas para analise posterior.

## Arquitetura escolhida

Escolhi uma **Arquitetura Hexagonal enxuta**.

Fiz essa escolha porque este servico tem um fluxo bem claro:

- recebe uma mensagem de uma fila SQS;
- aplica uma regra de negocio para definir a severidade;
- salva o resultado em banco de dados.

A arquitetura hexagonal fez sentido porque separa bem:

- a entrada do sistema, que e a AWS SQS;
- a regra de negocio, que e a classificacao da severidade;
- a saida do sistema, que e a persistencia no banco.

Assim, a regra principal do servico nao fica misturada com detalhes do Spring, do SQS ou do JPA.

## Organizacao do projeto

```text
src/main/java/com/viniciuscecatto/servicoauditordlq
|-- application
|   |-- command
|   |-- ports
|   |   |-- in
|   |   `-- out
|   `-- service
|-- domain
|   `-- model
`-- infrastructure
    |-- adapters
    |   |-- in
    |   |   `-- sqs
    |   `-- out
    |       `-- persistence
    `-- config
```

### Papel de cada camada

- `domain`
  - guarda os modelos centrais do servico;
  - contem a regra de severidade;
  - nao depende de framework.

- `application`
  - define o caso de uso principal;
  - define as portas de entrada e saida;
  - faz a orquestracao do processo.

- `infrastructure`
  - contem os adaptadores tecnicos;
  - recebe a mensagem da fila SQS;
  - salva os dados no banco com JPA/H2.

## Regra de negocio

A prioridade do erro e definida pela quantidade total de produtos da mensagem:

- maior que `100` -> `HIGH`
- entre `50` e `100` -> `MEDIUM`
- menor que `50` -> `LOW`

Essa regra foi colocada no centro da aplicacao, e nao no listener nem no repositorio.

## Como o servico funciona

1. O listener escuta a fila `_DLQ`.
2. Quando a mensagem chega, o servico le o payload bruto.
3. O sistema tenta identificar o motivo da falha original.
4. A severidade e calculada pela regra de negocio.
5. O registro e salvo no banco.
6. A mensagem so e removida da fila depois do salvamento com sucesso.

## Requisitos atendidos

- consumo da fila `_DLQ`
- calculo de severidade `LOW`, `MEDIUM` e `HIGH`
- remocao da mensagem apenas apos persistencia
- salvamento dos campos obrigatorios pedidos no enunciado

## Registro salvo no banco

O registro final segue este formato:

```json
{
  "errorId": "uuid-gerado-pelo-servico",
  "queueName": "T04N_VINICIUS_CECATTO-DLQ.fifo",
  "payload": "{ ... conteudo bruto da mensagem ... }",
  "timestamp": "2026-05-07T14:30:00Z",
  "status": "PENDING_ANALYSIS",
  "severity": "HIGH"
}
```

Tambem foi salvo o campo `failureReason`, para registrar o motivo da falha quando essa informacao estiver disponivel na mensagem.

## Banco de dados

Foi utilizado `H2` em modo arquivo para facilitar a execucao e a demonstracao do projeto.

Tabela utilizada:

- `dlq_audit_records`

## Configuracao

Variaveis esperadas:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `AUDIT_DLQ_QUEUE_NAME`

Essas variaveis podem ser configuradas no terminal ou na configuracao de execucao da IDE.

Exemplo no PowerShell:

```powershell
$env:AWS_ACCESS_KEY_ID="SUA_ACCESS_KEY"
$env:AWS_SECRET_ACCESS_KEY="SUA_SECRET_KEY"
$env:AWS_REGION="us-east-1"
$env:AUDIT_DLQ_QUEUE_NAME="T04N_VINICIUS_CECATTO-DLQ.fifo"
```

## Como executar

```powershell
./mvnw.cmd clean spring-boot:run
```

## Evidencias

Para a entrega, foram geradas duas evidencias:

- terminal mostrando a mensagem sendo recebida da `DLQ` e removida apos persistencia;
- H2 Console mostrando o registro salvo no banco.

Consulta usada no H2:

```sql
select error_id, queue_name, status, severity, failure_reason, audit_timestamp
from dlq_audit_records;
```

## Exemplo de mensagem consumida

```json
{
  "zipCode": "80010000",
  "customerId": 1,
  "orderItems": [
    {
      "sku": 1,
      "amount": 5
    },
    {
      "sku": 2,
      "amount": 3
    }
  ],
  "origin": "SQS_QUEUE",
  "occurredAt": "2024-05-20T14:30:00Z"
}
```
