package com.viniciuscecatto.servicoauditordlq.infrastructure.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class AwsSqsListenerConfiguration {

    @Bean
    @ConditionalOnBean(SqsAsyncClient.class)
    @ConditionalOnProperty(name = "aws.sqs.listener.enabled", havingValue = "true", matchIfMissing = true)
    public SqsMessageListenerContainerFactory<Object> manualAckSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient
    ) {
        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.acknowledgementMode(AcknowledgementMode.MANUAL))
                .build();
    }
}
