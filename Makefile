.PHONY: help env build test coverage run clean docker-up docker-down docker-logs up verify all

# Detect Maven wrapper per OS
# Check if running in WSL or native Windows
ifeq ($(OS),Windows_NT)
	# Native Windows - use full path format
	MVN := .\mvnw.cmd
else
	# Unix-like (including WSL)
	MVN := ./mvnw.cmd
endif

# Defaults (overridden by .env, CLI, or environment)
DB_USER ?= admin
DB_PASSWORD ?= admin
DB_NAME ?= fx-deals-warehouse
DB_PORT ?= 15432

# Load .env if present and export variables to child processes
ifneq (,$(wildcard .env))
include .env
endif

# Explicitly export the variables so they reach Maven, Docker, etc.
export DB_USER DB_PASSWORD DB_NAME DB_PORT

# Default target
help:
	@echo "FX Deals Warehouse - Make targets"
	@echo "----------------------------------------------------"
	@echo "make help         - Show this help"
	@echo "make env          - Print loaded environment variables"
	@echo "make build        - Build the application (package, skip tests)"
	@echo "make test         - Run tests"
	@echo "make coverage     - Run tests with coverage report"
	@echo "make verify       - Run tests with coverage and verify quality gates"
	@echo "make run          - Run the app via Maven (spring-boot:run)"
	@echo "make clean        - Clean build artifacts"
	@echo "make up           - Start all services (docker-up)"
	@echo "make docker-up    - Start services defined in docker-compose.yml"
	@echo "make docker-down  - Stop and remove services/volumes"
	@echo "make docker-logs  - Tail container logs"
	@echo "make all          - Clean, build, test, coverage (full CI pipeline)"

# Show key env vars (from .env or environment)
env:
	@echo "DB_USER=$(DB_USER)"
	@echo "DB_PASSWORD=$(DB_PASSWORD)"
	@echo "DB_NAME=$(DB_NAME)"
	@echo "DB_PORT=$(DB_PORT)"

# Build and test
ifeq ($(OS),Windows_NT)
build:
	@.\mvnw.cmd -q -DskipTests package

test:
	@.\mvnw.cmd -q test

coverage:
	@.\mvnw.cmd -q test jacoco:report
	@echo "Coverage report generated at: target/site/jacoco/index.html"

verify:
	@.\mvnw.cmd -q verify
	@echo "All tests passed with coverage verification"

run:
	@.\mvnw.cmd -q spring-boot:run

clean:
	@.\mvnw.cmd -q clean
else
build:
	@./mvnw.cmd -q -DskipTests package

test:
	@./mvnw.cmd -q test

coverage:
	@./mvnw.cmd -q test jacoco:report
	@echo "Coverage report generated at: target/site/jacoco/index.html"

verify:
	@./mvnw.cmd -q verify
	@echo "All tests passed with coverage verification"

run:
	@./mvnw.cmd -q spring-boot:run

clean:
	@./mvnw.cmd -q clean
endif

# Convenience target - same as docker-up
up: docker-up

# Full CI pipeline - clean checkout to verified build
all: clean verify
	@echo "Build and test complete with coverage verification"

# Docker Compose helpers
DOCKER_COMPOSE ?= docker compose

docker-up:
	$(DOCKER_COMPOSE) up -d

docker-down:
	$(DOCKER_COMPOSE) down -v

docker-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200
