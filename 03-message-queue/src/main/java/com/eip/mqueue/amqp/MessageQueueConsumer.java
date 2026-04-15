package com.eip.mqueue.amqp;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageQueueConsumer {

    @RabbitListener(queues = MessageQueueProducer.QUEUE_NAME)
    public void consume(String message) {
        System.out.println("Message consumed from queue: " + message);
    }
}