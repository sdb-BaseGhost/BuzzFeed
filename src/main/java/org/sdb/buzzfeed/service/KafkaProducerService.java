package org.sdb.buzzfeed.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC = "test-topic";

    public void sendMessage(String message) {
        System.out.println("🚀 发送消息: " + message);
        kafkaTemplate.send(TOPIC, message);
    }
}
