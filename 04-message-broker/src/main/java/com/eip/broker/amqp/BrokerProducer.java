package com.eip.broker.amqp;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;

@Configuration
@RestController
@RequestMapping("/api/broker")
public class BrokerProducer {

    public static final String EXCHANGE_NAME = "eip-broker-exchange";

    @Bean
    public TopicExchange brokerExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue("email-queue", false);
    }

    @Bean
    public Queue smsQueue() {
        return new Queue("sms-queue", false);
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange brokerExchange) {
        return BindingBuilder.bind(emailQueue).to(brokerExchange).with("notification.email.*");
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange brokerExchange) {
        return BindingBuilder.bind(smsQueue).to(brokerExchange).with("notification.sms.*");
    }

    private final RabbitTemplate rabbitTemplate;

    public BrokerProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/send")
    public String sendMessage(@RequestBody MessageRequest request) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, request.getRoutingKey(), request.getMessage());
        return "Message sent via broker with routing key: " + request.getRoutingKey();
    }

    public static class MessageRequest {
        private String routingKey;
        private String message;

        public String getRoutingKey() { return routingKey; }
        public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}