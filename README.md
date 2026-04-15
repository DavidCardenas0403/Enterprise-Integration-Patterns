# Enterprise Integration Patterns

A multi-module Gradle project demonstrating the four main Enterprise Integration Patterns (EIP).

## Modules

### 01-point-to-point
**Pattern:** Point-to-Point  
**Technology:** REST API + gRPC Gateway (Spring Boot)  
**Port:** 8080 (REST), 9090 (gRPC)

### 02-pubsub  
**Pattern:** Publication/Subscription  
**Technology:** Apache Kafka  
**Port:** 8081

### 03-message-queue  
**Pattern:** Message Queue  
**Technology:** RabbitMQ (Queue mode)  
**Port:** 8082

### 04-message-broker  
**Pattern:** Message Broker (Routing + Transformation)  
**Technology:** RabbitMQ (Topic Exchange)  
**Port:** 8083

## Prerequisites

- Java 17+
- Gradle 8+
- Docker (for Kafka/RabbitMQ containers)

## Running

```bash
# Start infrastructure
docker-compose up -d

# Build all modules
./gradlew build

# Run a specific module
./gradlew :01-point-to-point:bootRun
```

## Infrastructure

```yaml
# docker-compose.yml
version: '3'
services:
  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092

  rabbitmq:
    image: rabbitmq:management
    ports:
      - "5672:5672"
      - "15672:15672"
```