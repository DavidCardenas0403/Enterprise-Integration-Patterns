package com.eip.broker.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BrokerConsumers {

    @RabbitListener(queues = "email-queue")
    public void consumeEmail(String message) {
        System.out.println("Email consumer received: " + message);
    }

    @RabbitListener(queues = "sms-queue")
    public void consumeSms(String message) {
        System.out.println("SMS consumer received: " + message);
    }
}