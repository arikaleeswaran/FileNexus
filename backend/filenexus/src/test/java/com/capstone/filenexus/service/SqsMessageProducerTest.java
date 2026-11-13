package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.message.ScanMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SqsMessageProducerTest {

    @Autowired
    private SqsMessageProducer sqsMessageProducer;

    @Test
    void shouldSendScanMessageToSqs() {

        ScanMessage message = new ScanMessage(
                4L,
                12L,
                "filenexus-quarantine-143",
                "quarantine/test/sample.pdf",
                "test-checksum",
                "application/pdf",
                "sample.pdf",
                1,
                UUID.randomUUID().toString()
        );

        String messageId =
                sqsMessageProducer.sendScanMessage(message);

        assertNotNull(messageId);

        System.out.println("SQS Message ID: " + messageId);
    }
}