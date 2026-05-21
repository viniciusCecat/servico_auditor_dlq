# Servico Auditor DLQ

Servico independente responsavel por consumir mensagens da fila `DLQ`, classificar a severidade do erro e persistir um registro de auditoria em banco de dados.

## Objetivo

Quando uma mensagem cai na Dead Letter Queue, ela deixou de ser apenas um evento de negocio e passou a ser uma evidencia operacional. Este projeto existe para retirar a mensagem da `DLQ` com seguranca, registrar o payload bruto que falhou, manter um status inicial de analise e calcular a severidade para priorizacao do time tecnico.

## Explicacao rapida em linguagem simples

Se eu tivesse que explicar este projeto de forma bem direta, seria assim:

- existe uma fila chamada `DLQ` onde ficam mensagens que deram problema;
- este servico fica escutando essa fila;
- quando uma mensagem chega, ele le o conteudo, calcula o nivel de severidade e salva tudo no banco;
- so depois de salvar com sucesso ele remove a mensagem da fila.

Ou seja, o sistema funciona como um pequeno "hospital da fila": ele recebe a mensagem com falha, faz uma triagem e registra a ocorrencia para analise futura.

## O que sera avaliado e como este projeto atende

### 1. Funcionamento

O enunciado pede que a mensagem saia da `DLQ` e entre no banco com os campos corretos. Este projeto atende isso da seguinte forma:

- o listener `PedidoDlqListenerAdapter` escuta a fila `T04N_VINICIUS_CECATTO-DLQ.fifo`;
- o payload bruto recebido e salvo no campo `payload`;
- o servico gera um `UUID` para `errorId`;
- o `queueName` e preenchido com o nome da fila configurada;
- o `timestamp` e gerado pelo proprio servico;
- o `status` sempre inicia como `PENDING_ANALYSIS`;
- o `severity` e calculado pela regra de negocio antes de persistir;
- a mensagem so e removida da fila depois do salvamento, usando `acknowledgement.acknowledge()`.

### 2. Decisao arquitetural

O professor tambem avalia se a arquitetura escolhida faz sentido para um servico de apoio. Neste caso, a escolha por uma hexagonal enxuta faz sentido porque:

- a entrada principal nao e uma tela nem uma API REST, e sim uma fila SQS;
- existe uma regra de negocio clara no meio do processo, que e a triagem de severidade;
- existe uma saida tecnica bem definida, que e a persistencia no banco;
- separar entrada, regra e saida deixa o servico mais organizado e mais facil de explicar.

### 3. Aplicacao da arquitetura

Nao bastava apenas dizer que o projeto usa arquitetura hexagonal. Era preciso aplicar a ideia na pratica. Isso foi feito assim:

- `domain`: guarda os modelos centrais e a regra de severidade;
- `application`: guarda o caso de uso e as portas;
- `infrastructure.adapters.in`: recebe a mensagem da AWS SQS;
- `infrastructure.adapters.out`: salva no banco com JPA/H2.

Essa separacao evita misturar regra de negocio com detalhe tecnico de framework.

### 4. Justificativa

O `README` foi organizado para mostrar nao apenas "o que foi feito", mas tambem "por que foi feito assim". A justificativa principal e:

- proteger a regra de negocio de severidade;
- deixar o consumer SQS apenas como porta de entrada;
- deixar a persistencia apenas como porta de saida;
- facilitar manutencao, testes e explicacao academica do projeto.

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

## Mapa das classes principais

Para um universitario que estiver lendo o projeto pela primeira vez, estas sao as classes mais importantes:

- `PedidoDlqListenerAdapter`
  - e a porta de entrada do sistema;
  - escuta a `DLQ`;
  - recebe a mensagem e chama o caso de uso.

- `RegistrarErroDlqService`
  - e o caso de uso principal;
  - decide a severidade;
  - monta o registro final de auditoria.

- `SeveridadeErro`
  - concentra a regra que classifica `LOW`, `MEDIUM` e `HIGH`.

- `RegistroAuditoriaErro`
  - representa o registro que sera salvo no banco.

- `RegistroAuditoriaRepositoryAdapter`
  - pega o objeto do dominio e salva no banco usando JPA.

- `RegistroAuditoriaEntity`
  - representa a tabela `dlq_audit_records`.

Com esse mapa, fica mais facil entender o fluxo sem precisar ler todas as classes em ordem.

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

## Onde esta cada requisito do enunciado

Para deixar a ligacao com o trabalho mais explicita:

- consumir a fila `_DLQ`
  - implementado em `PedidoDlqListenerAdapter` com `@SqsListener`

- capturar o erro que impediu o processamento original
  - implementado com o campo `failureReason`
  - quando a mensagem vier com atributos como `errorMessage`, o servico persiste essa informacao

- remover a mensagem apenas depois do salvamento
  - implementado com `acknowledgement.acknowledge()` somente apos o `save`

- calcular prioridade `HIGH`, `MEDIUM` ou `LOW`
  - implementado em `SeveridadeErro` e usado por `RegistrarErroDlqService`

- salvar `errorId`, `queueName`, `payload`, `timestamp`, `status` e `severity`
  - implementado em `RegistroAuditoriaErro` e `RegistroAuditoriaEntity`

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
