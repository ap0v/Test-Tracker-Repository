# Test Tracker backend

A Spring Boot REST API for managing test cases. Users sign in with a session cookie, write test cases, and sort them into nested folders. Every record belongs to an account, and a signed-in user only ever sees their own account's data.

The React frontend in [`../frontend`](../frontend) talks to this API through the Vite dev-server proxy.

## Tech stack

- Java 21, Spring Boot 4.1
- Spring Web MVC, Spring Data JPA, Bean Validation
- Spring Security (form login, server-side sessions, CSRF protection)
- PostgreSQL (hosted on Supabase)
- Lombok
- Maven, through the bundled wrapper (`mvnw` / `mvnw.cmd`)

## Prerequisites

- **JDK 21.** Set `JAVA_HOME` to it; the Maven wrapper uses `JAVA_HOME`.
- **A PostgreSQL database with the schema already in place.** The app runs with `spring.jpa.hibernate.ddl-auto=validate`, so it checks the tables at startup but never creates or changes them. See [Database](#database).

You don't need to install Maven. The wrapper downloads Maven 3.10.0 and verifies its checksum.

## Configuration

Database settings are read from `.env.local` in this directory. The file is gitignored.

1. Copy the example file:

   ```sh
   cp .env.local.example .env.local
   ```

2. Fill in the three values:

   | Key | Value |
   | --- | --- |
   | `SUPABASE_DB_URL` | JDBC URL, e.g. `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` |
   | `SUPABASE_DB_USERNAME` | Database user, e.g. `postgres.<project-ref>` |
   | `SUPABASE_DB_PASSWORD` | Database password |

The file uses Java properties format. Don't quote the values, don't prefix keys with `export`, and don't put `user:password@` inside the JDBC URL.

The path is resolved relative to the working directory (`optional:file:./.env.local`), so **run the app from this `test-tracker` directory**. From anywhere else the file won't be found and startup fails on the unresolved `${SUPABASE_DB_URL}` placeholder. You can also set the three keys as environment variables instead.

## Running locally

```sh
./mvnw spring-boot:run        # macOS / Linux
.\mvnw.cmd spring-boot:run    # Windows
```

The API starts on <http://localhost:8080>. Spring Boot DevTools is included, so the app restarts automatically when compiled classes change.

To work on the full app, start the frontend in a second terminal (`npm run dev` in `../frontend`). Vite forwards `/api` requests to port 8080.

## Running tests

```sh
./mvnw test
```

`TestTrackerApplicationTests` starts the full application context and needs a working database connection from `.env.local`. The controller and unit tests use `@WebMvcTest` with mocked repositories and need no database. To run only those:

```sh
./mvnw test "-Dtest=!TestTrackerApplicationTests" "-Dsurefire.failIfNoSpecifiedTests=false"
```

## Database

The app expects these PostgreSQL tables:

| Table | Holds |
| --- | --- |
| `accounts` | An account (the owner of all data) |
| `account_logins` | Email + password hash for signing in to an account |
| `test_cases` | Test cases |
| `folders` | Folders; `parent_id` makes them nested |
| `folder_test_cases` | Links between folders and test cases (many-to-many) |

Some behaviour depends on the schema, not on Java code:

- **Folder deletes** rely on `ON DELETE CASCADE` foreign keys. Deleting a folder removes its subfolders and its folder–test case links. The test cases themselves are kept.
- **Duplicate folder names** under the same parent are blocked by a unique index. The API reports them as `409 Conflict`.
- **Duplicate emails** are blocked by a unique index on `account_logins`.
- Composite `(id, account_id)` foreign keys stop a folder or link from pointing at another account's rows.

The schema SQL is not committed to this repository.

## API

All endpoints are under `/api`. Apart from `GET /api/auth/csrf` and `POST /api/auth/register`, every endpoint requires a signed-in session and returns `401` without one.

### Authentication and CSRF

Every `POST`, `PUT`, `PATCH` and `DELETE` request (including login and register) must carry a CSRF token:

1. Call `GET /api/auth/csrf`. It returns `{ "headerName": "...", "parameterName": "...", "token": "..." }`.
2. Send the token in the header named by `headerName`.

Spring Security issues a new token when you sign in, so fetch a fresh one after login.

| Method | Path | Body | Success | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/auth/csrf` | – | `200` | Public |
| `POST` | `/api/auth/register` | JSON `{ accountName, email, password }` | `201` | Public. Password must be 8 characters to 72 UTF-8 bytes. `409` if the email is taken. Doesn't sign you in. |
| `POST` | `/api/auth/login` | Form-encoded `email`, `password` | `204` | `401` on bad credentials. Sets the session cookie. |
| `POST` | `/api/auth/logout` | – | `204` | |
| `GET` | `/api/auth/me` | – | `200` | `{ loginId, accountId, email }` |

Emails are trimmed and lowercased before they're stored and looked up.

### Test cases

| Method | Path | Success | Notes |
| --- | --- | --- | --- |
| `GET` | `/api/testcases` | `200` | Paged. Optional `folderId`, `page`, `size` (default 20), `sort` (default `createdAt,desc` then `id,desc`). |
| `GET` | `/api/testcases/{id}` | `200` | `404` if it doesn't exist or belongs to another account. |
| `POST` | `/api/testcases` | `201` | Returns the created test case with a `Location` header. `404` if any of `folderIds` doesn't exist or belongs to another account; nothing is saved. |

Create request body:

```json
{
  "title": "Login rejects a wrong password",
  "description": "optional",
  "preconditions": "optional",
  "expectedResults": "optional",
  "priority": "HIGH",
  "status": "READY",
  "tags": ["auth", "smoke"],
  "folderIds": ["7f3c2a9e-1b4d-4c8a-9e2f-5a6b7c8d9e0f"]
}
```

Only `title` is required. `priority` is `LOW`, `MEDIUM`, `HIGH` or `CRITICAL` and defaults to `MEDIUM`. `status` is `DRAFT`, `READY` or `ARCHIVED` and defaults to `DRAFT`. Tags can't be blank. `folderIds` files the new test case in each listed folder in the same transaction; a folder listed twice is filed once.

The list response has this shape:

```json
{
  "content": [ /* test cases */ ],
  "page": { "size": 20, "number": 0, "totalElements": 42, "totalPages": 3 }
}
```

### Folders

| Method | Path | Body | Success | Notes |
| --- | --- | --- | --- | --- |
| `GET` | `/api/folders` | – | `200` | All of the account's folders as a flat list, sorted by name. Build the tree from `parentId`. |
| `GET` | `/api/folders/{id}` | – | `200` | |
| `POST` | `/api/folders` | `{ name, parentId? }` | `201` | Omit `parentId` for a top-level folder. |
| `PATCH` | `/api/folders/{id}` | `{ name?, parentId? }` | `200` | See below. |
| `DELETE` | `/api/folders/{id}` | – | `204` | Also deletes subfolders. Test cases are kept. |
| `PUT` | `/api/folders/{folderId}/testcases/{testCaseId}` | – | `204` | Adds a test case to a folder. Safe to repeat. |
| `DELETE` | `/api/folders/{folderId}/testcases/{testCaseId}` | – | `204` | Removes it from the folder. The test case is kept. |

`PATCH` only changes the fields you send:

- `{ "name": "New name" }` renames the folder and leaves it where it is.
- `{ "parentId": "<uuid>" }` moves it under another folder.
- `{ "parentId": null }` moves it to the top level.

Moving a folder into itself or one of its own subfolders returns `400`. A name already used by a sibling returns `409`. A parent folder or test case that doesn't exist in your account returns `404`.

### Errors

- `400` – the request body failed validation (for example a blank `title` or `name`) or a path parameter isn't a valid UUID.
- `401` – not signed in, or wrong login credentials.
- `403` – missing or invalid CSRF token.
- `404` – the resource doesn't exist in your account. Other accounts' records always look like they don't exist.
- `409` – the email is already registered, or a folder name is already taken.

## Project layout

```text
src/main/java/com/example/testtracker/
├── TestTrackerApplication.java   Entry point
├── config/SecurityConfig.java    Security filter chain, login/logout, CSRF, password encoder
├── auth/                         Register, current user, CSRF endpoints; UserDetailsService
├── Account/                      Account entity and repository
├── AccountLogin/                 Login (email + password hash) entity and repository
├── TestCase/                     Test case entity, enums, service, controller
└── Folder/                       Folder and folder–test case link entities, service, controller
src/main/resources/application.properties
src/test/java/...                 MockMvc controller tests and unit tests
tools/password-hash/              Stand-alone BCrypt hash generator (not part of the build)
```

## Password hash tool

`tools/password-hash/GeneratePasswordHash.java` prints a password hash in the same format the app stores (`{bcrypt}$2a$10$...`). Use it when you need to insert or reset a row in `account_logins` by hand. For normal sign-ups, use `POST /api/auth/register`.

1. Copy `tools/password-hash/.env.example` to `tools/password-hash/.env` (gitignored) and set `PASSWORD_TO_HASH`.
2. From this directory, build the dependency classpath and run the tool:

   ```powershell
   # Windows (PowerShell)
   .\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target/classpath.txt"
   java --class-path (Get-Content target/classpath.txt) tools/password-hash/GeneratePasswordHash.java
   ```

   ```sh
   # macOS / Linux
   ./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
   java --class-path "$(cat target/classpath.txt)" tools/password-hash/GeneratePasswordHash.java
   ```

You can pass a different `.env` path as an argument. Passwords are limited to 72 UTF-8 bytes, which is BCrypt's limit. Delete the `.env` file when you're done.
