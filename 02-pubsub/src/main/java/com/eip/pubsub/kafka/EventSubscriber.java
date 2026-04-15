package com.eip.pubsub.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventSubscriber {

    @KafkaListener(topics = "eip-events-topic", groupId = "eip-subscriber-group")
    public void subscribe(String event) {
        System.out.println("Received event: " + event);
    }
}