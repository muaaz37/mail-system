# Review of the Original Application

This review documents the outdated state of the application before our team extended, restructured and further developed it. It serves as an assessment of the initial project that formed the basis for our subsequent work; it does not describe the current implementation.

## 1. Does the application work correctly from a user perspective?

The original application was only partially functional.

### Working features

- User registration, login and logout
- Internal messages between registered users
- Inbox, drafts and sent-message views
- Deletion of messages
- Upload of small image attachments
- Error page for unknown routes

### Missing or limited features

- Messages could only be delivered internally between registered application users.
- Sending and receiving external emails was not implemented.
- Attachments were stored on the local filesystem and limited to small image files.
- H2 was used as the default database, so application data was not retained reliably after a restart.

The application therefore provided the basic functionality of an internal messaging system, but it did not yet operate as a complete email support system.

## 2. How was the application structured?

The project consisted of two main modules:

- `frontend`: Angular application containing pages, components, services, route guards and TypeScript models
- `backend`: Kotlin and Spring Boot application containing controllers, services, repositories, persistence models and security configuration

The repository additionally contained:

- `docker-compose.yml` for starting a PostgreSQL database
- `uploads/` for storing attachments on the local filesystem

This structure separated the browser-based user interface from the backend API and persistence logic.

## 3. What technology stack was used?

### Frontend

| Technology | Role in the original application |
| --- | --- |
| **Angular 21.1** | Provided the single-page application, browser routing and component structure |
| **TypeScript 5.9** | Added static typing to the frontend source code |
| **PrimeNG** and **Tailwind CSS** | Supplied reusable UI components and utility-based styling |
| **RxJS** | Managed asynchronous data flows and communication with the backend |
| **npm** | Managed frontend dependencies and build scripts |

### Backend

| Technology | Role in the original application |
| --- | --- |
| **Kotlin 2.2.21** | Implemented the backend application and business logic |
| **Java 21** | Provided the JVM runtime for the backend |
| **Spring Boot 4.0.1** | Supplied the application framework and runtime configuration |
| **Spring Web** | Exposed the REST API |
| **Spring Security** and **JWT** | Implemented authentication and protected API requests |
| **Spring Data JPA** | Connected the application model to the database |
| **Gradle 9.2.1** | Managed dependencies, compilation and automated tests |

### Data storage and infrastructure

| Technology | Role in the original application |
| --- | --- |
| **H2** | Served as the default local database; data was not reliably retained after a restart |
| **PostgreSQL** | Was available as an optional persistent database through Docker Compose |
| **Local filesystem** | Stored uploaded attachment files in the `uploads/` directory |
| **Docker Compose** | Started the optional PostgreSQL database for local development |

The selected stack provided a suitable foundation for a basic full-stack application. However, authentication and attachment storage remained application-local, while external email communication had not yet been implemented.

## 4. What architecture was used?

The application followed a client-server architecture:

```text
Browser
  → Angular frontend
  → Spring Boot REST API
      → H2 or PostgreSQL database
      → Local attachment storage
```

The Angular frontend provided the browser-based user interface. It communicated with the Spring Boot backend through a REST API and used JWT tokens for authenticated requests. The backend implemented the application logic and stored data in either H2 or PostgreSQL. Uploaded attachments were stored separately on the local filesystem.

The backend also followed a layered architecture:

1. **Controller layer** — handled HTTP requests and responses.
2. **Service layer** — implemented the application and business logic.
3. **Repository layer** — provided access to persistent data.

This architecture offered a clear separation of responsibilities, but the original implementation still depended on local storage and lacked integration with external mail and identity services.
