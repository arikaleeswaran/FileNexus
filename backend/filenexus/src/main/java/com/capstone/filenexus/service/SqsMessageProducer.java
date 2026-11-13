package com.capstone.filenexus.service;

import com.capstone.filenexus.dto.message.ScanMessage;

public interface SqsMessageProducer {

    String sendScanMessage(ScanMessage message);
}