# User Management System

A microservices-based user management system built with Micronaut framework.

## Features

- User Management (CRUD operations)
- Address Management (CRUD operations)
- Password Change Request and Approval System
- Role-based Access Control (Admin and User roles)
- OpenAPI Documentation with Swagger UI

## Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Gradle 7.0 or higher

## Database Setup

1. Create a PostgreSQL database named `usermanagement`
2. Update the database credentials in `src/main/resources/application.yml` if needed

## Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application:
   ```bash
   ./gradlew run
   ```
4. Access the Swagger UI at: http://localhost:8080/swagger-ui/

## API Endpoints

### User Management

- `POST /api/users` - Create a new user
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users` - Get all users
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `POST /api/users/{id}/change-password` - Request password change
- `PUT /api/users/{id}/approve-password-change` - Approve password change

### Address Management

- `POST /api/addresses` - Create a new address
- `GET /api/addresses/{id}` - Get address by ID
- `GET /api/addresses/user/{userId}` - Get address by user ID
- `PUT /api/addresses/{id}` - Update address
- `DELETE /api/addresses/{id}` - Delete address
- `DELETE /api/addresses/user/{userId}` - Delete address by user ID

## Project Structure

```
src/main/java/com/example/
├── controller/
│   ├── UserController.java
│   └── AddressController.java
├── model/
│   ├── User.java
│   ├── Address.java
│   ├── Gender.java
│   ├── UserRole.java
│   └── AddressType.java
├── repository/
│   ├── UserRepository.java
│   └── AddressRepository.java
├── service/
│   ├── UserService.java
│   ├── AddressService.java
│   └── impl/
│       ├── UserServiceImpl.java
│       └── AddressServiceImpl.java
└── exception/
    ├── ResourceNotFoundException.java
    └── GlobalExceptionHandler.java
```

## Technologies Used

- Micronaut Framework
- PostgreSQL
- JPA/Hibernate
- OpenAPI/Swagger
- Gradle

## Micronaut 4.8.2 Documentation

- [User Guide](https://docs.micronaut.io/4.8.2/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.8.2/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.8.2/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)
---

- [Micronaut Gradle Plugin documentation](https://micronaut-projects.github.io/micronaut-gradle-plugin/latest/)
- [GraalVM Gradle Plugin documentation](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)
- [Shadow Gradle Plugin](https://gradleup.com/shadow/)
## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)


## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


