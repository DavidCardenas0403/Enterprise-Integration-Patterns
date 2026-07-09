# Enterprise Integration Patterns

A multi-module Gradle project demonstrating four core **Enterprise Integration Patterns (EIP)**, each implemented as an independent Spring Boot application. Every module is a runnable microservice that isolates one messaging style so you can study it on its own.

| # | Module | Pattern | Technology | HTTP Port | Extra Port |
|---|--------|---------|------------|-----------|------------|
| 01 | `01-point-to-point` | Point-to-Point | REST + gRPC (Spring Boot) | 8080 | 9090 (gRPC) |
| 02 | `02-pubsub` | Publish / Subscribe | Apache Kafka | 8081 | — |
| 03 | `03-message-queue` | Message Queue | RabbitMQ (default exchange → queue) | 8082 | — |
| 04 | `04-message-broker` | Message Broker (routing) | RabbitMQ (topic exchange) | 8083 | — |

---

## Tech stack

- **Java 25** (source/target compatibility set in the root `build.gradle`)
- **Gradle 9.2.1** via the committed wrapper (`./gradlew`) — no local Gradle install needed
- **Spring Boot 3.4.4** with the Spring Dependency Management plugin
- **Apache Kafka** (module 02) and **RabbitMQ** (modules 03 & 04), provisioned with Docker Compose
- **gRPC / Protobuf** (module 01) via the `grpc-spring-boot-starter` and the `protobuf` Gradle plugin

---

## Prerequisites

- **JDK 25** — check with `java -version`
- **Docker + Docker Compose** — only needed for modules 02, 03, 04 (Kafka/RabbitMQ). Module 01 needs nothing extra.
- `curl` (or any HTTP client) to exercise the REST endpoints
- Everything else (Gradle, Spring Boot, gRPC tooling) is pulled in automatically by the build

---

## Repository layout

```
Enterprise-Integration-Patterns/
├── build.gradle              # Root aggregator: shared config for all subprojects
├── settings.gradle           # Declares the 4 subprojects
├── gradlew / gradlew.bat     # Gradle wrapper scripts (use these to build/run)
├── gradle/wrapper/           # Pinned Gradle 9.2.1 distribution metadata
├── docker-compose.yaml       # Kafka (KRaft) + RabbitMQ infrastructure
│
├── 01-point-to-point/        # Pattern 1 — REST + gRPC
├── 02-pubsub/                # Pattern 2 — Kafka publish/subscribe
├── 03-message-queue/         # Pattern 3 — RabbitMQ work queue
└── 04-message-broker/        # Pattern 4 — RabbitMQ topic-based routing
```

### Root build files

- **`settings.gradle`** — names the root project (`enterprise-integration-patterns`) and `include`s the four modules so Gradle treats them as a single multi-project build.
- **`build.gradle`** (root) — an *aggregator only*. It declares the Spring Boot and Dependency-Management plugins with `apply false` (so the root itself never tries to produce a runnable jar) and then, via the `subprojects { }` block, applies the shared configuration to every module: the `java` plugin, Java 25 compatibility, the Maven Central repository, and the Spring Boot plugins. Each module keeps only its own dependencies.
- **`docker-compose.yaml`** — defines the messaging infrastructure (see [Infrastructure](#infrastructure)).

---

## Quick start

```bash
# 1. Start Kafka + RabbitMQ (only needed for modules 02/03/04)
docker compose up -d

# 2. Build every module (compiles, generates gRPC stubs, produces boot jars)
./gradlew build

# 3. Run whichever module you want (each blocks its terminal)
./gradlew :01-point-to-point:bootRun
./gradlew :02-pubsub:bootRun
./gradlew :03-message-queue:bootRun
./gradlew :04-message-broker:bootRun
```

Build a single module without the others: `./gradlew :02-pubsub:build`.
Stop the infrastructure when finished: `docker compose down`.

---

## Infrastructure

`docker-compose.yaml` starts the two brokers the modules depend on:

- **`kafka`** — `confluentinc/cp-kafka:7.6.1` running in **KRaft mode** (no ZooKeeper). It exposes `localhost:9092` to apps on the host and `kafka:29092` to other containers, and auto-creates topics. A healthcheck (`kafka-topics --list`) reports readiness.
- **`rabbitmq`** — `rabbitmq:3-management`. AMQP is on `5672` (used by the apps); the management UI is on **http://localhost:15672** (login `guest` / `guest`). A `rabbitmq-diagnostics ping` healthcheck reports readiness.

Wait until both are healthy before starting the messaging modules:

```bash
docker compose up -d
docker compose ps            # STATUS should show "healthy"
```

---

## Module 01 — Point-to-Point (`01-point-to-point`)

**Pattern.** A message goes from exactly one sender to exactly one receiver — a direct request/response call. This module shows the pattern over two transports: HTTP REST and gRPC.

**How it works.** Spring Boot starts an embedded Tomcat server (port 8080) for the REST controller and, via `grpc-spring-boot-starter`, a separate gRPC server (port 9090). A client calls one endpoint and gets one response back — no intermediary broker involved.

**Files**

| File | Purpose |
|------|---------|
| `build.gradle` | Adds `spring-boot-starter-web`, the gRPC starter, the gRPC/protobuf libraries, and the `com.google.protobuf` plugin that compiles `.proto` files into Java stubs at build time. |
| `src/main/proto/eip.proto` | The gRPC contract: an `OrderService` with a `createOrder` RPC plus the `OrderRequest`/`OrderResponse` messages. Gradle generates Java classes from this into `build/generated/`. |
| `src/main/java/com/eip/ptp/PointToPointApplication.java` | Spring Boot entry point (`main` method). |
| `src/main/java/com/eip/ptp/rest/OrderRestController.java` | REST endpoints: `POST /api/orders` (creates an order, returns a generated UUID) and `GET /api/orders/{orderId}`. |
| `src/main/java/com/eip/ptp/grpc/OrderGrpcService.java` | gRPC service implementation extending the generated `OrderServiceGrpc.OrderServiceImplBase`; handles the `createOrder` RPC. |
| `src/main/resources/application.properties` | Sets `server.port=8080` and `grpc.server.port=9090`. |

**Run & test**

```bash
./gradlew :01-point-to-point:bootRun     # no Docker required

# REST — create an order
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"product":"widget","quantity":3}'
# → {"message":"Order received via REST","status":"CREATED","orderId":"<uuid>"}

# REST — fetch an order
curl http://localhost:8080/api/orders/abc-123
# → {"message":"Order found","status":"PROCESSING","orderId":"abc-123"}
```

For gRPC (port 9090), use a gRPC client such as [`grpcurl`](https://github.com/fullstorydev/grpcurl):

```bash
grpcurl -plaintext \
  -d '{"order_id":"o-1","product":"gizmo","quantity":2}' \
  localhost:9090 com.eip.ptp.OrderService/createOrder
```

---

## Module 02 — Publish / Subscribe (`02-pubsub`)

**Pattern.** A publisher sends an event to a topic; every interested subscriber receives its own copy. Sender and receivers are decoupled and don't know about each other.

**How it works.** A REST endpoint publishes a string to the Kafka topic `eip-events-topic` using `KafkaTemplate`. A `@KafkaListener` in the same app consumes from that topic (consumer group `eip-subscriber-group`) and logs each event. In a real system the subscriber would live in a separate service — here they run together to make the round-trip observable in one log.

**Files**

| File | Purpose |
|------|---------|
| `build.gradle` | Adds `spring-boot-starter-web` and `spring-kafka`. |
| `src/main/java/com/eip/pubsub/PubSubApplication.java` | Spring Boot entry point. |
| `src/main/java/com/eip/pubsub/kafka/EventPublisher.java` | REST controller exposing `POST /api/events/publish`; sends the request body to the Kafka topic. |
| `src/main/java/com/eip/pubsub/kafka/EventSubscriber.java` | `@KafkaListener` that consumes from `eip-events-topic` and prints `Received event: ...`. |
| `src/main/resources/application.properties` | `server.port=8081`, Kafka bootstrap server `localhost:9092`, consumer group, `auto-offset-reset=earliest`, and the String (de)serializers. |

**Run & test**

```bash
docker compose up -d kafka                 # ensure Kafka is healthy first
./gradlew :02-pubsub:bootRun

curl -X POST http://localhost:8081/api/events/publish \
  -H 'Content-Type: text/plain' -d 'hello-eip'
# → Event published: hello-eip
```

Watch the application console — you'll see `Received event: hello-eip`, confirming the publish→subscribe round trip.

---

## Module 03 — Message Queue (`03-message-queue`)

**Pattern.** A producer drops a message on a queue; a single consumer pulls it off and processes it. Unlike pub/sub, each message is delivered to **one** consumer — the classic work-queue / load-levelling pattern.

**How it works.** On startup a durable-less queue named `eip-message-queue` is declared as a Spring bean. A REST endpoint pushes the request body onto that queue via `RabbitTemplate` (using RabbitMQ's default exchange, where the routing key equals the queue name). A `@RabbitListener` consumes messages from the queue and logs them.

**Files**

| File | Purpose |
|------|---------|
| `build.gradle` | Adds `spring-boot-starter-web` and `spring-boot-starter-amqp`. |
| `src/main/java/com/eip/mqueue/MessageQueueApplication.java` | Spring Boot entry point. |
| `src/main/java/com/eip/mqueue/amqp/MessageQueueProducer.java` | Declares the `eip-message-queue` `Queue` bean and exposes `POST /api/messages/send`, which sends the body to the queue. Holds the shared `QUEUE_NAME` constant. |
| `src/main/java/com/eip/mqueue/amqp/MessageQueueConsumer.java` | `@RabbitListener` on `eip-message-queue`; prints `Message consumed from queue: ...`. |
| `src/main/resources/application.properties` | `server.port=8082` and RabbitMQ connection (`localhost:5672`, `guest`/`guest`). |

**Run & test**

```bash
docker compose up -d rabbitmq              # ensure RabbitMQ is healthy first
./gradlew :03-message-queue:bootRun

curl -X POST http://localhost:8082/api/messages/send \
  -H 'Content-Type: text/plain' -d 'order-42'
# → Message sent to queue: order-42
```

Console shows `Message consumed from queue: order-42`. You can also inspect the queue in the RabbitMQ UI at http://localhost:15672.

---

## Module 04 — Message Broker (`04-message-broker`)

**Pattern.** A broker sits between producers and consumers and **routes** each message to the right destination based on its content — here, a routing key. One producer, many possible destinations, decided at runtime.

**How it works.** A **topic exchange** `eip-broker-exchange` is declared along with two queues, `email-queue` and `sms-queue`. Bindings route messages by routing-key pattern:

- `notification.email.*` → `email-queue`
- `notification.sms.*` → `sms-queue`

A REST endpoint takes a JSON body with a `routingKey` and `message`, and publishes to the exchange. RabbitMQ then delivers the message to whichever queue(s) match the key. Two `@RabbitListener`s (one per queue) log what they receive.

**Files**

| File | Purpose |
|------|---------|
| `build.gradle` | Adds `spring-boot-starter-web` and `spring-boot-starter-amqp`. |
| `src/main/java/com/eip/broker/MessageBrokerApplication.java` | Spring Boot entry point. |
| `src/main/java/com/eip/broker/amqp/BrokerProducer.java` | Declares the topic exchange, both queues, and their bindings; exposes `POST /api/broker/send` taking `{routingKey, message}` (via the inner `MessageRequest` DTO) and publishes to the exchange. |
| `src/main/java/com/eip/broker/amqp/BrokerConsumers.java` | Two `@RabbitListener`s — one on `email-queue`, one on `sms-queue` — that log the messages routed to them. |
| `src/main/resources/application.properties` | `server.port=8083` and RabbitMQ connection (`localhost:5672`, `guest`/`guest`). |

**Run & test**

```bash
docker compose up -d rabbitmq              # ensure RabbitMQ is healthy first
./gradlew :04-message-broker:bootRun

# Routed to the EMAIL queue
curl -X POST http://localhost:8083/api/broker/send \
  -H 'Content-Type: application/json' \
  -d '{"routingKey":"notification.email.welcome","message":"Welcome!"}'

# Routed to the SMS queue
curl -X POST http://localhost:8083/api/broker/send \
  -H 'Content-Type: application/json' \
  -d '{"routingKey":"notification.sms.otp","message":"Your code is 1234"}'
```

The console shows `Email consumer received: Welcome!` for the first call and `SMS consumer received: Your code is 1234` for the second — demonstrating content-based routing.

---

## Verification status

Verified on JDK 25 / Gradle 9.2.1 / Spring Boot 3.4.4:

- ✅ **`./gradlew build`** — all four modules compile and produce boot jars; module 01's gRPC/protobuf stubs generate correctly.
- ✅ **Module 01** — REST endpoints return the expected payloads; the gRPC server starts and listens on 9090.
- ✅ **Module 02** — a published event is received by the Kafka subscriber end-to-end.
- ✅ **Modules 03 & 04** — compile and start; they use the same Spring AMQP pattern as the verified modules. To run them you need the RabbitMQ image (`docker compose up -d rabbitmq`).

> **Note on the initial build fix:** the root `build.gradle` originally applied the Spring Boot plugin to the root project itself, which made Gradle try to build a `bootJar` for the (source-less) aggregator and fail with *"no repositories are defined"*. The root now declares those plugins with `apply false` and applies them only to the subprojects.

---

## Troubleshooting

- **`no repositories are defined` / root `bootJar` fails** — make sure the root `build.gradle` uses `apply false` for the Spring Boot plugins (already fixed here).
- **Module 02 logs `UNKNOWN_TOPIC_OR_PARTITION` on startup** — harmless; the topic is auto-created on first publish and the consumer reconnects.
- **Kafka/RabbitMQ connection refused** — the broker isn't ready yet. Run `docker compose ps` and wait for `healthy` before starting the module.
- **Docker image pull is cancelled on macOS** (`error getting credentials … User canceled`) — the Docker credential keychain prompt was dismissed. Re-run `docker compose up -d` and approve the keychain prompt, or `docker login` once so credentials are cached.
- **Port already in use** — another process holds 8080–8083, 9090, 9092, or 5672. Stop it or change the port in the module's `application.properties` / `docker-compose.yaml`.
