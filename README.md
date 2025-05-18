# Banking Transfer Service

A simple Spring Boot application that provides REST APIs to manage bank accounts and perform money transfers between accounts with proper validation, concurrency control, and notifications.

## Features

- Create and retrieve bank accounts with unique IDs and balances.
- Transfer funds between accounts with:
  - Validation for positive amounts and distinct accounts.
  - Sufficient balance checks.
  - Thread-safe transfer operations to avoid race conditions and deadlocks.
- Notification service integration to inform account holders about transfers.
- Comprehensive unit tests covering positive and negative scenarios.

## Technologies Used

- Java 17+
- Spring Boot
- Spring MVC (REST)
- Lombok
- JUnit 5 & Mockito for testing
- Gradle build tool

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle (or use the Gradle wrapper included)
- Git

