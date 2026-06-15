# Skillshare — Developer Manual

Software Engineering project — Corso di Laurea in Informatica per il Management
University of Bologna, A.Y. 2025/2026

This manual is intended for external collaborators who want to set up, run, and contribute to the project. It explains how to obtain and launch the software on a new machine, gives an overview of the architecture and of how each required feature was implemented (including the design patterns adopted and their benefits), and lists the demo credentials available in the database.

---

## 1. Technology stack

| Technology | Role in the project |
|------------|---------------------|
| **GWT 2.13** | Framework used to build the web application entirely in Java (client compiled to JavaScript). |
| **Maven** | Build, dependency management, packaging and test execution. |
| **MapDB 3.0.10** | Embedded Java database providing persistent `Map` collections on disk. |
| **JUnit 5 + Mockito** | Unit testing of business logic and services. |
| **Jetty 12 (EE10)** | Servlet container used to run the backend. |
| **Docker** | Reproducible build and deployment of the whole application as a container. |
| **GitHub** | Version control, branches, pull requests, issues and project board. |

The project targets **Java 17** and uses the **Jakarta** servlet API (not the legacy `javax`).

---

## 2. Project structure

The application is a multi-module Maven project. All source code lives under the `app/` directory, which contains three modules sharing the `it.unibo` package:

```
app/
├── skillshare-shared/   (jar)      Data objects and RPC interfaces shared by client and server
├── skillshare-server/   (war)      Backend: service implementations, repositories, managers, DatabaseCore
├── skillshare-client/   (gwt-app)  Frontend: GWT entry point and GUI classes
├── pom.xml                          Parent POM (modules, dependency management)
├── Dockerfile
└── docker-compose.yaml
```

- **`skillshare-shared`** contains only interfaces and serializable data structures. It must not contain logic that belongs to the client or the server.
- **`skillshare-server`** contains the backend classes. The `webapp/WEB-INF/web.xml` file declares all servlet endpoints.
- **`skillshare-client`** contains the frontend. The entry point is `it.unibo.App`, which is the class loaded when opening the application in the browser.

---

## 3. Getting the source code and running it

### 3.1 Prerequisites

- **Java JDK 17 or higher**
- **Maven 3.8 or higher**
- (Optional, for containerized deployment) **Docker** and **Docker Compose**

Verify the installation:

```sh
java -version
mvn -version
```

### 3.2 Clone the repository

```sh
git clone https://github.com/yerayyay-27/skillshare-software-engineering.git
cd skillshare-software-engineering/app
```

> **All Maven commands must be run from the `app/` directory.**

### 3.3 First build (install dependencies)

```sh
mvn clean install
```

### 3.4 Run in development mode

The application runs as **two processes in two separate terminals**:

**Terminal 1 — GWT Code Server** (incremental frontend compilation):

```sh
mvn gwt:codeserver -pl *-client -am
```

**Terminal 2 — Jetty server** (backend):

```sh
mvn jetty:run -pl *-server -am -Denv=dev
```

Once both processes are ready, open:

```
http://localhost:8080/
```

### 3.5 Run with Docker

From the `app/` directory:

```sh
docker compose up -d --build
```

The first build takes several minutes (it downloads dependencies and compiles all modules). When it finishes, the application is reachable at `http://localhost:8080/`.

To stop and remove the container:

```sh
docker compose down
```

### 3.6 Running the tests

```sh
mvn test
```

To run the tests of a single module or a single class:

```sh
mvn test -pl *-server
mvn test -pl *-server -Dtest=AuthServiceImplTest
```

---

## 4. Architecture overview

### 4.1 Client–server communication (GWT RPC)

The client never accesses the database directly. It communicates with the backend through **GWT RPC**, an asynchronous remote-procedure-call mechanism. Each backend service is defined by three coordinated pieces:

1. A **service interface** in `skillshare-shared`, annotated with `@RemoteServiceRelativePath`, that declares the synchronous method signatures.
2. An **async interface** in `skillshare-shared`, where every method returns `void` and receives an extra `AsyncCallback` parameter. This is the interface the client actually calls.
3. A **service implementation** in `skillshare-server`, which extends `com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet` and implements the service interface.

Each implementation is registered as a servlet in `web.xml`, mapping a servlet name to its class and to a URL under `/app/`.

> **Important convention:** every server-side `*Impl` class extends the **Jakarta** variant `com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet`. Using the legacy `javax`-based `RemoteServiceServlet` compiles but fails to deploy on Jetty 12/Jakarta with an HTTP 503 error.

### 4.2 Backend layering

The backend separates responsibilities into three layers, which keeps the code testable and avoids duplication:

```
Servlet (RPC entry point)  ->  Manager (business rules)  ->  Repository (persistence)
        *ServiceImpl                *Manager                    *Repository
```

- **Servlet (`*ServiceImpl`)** — thin RPC entry point. It only receives the call and delegates to the manager. It contains no business logic.
- **Manager (`*Manager`)** — holds validation and business rules (e.g. "you cannot request your own announcement", "rating must be 1–5", "only an accepted exchange can be reviewed"). It does not touch MapDB directly; it works through repositories.
- **Repository (`*Repository`)** — isolates all MapDB access. It opens the collections and performs reads/writes, exposing plain Java methods to the rest of the code.

### 4.3 Frontend (GUI navigation)

The frontend is a single-page application. Each screen is a class with a `show()` method that clears the page (`RootPanel.get().clear()`) and rebuilds its interface. Navigation between screens is performed by creating the next screen and calling its `show()` method, for example `new HomeGui(currentUser).show()`. The entry point `App.onModuleLoad()` loads the first screen (`LoginGui`).

The authenticated `User` object is passed from screen to screen through the constructor, so every screen knows who is currently signed in.

### 4.4 Persistence (MapDB)

Data is stored with **MapDB**, an embedded database that exposes persistent collections with the same API as `java.util.Map`. Access is centralized through the `DatabaseCore` class, which returns the single active database instance and exposes a `commit()` operation. After every write (`put`/`remove`), the code calls `DatabaseCore.commit()` to flush the change to disk.

The database file is `progetto_sweng.db`. In a containerized deployment its location is taken from the `DATA_DIR` environment variable and mapped to a mounted volume so that data survives container restarts.

---

## 5. How the required features were implemented

### 5.1 Identity & Profile Management

- **Registration and login** are handled by `AuthService` (interface), `AuthServiceImpl` (servlet) and the `UserRepository`. Passwords are stored only on the server and are never included in the `User` object sent to the client. Login returns the full `User`; registration validates the input (non-empty fields, minimum password length, unique email).
- **User profile** is handled by `ProfileService` / `ProfileServiceImpl`. A user can edit a bio and a comma-separated list of skill tags, and upload a profile photo. The photo is read in the browser with the `FileReader` API (helper `ImageReader`), converted to a base64 data URL, sent through the same profile RPC, validated on the server (format and size limit) and stored in MapDB. This reuses the existing RPC infrastructure and avoids a separate multipart upload servlet.
- The relevant client screens are `LoginGui`, `RegisterGui`, `HomeGui` and `ProfileGui`.

### 5.2 Skill Marketplace

- The serializable `Announcement` data object carries the announcement id, owner, offered skill, requested skill, description, availability and active status.
- `AnnouncementService` / `AnnouncementServiceImpl` expose creating, retrieving, listing active announcements, deleting/deactivating, and a case-insensitive search across offered skill, requested skill and description. Business rules live in `AnnouncementManager`; persistence in `AnnouncementRepository`. Duplicate ids are rejected atomically using `putIfAbsent`.
- Client screens: `MarketplaceGui` (listing and search) and `AnnouncementFormGui` (create/edit).

### 5.3 Interaction & Coordination

- **Exchange requests** are modelled by `ExchangeRequest` (status `PENDING` / `ACCEPTED` / `REJECTED`). From a marketplace announcement a user sends a request to the owner. `ExchangeRequestManager` generates the request id automatically (UUID), checks that the target announcement exists and is active, prevents requesting one's own announcement, and blocks duplicate pending requests. The owner accepts or rejects the request from `ExchangeRequestsGui`.
- **Internal chat** is enabled only once a request has been accepted, and is identified by the exchange request id. `ChatManager` enforces that a message can be sent or read only if the request exists, is `ACCEPTED`, and the user is one of the two participants. The client screen is `ChatGui`, with a manual *Refresh* button to reload messages (the project does not use real-time push).

### 5.4 Reputation & Feedback

- A `Review` (1–5 rating + comment) is tied to an accepted exchange. `ReviewManager` validates that the exchange is accepted and the reviewer is a participant, resolves the reviewed user as the other participant, blocks out-of-range ratings and a second review on the same exchange. It also computes a `UserReputation` (average rating, review count and the list of received reviews).
- The method `getReviewBlockReason` returns a human-readable reason when a review is not allowed (or `null` when it is), so the UI can explain the situation instead of failing. Client screens: `ReviewGui` (leave a review) and `ReputationGui` (public rating and reviews of a user).

---

## 6. Design patterns adopted

The project deliberately applies several design patterns. Each one is listed below together with the concrete benefit it brought.

### 6.1 Singleton — `DatabaseCore`

`DatabaseCore` exposes a single, synchronized point of access to the MapDB instance. **Benefit:** only one process opens the database file at a time, which avoids file-lock conflicts and concurrent-access problems, and gives the whole backend one consistent place to obtain the database and to commit changes.

### 6.2 Repository

Each entity has a dedicated repository (`UserRepository`, `AnnouncementRepository`, `ExchangeRequestRepository`, `ChatRepository`, `ReviewRepository`) that isolates all MapDB access behind plain Java methods. **Benefit:** persistence is decoupled from business logic and could be replaced without touching the rest of the code; it also makes the data layer a single source of truth and enables isolated testing against an in-memory database.

### 6.3 Service Layer (Manager)

Business rules live in manager classes (`AnnouncementManager`, `ExchangeRequestManager`, `ChatManager`, `ReviewManager`), separate from both the RPC servlet and the persistence layer. **Benefit:** validation and rules can be unit-tested directly, without needing a running servlet container or mocking the servlet context, and the logic is not duplicated across the servlet and the repository.

### 6.4 Dependency Injection (constructor injection)

Managers and service implementations receive their dependencies through the constructor: a public no-argument constructor for production use, and a package-private constructor that accepts the dependencies for tests. **Benefit:** tests can inject an in-memory repository (via `DatabaseCore.enableTestMode()`) and verify behaviour in isolation, fully decoupled from the file-based database.

### 6.5 Data Transfer Object (DTO)

The serializable classes in `skillshare-shared` (`User`, `Announcement`, `ExchangeRequest`, `ChatMessage`, `Review`, `UserReputation`) carry data between client and server. **Benefit:** a clear, typed contract for the data crossing the network, shared by both ends, with no business logic attached.

### 6.6 Asynchronous Proxy (GWT RPC)

For each service the GWT framework generates a client-side asynchronous proxy from the service interface, used through `AsyncCallback`. **Benefit:** the client calls the backend as if calling local methods, while the network round-trip is handled asynchronously without blocking the UI.

---

## 7. Testing strategy

Business logic is covered by JUnit 5 unit tests located in `skillshare-server/src/test`. Tests run against an in-memory MapDB instance by calling `DatabaseCore.enableTestMode()` in `@BeforeEach` and `DatabaseCore.disableTestMode()` in `@AfterEach`, so they never touch the real `progetto_sweng.db` file. Managers are tested directly (creation, validation errors, rejection rules, search, status transitions, reputation computation, etc.). Run the full suite with `mvn test`.

---

## 8. Demo credentials

A demo account is seeded automatically on first run (in the `UserRepository` constructor), so it exists on any machine without manual setup:

|     Email        | Password | Username   |
|------------------|----------|------------|
| `test@unibo.it`  |  `1234`  | `TestUser` |
| `yeray@unibo.it` |  `2222`  | `Yeray`    |
New accounts can also be created at any time through the registration screen.

---

## 9. Resetting the database

The database file is not committed to the repository. To start from an empty database, stop the application (clean shutdown) and remove the database file `progetto_sweng.db` (or, for the containerized deployment, the mounted data directory). On the next start, the demo account is seeded again.

---

## 10. Contributing

- Follow the existing layering: add a new feature as `shared` interfaces + DTOs, a `*Repository`, a `*Manager`, a `*ServiceImpl` registered in `web.xml`, and the matching client GUI.
- Server-side `*Impl` classes must extend the Jakarta `RemoteServiceServlet`.
- All identifiers, comments and user-facing messages are written in English.
- Use feature branches and pull requests; do not push significant changes directly to `main`.
