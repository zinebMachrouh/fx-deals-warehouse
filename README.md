# FX Deals Data Warehouse

A high-performance data warehouse solution for processing and storing Foreign Exchange (FX) deals, built with Spring Boot 3.5.7 and PostgreSQL 18.1.

## 🚀 Features

- **Deal Processing**: Single and batch import of FX deals
- **Data Validation**: Comprehensive validation of deal fields
- **Duplicate Prevention**: Ensures no duplicate deals are processed
- **High Performance**: Optimized for high-throughput processing
- **Comprehensive Testing**: 100% test coverage for core business logic
- **Containerized**: Easy deployment with Docker and Docker Compose
- **RESTful API**: Well-documented endpoints for integration

## 🛠 Tech Stack

- **Backend**: Java 17, Spring Boot 3.5.7
- **Database**: PostgreSQL 18.1
- **Build Tool**: Maven
- **Testing**: JUnit 5, TestContainers, RestAssured, k6
- **Containerization**: Docker, Docker Compose
- **Code Quality**: JaCoCo, Checkstyle, PMD
- **Database Migration**: Liquibase
- **Monitoring**: Spring Boot Actuator

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+

## 🚀 Quick Start

### Running with Docker Compose

```bash
git clone https://github.com/zinebMachrouh/fx-deals-warehouse
cd fx-deals-warehouse
docker-compose up -d
```

The application will be available at: http://localhost:8080

### Running Locally

1. Start PostgreSQL:
   ```bash
   docker-compose up -d db
   ```

2. Build and run the application:
   ```bash
   mvn spring-boot:run
   ```

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -Pintegration-test
```

### Performance Testing with k6
```bash
docker-compose --profile perf up k6
```

## 📚 API Documentation

- **Postman Collection**: `/src/test/resources/postman/FX Deal Warehouse.postman_collection.json`
- **Actuator Endpoints**:
  - Health: http://localhost:8080/actuator/health
  - Metrics: http://localhost:8080/actuator/metrics
  - Info: http://localhost:8080/actuator/info

## 🌐 API Endpoints

### Import Single Deal
```
POST /api/v1/deals/import/single
Content-Type: application/json

{
  "dealId": "DEAL123",
  "fromCurrency": "USD",
  "toCurrency": "EUR",
  "dealTimestamp": "2025-11-16T10:30:00Z",
  "amount": "1000.50"
}
```

### Import Batch Deals
```
POST /api/v1/deals/import/batch
Content-Type: application/json

[
  {
    "dealId": "DEAL123",
    "fromCurrency": "USD",
    "toCurrency": "EUR",
    "dealTimestamp": "2025-11-16T10:30:00Z",
    "amount": "1000.50"
  }
]
```

### Get All Deals
```
GET /api/v1/deals
```

## 📊 Database

- **PostgreSQL 18.1** (Alpine)
- **Port**: 15432 (configurable via DB_PORT)
- **Database**: fx-deals-warehouse
- **Username**: admin (configurable via DB_USER)
- **Password**: admin (configurable via DB_PASSWORD)

### Schema Management
Database schema is managed using Liquibase. Migration scripts are located in `src/main/resources/db/changelog/`.

## 🛡️ Security

- Input validation using Bean Validation (JSR-380)
- SQL injection prevention using JPA/Hibernate
- Environment-based configuration
- Comprehensive error handling with GlobalExceptionHandler

## 🚀 Performance Testing

Performance tests are written in k6 and can be run using:

```bash
docker-compose --profile perf up k6
```

Test scenarios include:
- Smoke tests for single deal import
- Load testing with multiple concurrent users

## 🎯 JIRA Integration

- **Project Key**: FXDW
- **Development Workflow**:
  1. Create feature branches: `feature/FXDW-123-description`
  2. Reference JIRA in commits: `FXDW-123: Add feature`
  3. Create PR with JIRA ticket in description

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📬 Contact

For any questions or support, please contact the development team.

## 🔍 Code Quality Gates

- 100% test coverage for core business logic
- No critical SonarQube issues
- All tests must pass
- Code style compliance (Checkstyle)
- OWASP dependency check passes

## 📊 Monitoring

- **Health Check**: `/actuator/health`
- **Liveness Probe**: `/actuator/health/liveness`
- **Readiness Probe**: `/actuator/health/readiness`
- **Metrics**: `/actuator/prometheus`
- **Info**: `/actuator/info`

## 🔒 Security Considerations

- All sensitive configuration is externalized using environment variables
- Input validation on all API endpoints
- Regular dependency updates for security patches
- SQL injection prevention using JPA/Hibernate
- Comprehensive logging for audit trails

## 🛠️ Development

### Code Style
- Follows Google Java Style Guide with some modifications
- Uses Checkstyle for code style enforcement

### Logging
- Logback for logging configuration
- Structured JSON logging in production
- Different log levels for different environments

### Error Handling
- Global exception handling with appropriate HTTP status codes
- Meaningful error messages for client applications
- Detailed error logging for debugging

### Testing Strategy
- Unit tests for business logic
- Integration tests for API endpoints
- TestContainers for database integration tests
- k6 for performance testing
