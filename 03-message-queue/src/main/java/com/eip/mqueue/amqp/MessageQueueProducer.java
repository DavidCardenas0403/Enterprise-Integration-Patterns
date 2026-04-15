package com.eip.mqueue.amqp;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

@Configuration
@RestController
@RequestMapping("/api/messages")
public class MessageQueueProducer {

    public static final String QUEUE_NAME = "eip-message-queue";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, false);
    }

    private final RabbitTemplate rabbitTemplate;

    public MessageQueueProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestBody String message) {
        rabbitTemplate.convertAndSend(QUEUE_NAME, message);
        return "Message sent to queue: " + message;
    }
}