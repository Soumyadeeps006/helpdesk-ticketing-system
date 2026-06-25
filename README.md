# IT Help Desk Ticketing System

A modern, full-stack REST API for managing IT support tickets, built with **Spring Boot 3**, **Spring Data JPA**, **RabbitMQ**, and **PostgreSQL**. 

This system provides a robust backend for ticket lifecycle management, complete with asynchronous notifications, file attachments, and strict timezone handling.

---

## Table of Contents

1. [Features](#features)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Getting Started](#getting-started)
5. [Database Schema](#database-schema)
6. [API Route Reference](#api-route-reference)
7. [Running Tests](#running-tests)
8. [Packaging & Deployment](#packaging--deployment)
9. [Known Limitations & Future Work](#known-limitations--future-work)

---

## Features

- **RESTful API**: Complete CRUD and state-transition endpoints for tickets.
- **Ticket Lifecycle**: Strict state-guard enforcement (`OPEN → IN_PROGRESS → CLOSED`).
- **Priority Management**: `LOW` / `MEDIUM` / `HIGH` / `CRITICAL` with audit logging on changes.
- **Async Notifications**: Decoupled event publishing via **RabbitMQ** (messages are only sent *after* the database transaction successfully commits).
- **File Attachments**: Upload screenshots and logs to tickets, stored locally (easily swappable to AWS S3/MinIO).
- **Strict Timezone Handling**: All timestamps are stored in the database as `TIMESTAMP WITH TIME ZONE` and serialized to UTC in JSON responses.
- **Immutable Audit Trail**: Every state change, priority update, or creation is logged with the actor and timestamp.
- **Zero-Install Build**: Includes the **Maven Wrapper** (`mvnw`) so developers don't need to install Maven globally.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Build | Maven 3.9+ (via Maven Wrapper) |
| Language | Java 21 |
| Framework | Spring Boot 3.4+ |
| ORM | Spring Data JPA / Hibernate 6 |
| Messaging | RabbitMQ (Spring AMQP) |
| Database | PostgreSQL 15+ |
| Validation | Jakarta Validation (Hibernate Validator) |
| Testing | JUnit 5, Spring Boot Test, H2 (in-memory) |

---

## Prerequisites

- **JDK 21+**
- **PostgreSQL 15+**
- **RabbitMQ** (Can be run locally via Docker: `docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management`)

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Soumyadeeps006/helpdesk-ticketing-system.git
cd helpdesk-ticketing-system
```

### 2. Configure the database

Create a PostgreSQL database:

```sql
    CREATE DATABASE helpdesk_db;
    -- Default credentials in application.yml are postgres/postgres
```

Apply the schema:

```bash
    mysql -u helpdesk -p helpdesk < src/main/resources/schema.sql
```

### 3. Configure the application

The application uses application.yml for configuration. By default, it connects to a local PostgreSQL database and RabbitMQ instance.
To override settings for local development, create an application-dev.yml in src/main/resources/ or pass them as environment variables.

### 4. Build and run

Thanks to the Maven Wrapper, you do not need Maven installed on your system.

```bash
    # Linux / macOS
    ./mvnw spring-boot:run

    # Windows
    mvnw.cmd spring-boot:run
```

The API will be available at http://localhost:8080/api/tickets.

---

## Database Schema

Hibernate automatically manages the schema via ddl-auto: update in the dev profile. The core tables are:

```sql
    -- Users (Simplified for this context)
    CREATE TABLE users (
        id BIGSERIAL PRIMARY KEY,
        username VARCHAR(100) UNIQUE NOT NULL,
        email VARCHAR(255) UNIQUE NOT NULL
    );

    -- Tickets
    CREATE TABLE tickets (
        id BIGSERIAL PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        description TEXT,
        status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
        priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
        updated_at TIMESTAMP WITH TIME ZONE NOT NULL
    );

    -- Attachments
    CREATE TABLE attachments (
        id BIGSERIAL PRIMARY KEY,
        ticket_id BIGINT REFERENCES tickets(id) ON DELETE CASCADE,
        original_filename VARCHAR(255) NOT NULL,
        stored_filename VARCHAR(255) UNIQUE NOT NULL,
        content_type VARCHAR(100),
        file_size BIGINT,
        file_path VARCHAR(500) NOT NULL
    );

    -- Audit Logs
    CREATE TABLE audit_logs (
        id BIGSERIAL PRIMARY KEY,
        ticket_id BIGINT REFERENCES tickets(id) ON DELETE CASCADE,
        actor_id BIGINT REFERENCES users(id),
        action VARCHAR(100) NOT NULL,
        old_value TEXT,
        new_value TEXT,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    );
```

---

## API Route Reference

### Ticket routes (Spring MVC `TicketController`)

| Method | URL | Description |
|---|---|---|
| `GET` | `api/tickets` | List all tickets |
| `GET` | `api/tickets/{id}` | Get ticket details by ID |
| `POST` | `api/tickets/new` | Create a new ticket (Triggers RabbitMQ event) |
| `PUT` | `api/tickets/{id}/start` | Move ticket to IN_PROGRESS and assign |
| `PUT` | `api/tickets/{id}/close` | Close the ticket |
| `PUT` | `api/tickets/{id}/priority` | Update ticket priority |

### Comment routes (Spring MVC `CommentController`)

| Method | URL | Description |
|---|---|---|
| `POST` | `/tickets/{id}/comments` | Add root comment |
| `POST` | `/tickets/{id}/comments/{parentId}/reply` | Add nested reply |

### Servlet endpoints (J2EE `HttpServlet`)

| Method | URL | Description |
|---|---|---|
| `POST` | `/notify` | Trigger email notification (async) |
| `POST` | `/audit` | Direct audit write endpoint |
| `GET` | `/audit?ticketId={id}` | Fetch audit trail as JSON |

### Timeline view

| Method | URL | Description |
|---|---|---|
| `GET` | `/tickets/{id}/timeline` | Rendered audit timeline (JSP) |

### Attachment Endpoints (AttachmentController)
| Method | URL | Description |
|---|---|---|
| `POST` | `/api/tickets/{ticketId}/attachments` | Upload a file to a specific ticket |

Note: RabbitMQ events are published asynchronously on ticket creation, status changes, and priority updates using the routing key pattern ticket.event.#.

---

## Running Tests

Run the unit and integration tests using the Maven Wrapper:

```bash
    # Linux / macOS
    ./mvnw test

    # Windows
    mvnw.cmd test
```

The test suite uses an in-memory H2 database and mocks RabbitMQ to ensure fast, isolated testing without requiring external services.

## Packaging & Deployment

To build a production-ready executable JAR:

```bash
    ./mvnw clean package -DskipTests
```

The artifact will be generated at target/helpdesk-ticketing-system-0.0.1-SNAPSHOT.jar.

Run it in production:

```bash
    java -jar target/helpdesk-ticketing-system-0.0.1-SNAPSHOT.jar \
     --spring.datasource.url=jdbc:postgresql://prod-db:5432/helpdesk \
     --spring.rabbitmq.host=prod-rabbitmq
```
---

## Known Limitations

* Authentication & Authorization: Currently, there is no security context. Any user ID can be passed in the API. Future: Integrate Spring Security with JWT or OAuth2.
* Local File Storage: Files are currently saved to the local ./uploads directory. Future: Implement an S3StorageService that implements the same interface as LocalStorageService to upload directly to AWS S3 or MinIO.
* Pagination in Controllers: While TicketService supports pagination (getTicketsPage), the TicketController currently returns all tickets. Future: Add Pageable parameters to the REST endpoints.
* RabbitMQ Dead Letter Queues: If the notification listener fails, the message is currently lost. Future: Configure a Dead Letter Exchange (DLX) to handle and retry failed notifications.
