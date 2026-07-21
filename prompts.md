# Codex Prompt Log — Car Dealership API

A running record of prompts used to drive this project via strict TDD (red-green-refactor).

---

## 1. Boilerplate Setup

**Goal:** Scaffold empty feature packages (main + test) before any code exists.

> Create the following empty packages (with `.gitkeep` placeholder files, no stub classes) under `src/main/java/com/atharva/dealership/`:
> - `auth` — registration, login, JWT issuance
> - `user` — User entity, roles
> - `vehicle` — Vehicle entity, CRUD, search
> - `inventory` — purchase/restock logic
> - `security` — SecurityConfig, JWT filter
> - `exception` — global exception handling
> - `dto` — request/response records
>
> Also create the corresponding mirrored structure under `src/test/java/com/atharva/dealership/` for the same packages, so test classes have a home from the start.
>
> Do not create any actual Java classes, controllers, services, or stub implementations — only the package folders and `.gitkeep` files. I'll be following strict TDD and want the structure ready before writing any failing tests.
>
> After creating the structure, show me the resulting file tree so I can verify it matches.

---

## 2. MySQL / Environment Setup

**Goal:** Configuration only — no Java code, no entities, no controllers.

> Do the following, configuration only — no Java code, no entities, no controllers:
>
> 1. Add `com.h2database:h2` as a `runtimeOnly` (or `testRuntimeOnly`) dependency in `build.gradle`, scoped so it's only used for tests.
>
> 2. In `src/main/resources/application.properties`, configure for MySQL:
>    - datasource URL pointing to `localhost:3306`, database `dealership_db`
>    - username/password read from environment variables `DB_USER` and `DB_PASSWORD`, with local defaults `dealership_user` / `dealership_pass`
>    - `spring.jpa.hibernate.ddl-auto=update`
>    - `spring.jpa.show-sql=true`
>    - correct MySQL dialect property
>
> 3. Create `src/test/resources/application.properties` configured for H2 in-memory, MySQL compatibility mode (`MODE=MySQL`), so JPA entity behavior matches production MySQL as closely as possible:
>    - `ddl-auto=create-drop`
>    - correct H2 dialect property
>    - this file should apply automatically when running `./gradlew test`, no active profile flag needed
>
> 4. Create a `docker-compose.yml` in the project root defining a MySQL 8.0 service:
>    - database name, username, password matching what's in `application.properties`
>    - exposed on port `3306`
>    - a named volume for data persistence
>
> Do not modify anything outside `build.gradle`, the two `application.properties` files, and `docker-compose.yml`.

---

## 3. Register Endpoint

### 3a. Test Case Prompts

**3a.1 — Happy path unit test (RED)**

> I'm building the first feature in a brand-new Spring Boot project using strict TDD (red-green-refactor). There are currently zero controllers, services, repositories, or entities anywhere in the codebase — only empty folders organized by feature, each containing a `.gitkeep`.
>
> Write ONLY a JUnit 5 + Mockito unit test for `UserService.registerUser(...)`. Do NOT create `UserService`, `UserRepository`, `User`, `RegisterUserRequest`, `PasswordEncoder` bean config, or any other production class. This test should fail to compile — that is the expected "red" step.
>
> Use `@ExtendWith(MockitoExtension.class)`. Mock `UserRepository` (a Spring Data JPA repository) and `PasswordEncoder` (from Spring Security's `org.springframework.security.crypto.password.PasswordEncoder`) using `@Mock`, and inject them into `UserService` using `@InjectMocks`.
>
> **Requirement (happy path only):**
> Given a `RegisterUserRequest` with a valid, unique email and a valid raw password, when `userService.registerUser(request)` is called, then:
> - `passwordEncoder.encode(rawPassword)` is called exactly once with the raw password from the request
> - `userRepository.save(...)` is called exactly once with a `User` whose password field equals the **encoded** password returned by the mocked `passwordEncoder.encode(...)` — NOT the raw password
> - The method returns a result whose email matches the input email (decide for yourself whether this should be the `User` entity or a separate `UserResponse`/DTO — document your choice in a comment, don't build the actual class)
>
> Do not test duplicate-email handling, validation, or exceptions in this test — that's a separate test case. Keep this test scoped strictly to the happy path.
>
> Add a comment block at the top of the test listing every class, method, and field it assumes exists (e.g. `UserService.registerUser(RegisterUserRequest)`, `User` fields, `RegisterUserRequest` fields, `PasswordEncoder.encode(String)`, `UserRepository.save(User)`), so I know exactly what to scaffold in the "green" step.
>
> Do not create a build file dependency list this time — assume Mockito and JUnit 5 are already present via `spring-boot-starter-test` (standard for any Spring Boot project), since this is a pure unit test with no Spring context involved.

**3a.2 — Validation failure test (RED)**

> Write a failing JUnit test (`{TEST_TYPE}=unit`) for `UserService.register`. Test that when given an invalid email (e.g. `"not-an-email"`) or a password that fails complexity rules (e.g. shorter than 8 characters), the method throws a `ValidationError`. Assert the repository's create method is NEVER called in either failure case — use a mock and check `toHaveBeenCalledTimes(0)`. Only write the test file; do not modify `AuthService`.

**3a.3 — Duplicate email test (RED)**

> Write a failing JUnit unit test for `UserService.register`. Mock the repository so that looking up an existing email (e.g. via `findByEmail`) returns an existing user. Assert `UserService.register` throws a `DuplicateEmailError` and that create is never called. Only write the test file.

**3a.4 — Email normalization test (RED)**

> Write a failing JUnit unit test for `UserService.register`. Mock the repository so `findByEmail` returns an existing user for `"test@example.com"`. Call register with `"Test@Example.com"` (mixed case) and assert it throws `EmailAlreadyExistsError` — proving the service normalizes email before the uniqueness check, not just relying on a DB constraint. Also assert that when save IS called (in a separate non-conflicting case), the email passed to the repository is the lowercased/trimmed form. Only write the test file.

**3a.5 — Batch of remaining red-step tests**

> Write the test cases for the following:
> 1. Edge Case: Hashing Correctness (unit)
> 2. Full Register → Persist → Fetch-Back (integration smoke test)
> 3. Request Shape Validation → HTTP Status Mapping (controller, unit)
>
> These are meant to fail as the "red" step in the TDD, so do not implement any solutions to fix it yet.

### 3b. Implementation Prompts (GREEN)

**3b.1 — Minimum code for happy-path test**

> Now write ONLY the minimum production code needed to make this ONE test pass — nothing more. This is the "green" step: no extra validation, no duplicate-email checking, no exception handling, no additional fields beyond what the test requires, no controller, no repository implementation beyond the interface, no database migration/schema. If the test doesn't require it, don't write it.
>
> Based on that test, create:
> - `RegisterUserRequest` — a simple request object/record with only the fields the test references
> - `User` — a JPA entity (or plain class if the test doesn't need persistence annotations yet) with only the fields the test references
> - `UserRepository` — a Spring Data JPA interface extending `JpaRepository<User, ...>`, with only the method(s) the test actually calls (e.g. `save`)
> - `UserService` — with a constructor taking `UserRepository` and `PasswordEncoder` (matching what `@InjectMocks` expects), and a `registerUser(RegisterUserRequest request)` method that:
>   - calls `passwordEncoder.encode(...)` on the raw password
>   - builds a `User` with the encoded password
>   - calls `userRepository.save(...)`
>   - returns whatever the test asserts on (document in a comment whether it's the `User` entity or a separate response type, matching what the test expects)
>
> Do not add a `PasswordEncoder` bean configuration class unless the test's `@Mock`/`@InjectMocks` setup requires a real Spring bean — for a pure Mockito unit test it shouldn't.
>
> After writing this, run the test and confirm it passes. If it doesn't pass on the first attempt, tell me exactly what failed and why, rather than silently adding more code to force it green — I want to understand the failure.

**3b.2 — Add validation**

> Add input validation to `UserService.register` to make the failing test in `C:\Atharva\Projects\car-dealership-api\src\test\java\com\atharva\dealership\user\UserServiceTest.java` pass. Validate email format and password complexity (minimum length 8, adjust to match project's existing password policy if one exists in the codebase — check for it first). Throw `ValidationError` with a descriptive message before any repository call. Do not touch the happy-path or conflict logic.

**3b.3 — Duplicate-email check**

> Update `UserService.register` to check for an existing user by email before creating a new one, making the failing test in `{test file path}` pass. Throw `EmailAlreadyExistsError` (rename the `DuplicateError`) if found. Ensure this check happens after validation but before hashing/persistence, so invalid emails still fail with `ValidationError` first (don't break the Case 2 test).

**3b.4 — Email normalization**

> Update `UserService.register` to trim and lowercase the email before both the `findByEmail` duplicate check and the create call, making the previous failing test pass. Apply normalization as early as possible (right after basic validation) so it's consistent across all downstream logic.
>
> Write minimal required code to pass these test cases.

---

## 4. Chore: Structured Logging

**Goal:** Add SLF4J + Lombok `@Slf4j` logging for observability, without touching tests or behavior.

> Add structured application logging using SLF4J with Lombok's `@Slf4j` annotation wherever it improves observability.
>
> **Requirements:**
> - Do not modify any test files.
> - Do not add logging to unit tests, integration tests, or test utilities.
> - Use `@Slf4j` instead of manually creating Logger instances.
> - Add meaningful logs only at important application flow points; avoid excessive logging.
>
> **Add logs in production code where appropriate:**
>
> *Service layer:*
> - Log the start of important business operations.
> - Log successful completion of operations.
> - Log important decisions (e.g. duplicate email detection, validation failures, state changes).
> - Log exceptions before propagating them where it adds debugging value.
>
> *Controller layer:*
> - Log incoming business actions (do not log sensitive request data).
> - Log successful request handling.
> - Log failures that result in error responses.
>
> *Repository/infrastructure-related code:*
> - Add logs only if there is custom logic or exception handling.
> - Do not add unnecessary logs around standard Spring Data repository methods.
>
> **Logging guidelines:**
> - Levels:
>   - `INFO` — successful business events and important lifecycle events.
>   - `WARN` — expected exceptional situations (e.g., duplicate registration attempt).
>   - `ERROR` — unexpected failures with stack traces.
>   - `DEBUG` — detailed diagnostic information useful during development.
> - Never log:
>   - Raw passwords.
>   - Password hashes.
>   - Tokens.
>   - Sensitive user information.
> - Prefer parameterized logging:
>   ```java
>   log.info("Registering user with email: {}", email);
>   ```
>   instead of string concatenation.
>
> **After adding logs:**
> - Ensure imports are clean.
> - Ensure the project compiles.
> - Do not change business logic or behavior.
> - Keep the changes limited to observability improvements.

---

## 5. Login Endpoint

**Goal:** Implement JWT-based login, then refactor the refresh token flow to be fully stateless.

### 5a. Plan Prompts

> I want to implement a login endpoint. It should support JWT-based authentication. Give a plan to implement this with a test-driven development approach.

### 5b. Test Case Prompts (RED)

> Follow this plan. Start by writing all failing test cases first. Do not write any implementation code yet. Only confirm the changes after I approve.

### 5c. Implementation Prompts (GREEN)

> All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Do not apply any changes yet. Show me a diff-style preview (`--- current` / `+++ proposed`, with `+`/`-` markers) for every file you intend to create or modify. Do not run, save, commit, or confirm anything on your own. Wait for my explicit review and approval before writing to disk. If I request edits, regenerate the diff and wait again. After I approve and changes are applied, run the test suite and report the actual pass/fail result — don't assume success.

### 5d. Refactor Prompts

> Refactor the refresh token flow in `AuthService.java` (`com.atharva.dealership.auth`) to be **fully stateless**: refresh tokens should be self-contained signed JWTs with a 7-day expiration, with no database persistence, no `RefreshTokenRepository`, and no revocation mechanism. This is an intentional tradeoff — revocation is explicitly out of scope for this refactor.
>
> **Requirements:**
>
> 1. **Extend `JwtService`** to support refresh token generation and validation, mirroring the access token methods:
>    - `generateRefreshToken(String subject)` — same structure as `generateAccessToken`, but using `refreshTokenExpirationSeconds` for the expiry claim. If the jjwt refactor from earlier is already in place, reuse `Jwts.builder()`; otherwise use the existing signing mechanism.
>    - `extractRefreshSubject(String token)` / `isValidRefreshToken(String token)` — parse and validate a refresh token the same way access tokens are validated (signature, expiration), reusing existing private helpers where possible rather than duplicating logic.
>    - Consider adding a `"type":"refresh"` (or similar) claim to both token types so a refresh token can't be replayed as an access token and vice versa. Validate this claim on parse for each respective method.
>
> 2. **Update `AuthService`:**
>    - Remove `RefreshTokenRepository`, `RefreshToken` entity usage, `hashRefreshToken`, and the SHA-256/`HexFormat` hashing logic entirely — no longer needed since nothing is persisted.
>    - Remove `generateRefreshToken()` (the `SecureRandom`/Base64 version) and the `secureRandom` field.
>    - `issueTokens(User user)`: generate the refresh token via `jwtService.generateRefreshToken(user.getEmail())` instead of the random-bytes version. No DB save.
>    - `refresh(String rawRefreshToken)`: replace the DB lookup/revocation-check logic with:
>      - Validate the token via `jwtService.isValidRefreshToken(...)`, throwing `AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE)` if invalid or expired.
>      - Extract the subject (email) and re-fetch the `User` via `userRepository.findByEmail(...)`, throwing the same error if the user no longer exists (e.g. deleted account).
>      - Call `issueTokens(user)` and return the new `AuthResponse` as before.
>      - Remove the `@Transactional` annotation from `refresh()` if nothing in the method touches the DB for writes anymore (keep it only if `findByEmail` needs it for other reasons — check).
>
> 3. **Delete now-unused code:**
>    - `RefreshToken` entity class.
>    - `RefreshTokenRepository` interface.
>    - Any Flyway/Liquibase migration or schema definition for the refresh token table (leave a note/comment or a follow-up migration to drop the table if it already exists in deployed environments — don't silently leave orphaned schema).
>    - Any Spring config or bean wiring referencing `RefreshTokenRepository`.
>
> 4. **Logging:** keep existing `log.info`/`log.warn`/`log.debug` statements at equivalent points (start of refresh, invalid token, success), adjusted for the new flow. Do not log token values.
>
> 5. **Tests:**
>    - Update/replace `AuthService` tests that relied on `RefreshTokenRepository` mocks — remove those mocks and DB-interaction assertions.
>    - Add/update tests:
>      - valid refresh token round-trip issues new access+refresh tokens
>      - expired refresh token is rejected
>      - tampered/malformed refresh token is rejected
>      - a refresh token used where an access token is expected (and vice versa) is rejected, if the `type` claim is added
>      - refresh for a since-deleted user is rejected
>    - Use the injected `Clock` to simulate expiration rather than `Thread.sleep`.
>
> 6. **Explicitly do not implement:** revocation, reuse detection, logout endpoints that invalidate tokens server-side, or a token blocklist. If a `logout` endpoint currently exists and relies on DB revocation, update it to be a client-side-only operation (i.e., the client discards the tokens) and note in a comment that server-side logout is not possible with this design.

---

# Vehicle:

**Goal:** To add all functionalities related to vehicles.

## 1. Add Vehicle Endpoint

### 1a. Plan prompts:

> This project is a car dealership inventory system. Now the authentication part is done, Its  time to implement the core functionalities.
>
>- Vehicles (Protected):
>- ***POST /api/vehicles: Add a new vehicle.***
>- ***GET /api/vehicles: View a list of all available vehicles.***
>- ***GET /api/vehicles/search: Search for vehicles by make, model, category, or price range.***
>- ***PUT /api/vehicles/:id: Update a vehicle's details.***
>- ***DELETE /api/vehicles/:id: Delete a vehicle (Admin only).***
>- ***Each vehicle must have a unique ID, make, model, category, price, and quantity in stock.***
>
>Knowing this, derive a plan for the first endpoint ( add a new vehicle). Follow the test-driven-development. There should be 3 types of tests. Units tests, integration tests and end to end tests.
>
>
>### Test Case Prompts (RED)
>
> Write all the necessary failing tests first except end to end tests. Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. These test cases are meant to fail. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.


## Template for Future Entries

```
## N. <Feature/Chore Name>

**Goal:** <one-line summary>

### Test Case Prompts (RED)
> <prompt text>

### Implementation Prompts (GREEN)
> <prompt text>
```