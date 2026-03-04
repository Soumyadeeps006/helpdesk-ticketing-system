# IT Help Desk Ticketing System

A full-stack Java web application for managing IT support tickets, built with **Spring MVC**, **Hibernate ORM**, **J2EE Servlets**, **JSP/JSTL**, and **MySQL**.

---

## Table of Contents

1. [Features](#features)
2. [Technology Stack](#technology-stack)
3. [Prerequisites](#prerequisites)
4. [Getting Started](#getting-started)
5. [Database Schema](#database-schema)
6. [API Route Reference](#api-route-reference)
7. [Running Tests](#running-tests)
8. [WAR Packaging & Deployment](#war-packaging--deployment)
9. [Known Limitations](#known-limitations)

---

## Features

- Create, view, assign, and resolve support tickets
- Priority levels: `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`
- Ticket lifecycle: `OPEN → IN_PROGRESS → CLOSED` with state-guard enforcement
- Threaded comments with unlimited nesting (self-referencing tree)
- Immutable audit trail — every write operation is logged with actor and timestamp
- Background notification and audit servlets (J2EE lifecycle)
- Bootstrap 5 responsive UI with a timeline view for the audit log

---

## Technology Stack

| Layer | Technology |
|---|---|
| Build | Maven 3.9+ |
| Language | Java 17 |
| Web framework | Spring MVC 6 |
| ORM | Hibernate 6 / Jakarta Persistence |
| Servlets | Jakarta Servlet 5 (Tomcat 10) |
| View | JSP 3 + JSTL 3 |
| Database (prod) | MySQL 8 |
| Database (test) | H2 (in-memory) |
| Frontend | Bootstrap 5.3 |

---

## Prerequisites

- JDK 17+
- Maven 3.9+
- MySQL 8 (for production use)
- Apache Tomcat 10.1 (for deployment)

---

## Getting Started

### 1. Clone the repository

```bash
    git clone https://github.com/Soumyadeeps006/helpdesk-ticketing-system.git
    cd helpdesk-ticketing-system
```

### 2. Configure the database

Create a MySQL database and user:

```sql
    CREATE DATABASE helpdesk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE USER 'helpdesk'@'localhost' IDENTIFIED BY 'changeme';
    GRANT ALL PRIVILEGES ON helpdesk.* TO 'helpdesk'@'localhost';
```

Apply the schema:

```bash
    mysql -u helpdesk -p helpdesk < src/main/resources/schema.sql
```

### 3. Update Hibernate configuration

Edit `src/main/resources/hibernate.cfg.xml` and set your credentials:

```xml
    <property name="hibernate.connection.url">
        jdbc:mysql://localhost:3306/helpdesk?useSSL=false&amp;serverTimezone=UTC
    </property>
    <property name="hibernate.connection.username">helpdesk</property>
    <property name="hibernate.connection.password">changeme</property>
```

### 4. Build and deploy

```bash
    mvn clean package
    cp target/helpdesk.war $CATALINA_HOME/webapps/
    $CATALINA_HOME/bin/startup.sh
```

Open `http://localhost:8080/helpdesk/` in your browser.

---

## Database Schema

```
    users
    id          BIGINT PK AUTO_INCREMENT
    username    VARCHAR(100) UNIQUE NOT NULL
    email       VARCHAR(255) UNIQUE NOT NULL

    tickets
    id              BIGINT PK AUTO_INCREMENT
    title           VARCHAR(255) NOT NULL
    description     TEXT
    status          ENUM('OPEN','IN_PROGRESS','CLOSED') NOT NULL DEFAULT 'OPEN'
    priority        ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL
    reporter_id     BIGINT FK → users.id
    assignee_id     BIGINT FK → users.id (nullable)
    resolution_note TEXT
    created_at      DATETIME NOT NULL
    updated_at      DATETIME NOT NULL

    comments
    id          BIGINT PK AUTO_INCREMENT
    ticket_id   BIGINT FK → tickets.id NOT NULL
    author_id   BIGINT FK → users.id NOT NULL
    parent_id   BIGINT FK → comments.id (nullable — NULL for root comments)
    body        TEXT NOT NULL
    created_at  DATETIME NOT NULL

    audit_logs
    id          BIGINT PK AUTO_INCREMENT
    ticket_id   BIGINT FK → tickets.id NOT NULL
    actor_id    BIGINT FK → users.id NOT NULL
    action      VARCHAR(100) NOT NULL
    detail      TEXT
    created_at  DATETIME NOT NULL
```

---

## API Route Reference

### Ticket routes (Spring MVC `TicketController`)

| Method | URL | Description |
|---|---|---|
| `GET` | `/tickets` | List all tickets |
| `GET` | `/tickets/{id}` | Ticket detail view |
| `GET` | `/tickets/new` | New-ticket form |
| `POST` | `/tickets/create` | Submit new ticket |
| `POST` | `/tickets/{id}/assign` | Assign ticket to agent |
| `POST` | `/tickets/{id}/progress` | Move ticket to IN_PROGRESS |
| `POST` | `/tickets/{id}/close` | Close ticket with resolution note |

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

---

## Running Tests

### Unit tests (H2 in-memory, no Tomcat needed)

```bash
    mvn test
```

17 unit tests across `TicketServiceTest` (9) and `CommentDAOTest` (8).

### Integration tests (Day 5)

```bash
    mvn verify -P integration-tests
```

| Test class | Coverage |
|---|---|
| `TicketWorkflowIT` | Full lifecycle: OPEN → IN_PROGRESS → CLOSED |
| `AuditTrailIT` | Every service write produces a correct AuditLog entry |
| `CommentTreeIT` | Parent/child tree via `findTreeByTicketId` JOIN FETCH |

---

## WAR Packaging & Deployment

```bash
    # Build the WAR
    mvn clean package

    # The artifact is at:
    target/helpdesk.war

    # Deploy to Tomcat 10 (hot-deploy)
    cp target/helpdesk.war $CATALINA_HOME/webapps/

    # Smoke-test key routes
    curl -I http://localhost:8080/helpdesk/tickets
    curl -I http://localhost:8080/helpdesk/tickets/new
```

---

## Known Limitations

- **No authentication or authorisation** — any user can perform any action. Integrate Spring Security for production use.
- **No pagination** — the `/tickets` list fetches all rows. Add server-side pagination for large datasets.
- **Email notifications are fire-and-forget** — `NotificationServlet` logs but does not retry on failure. Consider a proper message queue (e.g., RabbitMQ) for reliability.
- **H2 dialect differences** — some MySQL-specific DDL (e.g., `ENUM` columns) is shimmed for H2 in tests. Always verify migrations against the real MySQL schema before deploying.
- **No file attachments** — tickets cannot have attached screenshots or logs. A future iteration could integrate S3 or a local file store.
- **Timestamps are server-local** — no timezone conversion is applied in the UI. Consider storing UTC and converting in the JSP layer with `fmt:timeZone`.
