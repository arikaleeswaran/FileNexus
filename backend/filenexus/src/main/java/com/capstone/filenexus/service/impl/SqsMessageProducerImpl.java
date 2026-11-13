package com.capstone.filenexus.service.impl;

import com.capstone.filenexus.dto.message.ScanMessage;
import com.capstone.filenexus.service.SqsMessageProducer;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
public class SqsMessageProducerImpl implements SqsMessageProducer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String scanQueueUrl;

    public SqsMessageProducerImpl(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${filenexus.aws.sqs.scan-queue-url}")
            String scanQueueUrl
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.scanQueueUrl = scanQueueUrl;
    }

    @Override
    public String sendScanMessage(ScanMessage message) {

        try {
            String messageBody = objectMapper.writeValueAsString(message);

            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(scanQueueUrl)
                    .messageBody(messageBody)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(request);

            return response.messageId();

        } catch (JacksonException  exception) {
            throw new IllegalStateException(
                    "Unable to convert scan message to JSON",
                    exception
            );
        }
    }
}