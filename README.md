# Hotel Booking Microservices System

A microservices-based hotel booking system built with Spring Boot 3.5, Spring Cloud, and JWT authentication.

## Architecture

```
                    ┌─────────────────┐
                    │  Eureka Server  │
                    │     :8761       │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  API Gateway  │   │ Hotel Service │   │Booking Service│
│    :8080      │──▶│    :8081      │◀──│    :8082      │
└───────────────┘   └───────────────┘   └───────────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Eureka Server | 8761 | Service Discovery |
| API Gateway | 8080 | Request routing, JWT proxy |
| Hotel Service | 8081 | Hotel and room management |
| Booking Service | 8082 | Booking management, authentication |

## Technology Stack

- Java 17+
- Spring Boot 3.5.0
- Spring Cloud 2025.0.0
- Spring Data JPA + H2 (in-memory)
- Spring Security + JWT
- Spring Cloud Gateway
- Spring Cloud Eureka
- OpenFeign + Resilience4j
- Lombok + MapStruct
- Swagger/OpenAPI

## Prerequisites

- Java 17 or higher
- Maven 3.8+

## Building the Project

```bash
# Build all modules
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

## Running the Services

Start services in the following order:

```bash
# 1. Start Eureka Server
cd eureka-server
mvn spring-boot:run

# 2. Start Hotel Service
cd hotel-service
mvn spring-boot:run

# 3. Start Booking Service
cd booking-service
mvn spring-boot:run

# 4. Start API Gateway
cd api-gateway
mvn spring-boot:run
```

Or run each JAR directly:

```bash
java -jar eureka-server/target/eureka-server-1.0.0.jar
java -jar hotel-service/target/hotel-service-1.0.0.jar
java -jar booking-service/target/booking-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

## API Documentation

Once services are running:

- Hotel Service Swagger: http://localhost:8081/swagger-ui.html
- Booking Service Swagger: http://localhost:8082/swagger-ui.html
- Eureka Dashboard: http://localhost:8761

## Default Users

| Username | Password | Role |
|----------|----------|------|
| admin | password123 | ADMIN |
| john_doe | password123 | USER |
| jane_smith | password123 | USER |
| bob_wilson | password123 | USER |

## API Endpoints

### Authentication (via Gateway :8080)

```bash
# Register new user
POST /api/users/register
{
    "username": "newuser",
    "password": "password123"
}

# Login
POST /api/users/auth
{
    "username": "john_doe",
    "password": "password123"
}
```

### Hotels (via Gateway :8080)

```bash
# Get all hotels
GET /api/hotels

# Get hotel by ID
GET /api/hotels/{id}

# Create hotel (ADMIN)
POST /api/hotels
Authorization: Bearer <token>
{
    "name": "New Hotel",
    "address": "123 Main St"
}
```

### Rooms (via Gateway :8080)

```bash
# Get all available rooms
GET /api/rooms

# Get recommended rooms (sorted by times_booked)
GET /api/rooms/recommend?startDate=2025-01-15&endDate=2025-01-20

# Create room (ADMIN)
POST /api/rooms
Authorization: Bearer <token>
{
    "hotelId": 1,
    "number": "301"
}
```

### Bookings (via Gateway :8080)

```bash
# Create booking (manual room selection)
POST /api/bookings/booking
Authorization: Bearer <token>
{
    "roomId": 1,
    "hotelId": 1,
    "startDate": "2025-01-15",
    "endDate": "2025-01-20",
    "autoSelect": false
}

# Create booking (auto room selection)
POST /api/bookings/booking
Authorization: Bearer <token>
{
    "hotelId": 1,
    "startDate": "2025-01-15",
    "endDate": "2025-01-20",
    "autoSelect": true
}

# Get user's bookings
GET /api/bookings/bookings
Authorization: Bearer <token>

# Cancel booking
DELETE /api/bookings/booking/{id}
Authorization: Bearer <token>
```

## Key Features

### Saga Pattern for Distributed Transactions

Booking creation follows a saga pattern:
1. Create booking in PENDING status
2. Confirm room availability with Hotel Service
3. On success: update to CONFIRMED, increment times_booked
4. On failure: CANCELLED status, release room lock (compensation)

### Idempotency

All booking operations use `requestId` for idempotency:
- Duplicate requests return existing result
- No duplicate bookings created

### Resilience

- Circuit breaker on Hotel Service calls
- Retry with exponential backoff
- Graceful degradation with fallbacks

### Room Recommendation Algorithm

Rooms are recommended based on `times_booked` counter:
- Sorted by times_booked ASC, then by ID
- Ensures uniform load distribution

## Testing

```bash
# Run all tests
mvn test

# Run tests for specific module
cd hotel-service
mvn test
```

## Database Access

H2 Console is available for each service:
- Hotel Service: http://localhost:8081/h2-console (JDBC URL: jdbc:h2:mem:hoteldb)
- Booking Service: http://localhost:8082/h2-console (JDBC URL: jdbc:h2:mem:bookingdb)

## Project Structure

```
spring-rest-task/
├── pom.xml                 # Parent POM
├── eureka-server/          # Service Discovery
├── api-gateway/            # API Gateway
├── hotel-service/          # Hotel Management
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── mapper/
│   ├── security/
│   └── exception/
├── booking-service/        # Booking Management
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   ├── mapper/
│   ├── security/
│   ├── client/             # Feign clients
│   └── exception/
└── IMPLEMENTATION_GUIDE.md # Detailed implementation guide
```

## Architectural Decisions

### JWT Validation per Service
Each service validates JWT independently rather than centralizing at Gateway. This provides:
- Service independence
- Better security (defense in depth)
- Simpler Gateway configuration

### H2 In-Memory Database
Used for simplicity. For production:
- Replace with PostgreSQL/MySQL
- Update connection strings in application.yml
- Remove H2 console settings

### Compensation over Two-Phase Commit
Using saga pattern with compensation instead of distributed transactions:
- Better performance
- Eventual consistency is acceptable for booking use case
- Simpler implementation
