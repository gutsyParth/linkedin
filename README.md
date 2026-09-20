# LinkedIn System: Event-Driven Microservices Backend

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Event%20Streaming-black.svg)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-Feed%20Cache-red.svg)](https://redis.io/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-Search-blue.svg)](https://www.elastic.co/elasticsearch)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)

A **LinkedIn-inspired backend system** built to explore real-world **microservices architecture, event-driven communication, distributed data ownership, caching, search indexing, authentication, and object storage** using the Java/Spring ecosystem.

The system is split into **six independently running Spring Boot services** connected through REST APIs and Apache Kafka events. Each service owns a focused responsibility and uses the datastore or infrastructure component best suited to that responsibility.

> This project focuses on backend architecture and distributed-system design rather than reproducing LinkedIn's production infrastructure.

---

## Architecture

### Core architectural ideas

- **API Gateway as the single entry point** for client requests and centralized JWT validation.
- **Database-per-responsibility approach**: relational business data in MySQL, feed data in Redis, searchable documents in Elasticsearch, and media in S3.
- **Event-driven communication** through Kafka so services can react independently to changes.
- **Fan-out on write** for feed generation: when a post is created, its ID is pushed into the feeds of the author's connections.
- **Stateless notification processing**: notifications are created from incoming Kafka event payloads and are not persisted.
- **REST-based synchronous communication** where immediate data is required, such as Feed Service calling User Service through OpenFeign.

---

## Microservices

| Service | Port | Responsibility |
|---|---:|---|
| **API Gateway** | `8080` | Central routing, JWT validation, protected-route access, propagation of authenticated user information |
| **User Service** | `8081` | Registration, login, profiles, connections, JWT generation, profile media |
| **Post Service** | `8082` | Posts, likes, comments, post media and content events |
| **Feed Service** | `8083` | Redis-backed personalized feeds using fan-out on write |
| **Search Service** | `8084` | Elasticsearch indexing and search for users and posts |
| **Notification Service** | `8085` | Stateless Kafka-driven notification handling |

---

## Event-Driven Communication

Kafka decouples services that produce business events from services that react to them.

| Kafka topic | Producer | Consumers | Purpose |
|---|---|---|---|
| `user.created` | User Service | Search Service, Notification Service | Index a new profile and generate a welcome notification |
| `user.updated` | User Service | Search Service | Keep the user search index synchronized |
| `connection.requested` | User Service | Notification Service | Notify the receiving user |
| `connection.accepted` | User Service | Notification Service | Notify the requester |
| `post.created` | Post Service | Feed Service, Search Service | Fan out the post to feeds and index it for search |
| `post.liked` | Post Service | Notification Service | Notify the post author |
| `post.commented` | Post Service | Notification Service | Notify the post author |

Public authentication endpoints can be accessed without a token, while protected user, post, feed and search operations are routed through the gateway.

---

## User Service

The User Service is the foundation of the system.

### Responsibilities

- User registration and login
- BCrypt password hashing
- JWT access and refresh token generation
- User profile management
- Profile photo upload
- Connection requests and connection acceptance
- Retrieving user connections
- Publishing user and connection events to Kafka
- Persisting users and connections in MySQL
- Uploading profile media to AWS S3

### User data

User profiles contain information such as:

- email
- first name / last name
- headline
- about
- location
- profile photo URL
- cover photo URL
- role
- skills
- created / updated timestamps

The project models user roles such as:

```text
ADMIN
NORMAL_USER
RECRUITER
```

Connection state is represented using statuses such as:

```text
PENDING
CONNECTED
REJECTED
```

---

## Post Service

The Post Service acts as the content engine of the platform.

### Responsibilities

- Create posts
- Retrieve individual posts
- Retrieve posts created by a user
- Like / unlike posts
- Add comments
- Retrieve comments
- Delete posts
- Upload post media to S3
- Publish post-related Kafka events

### Main entities

- `Post`
- `Like`
- `Comment`

A uniqueness constraint on post/user likes prevents duplicate likes for the same combination.

Media is stored in **AWS S3**, while the relational database stores the resulting media URL instead of the binary file itself.

---

## Feed Service

The Feed Service maintains personalized feeds in **Redis**.

It uses a **fan-out-on-write** strategy.

When a user creates a post:

1. Post Service publishes `post.created`.
2. Feed Service consumes the event.
3. Feed Service retrieves the author's connections from User Service through **OpenFeign**.
4. The post ID is pushed into each connection's Redis feed.
5. The post is also pushed into the author's own feed.
6. Redis lists are trimmed to the configured maximum feed size.

This intentionally trades additional work at write time for very fast feed reads.

### Pagination

Feed pagination is performed by calculating Redis list ranges:

```text
start = page * size
end   = start + size - 1
```

For example:

```text
page = 0, size = 10  -> items 0..9
page = 1, size = 10  -> items 10..19
```

---

## Search Service

The Search Service is the discovery engine of the system.

It does not use the relational database for search. Instead, it maintains **Elasticsearch indexes** that are updated from Kafka events.

### Indexed documents

**UserDocument**

- id
- first name
- last name
- headline
- location
- skills
- email
- profile photo URL

**PostDocument**

- id
- content
- author ID
- image URL
- created timestamp

### Search capabilities

- Search people across profile fields
- Search users by skill
- Full-text post search
- Automatic indexing when users are created or updated
- Automatic post indexing after `post.created`

This keeps search responsibilities isolated from transactional services.

---

## Notification Service

The Notification Service is intentionally **stateless**.

It has:

- no notification database
- no notification entity
- no notification repository
- no notification history

Each Kafka event contains the information required to create the relevant notification. The service consumes the event, builds the notification message, logs/processes it, and finishes.

Examples include:

```text
Welcome to LinkedIn!
New Connection Request
Connection Accepted
Someone liked your post
Someone commented on your post
```

This demonstrates how stateless event consumers can scale independently when they do not need retained application state between operations.

---

## API Gateway

The API Gateway is the central entry point into the backend.

### Responsibilities

- Route API requests to the appropriate service
- Validate JWTs for protected routes
- Extract authenticated user information
- Add identity headers for downstream services

Example propagated headers:

```http
X-User-Id: <authenticated-user-id>
X-User-Email: <authenticated-user-email>
```

Authentication endpoints remain publicly accessible, while protected routes pass through JWT validation.

---

## Representative REST APIs

### Authentication

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Users & Connections

```http
GET    /api/v1/users/{userId}
PUT    /api/v1/users/{userId}/profile
POST   /api/v1/users/{targetUserId}/connect
PUT    /api/v1/users/connection/{connectionId}/accept
GET    /api/v1/users/{userId}/connections
POST   /api/v1/users/{userId}/profile-photo
```

### Posts

```http
POST   /api/v1/posts
GET    /api/v1/posts/{postId}
GET    /api/v1/posts/user/{userId}
POST   /api/v1/posts/{postId}/like
POST   /api/v1/posts/{postId}/comments
GET    /api/v1/posts/{postId}/comments
DELETE /api/v1/posts/{postId}
```

### Feed

```http
GET    /api/v1/feed/{userId}?page=0&size=10
DELETE /api/v1/feed/{userId}/cache
```

### Search

```http
GET /api/v1/search/people?q=<query>
GET /api/v1/search/skills?skill=<skill>
GET /api/v1/search/posts?q=<query>
```

> Endpoint signatures may evolve as the project develops. The service code is the source of truth for the current API contract.

---

## Tech Stack

### Backend

- **Java 17**
- **Spring Boot**
- Spring Web / Spring MVC
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Spring Data JPA
- Hibernate
- Spring Data Redis
- Spring Data Elasticsearch
- Spring for Apache Kafka
- Spring Validation
- Spring Boot Actuator
- Spring Security Crypto / BCrypt
- JJWT
- Lombok
- Maven

### Data & Infrastructure

- **MySQL** — users, connections, posts, likes and comments
- **Redis** — personalized feed cache/storage
- **Elasticsearch** — user and post search indexes
- **Apache Kafka** — asynchronous event streaming
- **AWS S3** — media/object storage
- **Docker Compose** — local infrastructure orchestration

---

## Project Structure

```text
linkedin-system/
├── api-gateway/
├── user-service/
├── post-service/
├── feed-service/
├── search-service/
├── notification-service/
├── docker-compose.yaml
└── README.md
```

Each service is an independent Maven/Spring Boot application with its own:

```text
pom.xml
src/main/java/
src/main/resources/application.yaml
```

---

## Local Development

### Prerequisites

Install:

- Java 17
- Maven or use the included Maven Wrapper
- Docker Desktop
- MySQL
- Git
- An AWS account/S3 bucket if you want to test media uploads

Verify:

```bash
java -version
docker --version
git --version
```

---

## Start Infrastructure

The project uses Docker Compose for Kafka, Redis and Elasticsearch.

From the repository root:

```bash
docker compose up -d redis elasticsearch kafka
```

Check the containers:

```bash
docker compose ps
```

Expected local infrastructure:

| Component | Default port |
|---|---:|
| Redis | `6379` |
| Kafka | `9092` |
| Elasticsearch | `9200` |
| MySQL | `3306` |

MySQL can run locally outside Docker.

---

## Environment Variables

Real credentials are intentionally **not committed** to the repository.

Configure the required values locally before starting the relevant services:

```bash
export MYSQL_USERNAME=root
export MYSQL_PASSWORD='your-local-mysql-password'

export JWT_SECRET_KEY='your-base64-jwt-secret'

export AWS_ACCESS_KEY_ID='your-aws-access-key'
export AWS_SECRET_ACCESS_KEY='your-aws-secret-key'
export AWS_REGION='ap-south-1'
export AWS_S3_BUCKET='your-s3-bucket'
```

The **User Service and API Gateway must use the same JWT signing secret** so that tokens created by User Service can be validated by the gateway.

Never commit real database passwords, JWT secrets or AWS credentials.

---

## Running the Services

Start each Spring Boot service from its directory.

Example:

```bash
cd user-service
./mvnw spring-boot:run
```

Repeat for:

```text
user-service          -> 8081
post-service          -> 8082
feed-service          -> 8083
search-service        -> 8084
notification-service  -> 8085
api-gateway           -> 8080
```

A practical local startup order is:

```text
1. MySQL + Docker infrastructure
2. User Service
3. Post Service
4. Search Service
5. Feed Service
6. Notification Service
7. API Gateway
```

---

## Data Ownership

One of the main design goals is to avoid forcing every service onto the same persistence technology.

```text
User Service
    └── MySQL
        ├── users
        └── connections

Post Service
    └── MySQL
        ├── posts
        ├── likes
        └── comments

Feed Service
    └── Redis

Search Service
    └── Elasticsearch
        ├── users index
        └── posts index

Notification Service
    └── No persistent datastore

Media
    └── AWS S3
```

---

## Key Engineering Concepts Demonstrated

- Microservices decomposition
- Event-driven architecture
- Apache Kafka producer/consumer workflows
- Consumer groups and offsets
- Synchronous vs asynchronous service communication
- API Gateway pattern
- JWT authentication
- BCrypt password hashing
- REST API design and versioning
- Database-per-service/responsibility
- JPA/Hibernate persistence
- Redis caching and feed generation
- Fan-out on write
- Elasticsearch full-text search
- Event-driven search-index synchronization
- Stateless service design
- AWS S3 media storage
- Docker-based local infrastructure
- Pagination
- DTOs and entity mapping
- Spring dependency injection
- Repository pattern
- Configuration through environment variables

---

## Why This Project?

The goal of this project is not only to implement LinkedIn-like functionality, but to understand **why different backend components exist and how they collaborate in a distributed system**.

Examples:

- Why use Kafka instead of making every service call every other service synchronously?
- Why keep feed data in Redis instead of repeatedly calculating the feed from a relational database?
- Why maintain a dedicated Elasticsearch index for search?
- Why centralize authentication at an API gateway?
- Why store media in S3 rather than in MySQL?
- What makes an event consumer stateless?
- What are the trade-offs of fan-out on write?

The project is therefore both a functional backend and a hands-on system-design exercise.

---

## Future Improvements

Potential extensions:

- Frontend client
- Persisted notification history
- Refresh-token endpoint and token revocation strategy
- Centralized exception handling across services
- Service discovery
- Distributed tracing
- Centralized logging
- Metrics dashboards
- CI/CD pipeline
- Kubernetes deployment
- Dead-letter topics for failed Kafka events
- Schema-based event contracts
- Integration and end-to-end tests
- API documentation with OpenAPI/Swagger
- Gateway rate limiting
- Cloud deployment

---

## Security

This repository does not intentionally contain real credentials.

For local development:

- provide secrets using environment variables or ignored local configuration
- rotate any credential that is accidentally exposed
- never commit AWS credentials
- keep the JWT signing secret private
- use separate credentials for development and production

---

## Author

**Parth Yadav**

Built as a hands-on project for learning and demonstrating backend engineering, microservices, event-driven architecture and system-design concepts with Java and Spring Boot.

---

## License

This project is currently intended for educational and portfolio use.
