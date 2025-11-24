# Tarology Web Service

## Development

### Prerequisites
- Java 21
- PostgreSQL 15
- Docker & Docker Compose (optional)

### Build & Test
```bash
# Build the project
./gradlew build

# Run all tests
./gradlew test

# Run unit tests only
./gradlew test --tests "*Test" --exclude-task "*E2E*"

# Run E2E tests only
./gradlew test --tests "com.github.butvinmitmo.highload.e2e.*"

# Run tests with coverage
./gradlew test jacocoTestReport

# View coverage report
# HTML report: build/reports/jacoco/test/html/index.html
# XML report: build/reports/jacoco/test/jacocoTestReport.xml

# View test results
# HTML report: build/reports/tests/test/index.html

# Clean build artifacts
./gradlew clean

# Run with hot reload (development mode)
./gradlew bootRun
```

### Code Quality
```bash
# Run ktlint checks
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

### Running with Docker
```bash
# Start all services (database + application)
docker compose up -d

# View logs
docker compose logs -f app
```

### Running locally
```bash
# Start only the database
docker compose up -d postgres

# Run the application
./gradlew bootRun
```
