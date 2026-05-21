package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciuscecatto.servicoauditordlq.domain.model.EventoPedidoFalho;
import com.viniciuscecatto.servicoauditordlq.domain.model.ItemPedidoFalho;
import com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.in.sqs.dto.PedidoDlqPayload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PedidoDlqPayloadMapper {

    private final ObjectMapper objectMapper;

    public PedidoDlqPayloadMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventoPedidoFalho toDomain(String rawPayload) throws JsonProcessingException {
        PedidoDlqPayload payload = objectMapper.readValue(rawPayload, PedidoDlqPayload.class);

        List<ItemPedidoFalho> itens = payload.orderItems() == null
                ? List.of()
                : payload.orderItems().stream()
                .map(item -> new ItemPedidoFalho(item.sku(), item.amount()))
                .toList();

        return new EventoPedidoFalho(
                payload.zipCode(),
                payload.customerId(),
                itens,
                payload.origin(),
                payload.occurredAt()
        );
    }
}
