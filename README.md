# Servico Auditor DLQ

Servico independente responsavel por consumir mensagens da fila `DLQ`, classificar a severidade do erro e persistir um registro de auditoria em banco de dados.

## Objetivo

Quando uma mensagem cai na Dead Letter Queue, ela deixou de ser apenas um evento de negocio e passou a ser uma evidencia operacional. Este projeto existe para retirar a mensagem da `DLQ` com seguranca, registrar o payload bruto que falhou, manter um status inicial de analise e calcular a severidade para priorizacao do time tecnico.

## Arquitetura escolhida

Escolhi uma **Arquitetura Hexagonal enxuta**.

### Por que Hexagonal faz sentido aqui?

Este servico tem um caso de uso muito especifico:

1. receber uma mensagem vinda de uma tecnologia externa (`AWS SQS`);
2. aplicar uma regra de negocio de triagem (`LOW`, `MEDIUM`, `HIGH`);
3. salvar o resultado em outra tecnologia externa (`JPA/H2`).

Essas duas bordas tecnicas sao bem definidas, e a regra de negocio no meio e pequena, mas importante. Por isso a arquitetura hexagonal se encaixa muito bem:

- o caso de uso fica isolado da forma como a mensagem chega;
- a regra de severidade nao fica presa no listener nem no repositorio;
- a persistencia pode mudar de `H2` para `Postgres` sem alterar a regra central;
- o consumer SQS pode ser trocado por outro mecanismo de entrada sem reescrever o nucleo do servico.

### Por que eu nao escolhi MVC?

MVC faria menos sentido porque este servico nao e orientado a tela nem a controller HTTP. A entrada principal dele e assincrona, via fila, entao modelar o centro do projeto em torno de controllers seria forcar uma estrutura que nao representa o problema.

### Por que eu nao escolhi uma Layered tradicional?

Uma Layered Architecture tambem funcionaria, mas a hexagonal me deu uma separacao mais explicita entre:

- porta de entrada: receber a mensagem da DLQ;
- caso de uso: registrar a auditoria e calcular a severidade;
- porta de saida: salvar o registro no banco.

Como a disciplina esta trabalhando exatamente o conceito de portas e adaptadores, preferi uma versao enxuta dessa abordagem para mostrar intencionalidade arquitetural, sem exagerar na quantidade de classes.

## Organizacao das pastas

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

### Responsabilidade de cada camada

- `domain`
  - contem o modelo central do servico (`EventoPedidoFalho`, `RegistroAuditoriaErro`, `SeveridadeErro`, `StatusAnalise`);
  - nao conhece Spring, JPA, SQS ou H2.

- `application`
  - define o contrato do caso de uso (`RegistrarErroDlqUseCase`);
  - define a porta de persistencia (`RegistroAuditoriaRepositoryPort`);
  - implementa a orquestracao no `RegistrarErroDlqService`.

- `infrastructure.adapters.in.sqs`
  - contem o listener SQS;
  - converte o payload bruto para o modelo interno;
  - extrai o motivo da falha, quando ele vier como atributo da mensagem;
  - so reconhece a mensagem (`ack`) depois de salvar com sucesso.

- `infrastructure.adapters.out.persistence`
  - implementa a persistencia com JPA/H2;
  - mapeia o modelo de dominio para a entidade de banco.

- `infrastructure.config`
  - concentra beans tecnicos, como `Clock` e configuracao do listener SQS com `acknowledgement` manual.

## Regra de negocio implementada

A severidade e calculada no centro do servico:

- quantidade total de produtos maior que `100` -> `HIGH`
- quantidade total de produtos entre `50` e `100` -> `MEDIUM`
- quantidade total de produtos menor que `50` -> `LOW`

Essa regra esta encapsulada no dominio, e o listener apenas encaminha os dados para o caso de uso.

## Fluxo do processamento

1. o adapter `PedidoDlqListenerAdapter` escuta a fila configurada em `aws.sqs.queue.dlq-name`;
2. a mensagem chega como `payload` bruto;
3. o adapter tenta transformar o JSON recebido em um `EventoPedidoFalho`;
4. o caso de uso calcula a severidade e cria um `RegistroAuditoriaErro`;
5. o adapter de persistencia salva o registro no banco;
6. somente depois disso o listener executa `acknowledgement.acknowledge()`, removendo a mensagem da DLQ.

## Contrato salvo no banco

Os campos obrigatorios pedidos no enunciado sao preservados:

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

Adicionei tambem o campo `failureReason`, porque o proprio enunciado pede para capturar o erro que impediu o processamento original. Sem esse campo, eu seria obrigado a misturar diagnostico tecnico dentro do `payload`, o que quebraria o contrato da auditoria.

## Observacao importante sobre o erro original da DLQ

No `AWS SQS`, a mensagem movida para a DLQ normalmente preserva o payload original, mas nao carrega automaticamente a excecao Java que derrubou o primeiro consumer.

Por isso, este servico adota a seguinte estrategia:

- se a mensagem vier com atributos como `errorMessage`, `failureReason`, `processingError` ou `originalError`, eles sao persistidos em `failureReason`;
- se esses atributos nao existirem, o servico registra um texto de fallback informando que a mensagem foi parar na DLQ sem detalhe tecnico do erro original.

Essa decisao foi importante para manter honestidade arquitetural: o servico nao inventa um erro que nao existe, mas tambem nao perde a oportunidade de auditar o maximo de contexto disponivel.

## Banco de dados

Para simplificar a execucao e a avaliacao, foi usado `H2` em modo arquivo:

- nao depende de instalar banco externo;
- permite abrir o console em `/h2-console`;
- deixa a evidencia mais facil para o print da atividade.

Tabela gerada:

- `dlq_audit_records`

Colunas:

- `error_id`
- `queue_name`
- `payload`
- `audit_timestamp`
- `status`
- `severity`
- `failure_reason`

## Configuracao

Arquivo: `src/main/resources/application.properties`

Variaveis esperadas:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION` (opcional, default `us-east-1`)
- `AUDIT_DLQ_QUEUE_NAME` (opcional, default `T04N_VINICIUS_CECATTO-DLQ.fifo`)

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

Quando a aplicacao estiver no ar, ela vai ficar ouvindo ativamente a fila configurada.

## Como testar a evidencia pedida

### 1. Evidencia no terminal

Suba a aplicacao e envie ou redirecione uma mensagem para a DLQ. O log esperado sera parecido com:

```text
Mensagem recebida da DLQ. queueName=T04N_VINICIUS_CECATTO-DLQ.fifo, messageId=...
Mensagem removida da DLQ apos persistencia segura. errorId=..., severity=LOW|MEDIUM|HIGH
```

### 2. Evidencia no banco

Abra:

- [H2 Console](http://localhost:8080/h2-console)

JDBC URL:

```text
jdbc:h2:file:./data/dlq-audit-db
```

Consulta sugerida:

```sql
select error_id, queue_name, audit_timestamp, status, severity, failure_reason
from dlq_audit_records;
```

## Exemplo de mensagem esperada na DLQ

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

## Testes automatizados

Foram adicionados testes para:

- classificacao da severidade;
- geracao do registro de auditoria;
- comportamento do listener, garantindo `ack` somente apos sucesso.

Executar:

```powershell
./mvnw.cmd test
```

## Entrega sugerida

1. criar um repositorio proprio com o nome `servico_auditor_dlq`;
2. manter o desenvolvimento na branch `master`;
3. fazer o push do projeto;
4. anexar o link do repositorio;
5. anexar os prints do terminal e do banco.
