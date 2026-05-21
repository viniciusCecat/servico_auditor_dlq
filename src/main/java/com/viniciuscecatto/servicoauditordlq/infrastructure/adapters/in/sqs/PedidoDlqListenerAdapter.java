package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.viniciuscecatto.servicoauditordlq.application.command.RegistrarErroDlqCommand;
import com.viniciuscecatto.servicoauditordlq.application.ports.in.RegistrarErroDlqUseCase;
import com.viniciuscecatto.servicoauditordlq.domain.model.EventoPedidoFalho;
import com.viniciuscecatto.servicoauditordlq.domain.model.RegistroAuditoriaErro;
import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs.mapper.PedidoDlqPayloadMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "aws.sqs.listener.enabled", havingValue = "true", matchIfMissing = true)
public class PedidoDlqListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(PedidoDlqListenerAdapter.class);

    private static final List<String> POSSIVEIS_ATRIBUTOS_DE_ERRO = List.of(
            "errorMessage",
            "failureReason",
            "processingError",
            "originalError",
            "errorType"
    );

    private final RegistrarErroDlqUseCase registrarErroDlqUseCase;
    private final PedidoDlqPayloadMapper pedidoDlqPayloadMapper;
    private final String queueName;

    public PedidoDlqListenerAdapter(
            RegistrarErroDlqUseCase registrarErroDlqUseCase,
            PedidoDlqPayloadMapper pedidoDlqPayloadMapper,
            @Value("${aws.sqs.queue.dlq-name}") String queueName
    ) {
        this.registrarErroDlqUseCase = registrarErroDlqUseCase;
        this.pedidoDlqPayloadMapper = pedidoDlqPayloadMapper;
        this.queueName = queueName;
    }

    @SqsListener(value = "${aws.sqs.queue.dlq-name}", factory = "manualAckSqsListenerContainerFactory")
    public void receberMensagem(String payload, Acknowledgement acknowledgement, Message originalMessage) {
        log.info("Mensagem recebida da DLQ. queueName={}, messageId={}", queueName, originalMessage.messageId());

        try {
            String failureReason = extrairMotivoFalha(originalMessage);
            EventoPedidoFalho eventoPedidoFalho = null;

            try {
                eventoPedidoFalho = pedidoDlqPayloadMapper.toDomain(payload);
            } catch (JsonProcessingException exception) {
                failureReason = combinarMotivos(
                        failureReason,
                        "Nao foi possivel interpretar o payload da DLQ para triagem automatica: " + exception.getOriginalMessage()
                );
            }

            RegistroAuditoriaErro registro = registrarErroDlqUseCase.registrar(
                    new RegistrarErroDlqCommand(queueName, payload, failureReason, eventoPedidoFalho)
            );

            acknowledgement.acknowledge();
            log.info(
                    "Mensagem removida da DLQ apos persistencia segura. errorId={}, severity={}",
                    registro.errorId(),
                    registro.severity()
            );
        } catch (Exception exception) {
            log.error("Falha ao auditar mensagem da DLQ. queueName={}, messageId={}", queueName, originalMessage.messageId(), exception);
            throw exception;
        }
    }

    private String extrairMotivoFalha(Message originalMessage) {
        Map<String, MessageAttributeValue> messageAttributes = originalMessage.messageAttributes();
        List<String> motivos = new ArrayList<>();

        if (messageAttributes != null) {
            for (String atributo : POSSIVEIS_ATRIBUTOS_DE_ERRO) {
                MessageAttributeValue valor = messageAttributes.get(atributo);
                if (valor != null && valor.stringValue() != null && !valor.stringValue().isBlank()) {
                    motivos.add(atributo + "=" + valor.stringValue());
                }
            }
        }

        if (!motivos.isEmpty()) {
            return String.join(" | ", motivos);
        }

        String receiveCount = originalMessage.attributesAsStrings().get("ApproximateReceiveCount");
        if (receiveCount != null && !receiveCount.isBlank()) {
            return "Mensagem movida para a DLQ apos " + receiveCount
                    + " tentativas sem detalhe do erro original nos atributos da mensagem.";
        }

        return "Mensagem movida para a DLQ sem detalhe do erro original nos atributos da mensagem.";
    }

    private String combinarMotivos(String motivoOriginal, String motivoComplementar) {
        if (motivoOriginal == null || motivoOriginal.isBlank()) {
            return motivoComplementar;
        }
        return motivoOriginal + " | " + motivoComplementar;
    }
}
