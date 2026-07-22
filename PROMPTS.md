# Brainstorm with Claude:
> 
> ***The goal to design, build, and test a full-stack Car Dealership Inventory System. This project will test your skills in API development, database management, frontend implementation, testing, and modern development workflows, including the use of AI tools.***
> 
> ***Core Requirements***
> 
> ***1. Backend API (RESTful)***
> 
> ***You are to build a robust backend API that will serve as the brain of the application.***
> 
> - **Technology:** Java SpringBoot
> - **Database:** MySQL
> - **User Authentication:**
> - Users must be able to register and log in.
> - Implement token-based authentication (e.g., JWT) to secure certain API endpoints.
> - **API Endpoints:**
> - ***Auth: POST /api/auth/register, POST /api/auth/login***
> - Vehicles (Protected):
> - ***POST /api/vehicles: Add a new vehicle.***
> - ***GET /api/vehicles: View a list of all available vehicles.***
> - ***GET /api/vehicles/search: Search for vehicles by make, model, category, or price range.***
> - ***PUT /api/vehicles/:id: Update a vehicle's details.***
> - ***DELETE /api/vehicles/:id: Delete a vehicle (Admin only).***
> - Inventory (Protected):
> - ***POST /api/vehicles/:id/purchase: Purchase a vehicle, decreasing its quantity.***
> - ***POST /api/vehicles/:id/restock: Restock a vehicle, increasing its quantity (Admin only).***
> 
> ***Each vehicle must have a unique ID, make, model, category, price, and quantity in stock.***
> 
> Suggest a detailed plan. It should follow the Test Driven Development approach and match industry standards.

 # Codex Prompt Log — Car Dealership API
> 
A running record of prompts used to drive this project via strict TDD (red-green-refactor).
> 
> ---
> 
## 1. Boilerplate Setup
> 
> **Goal:** Scaffold empty feature packages (main + test) before any code exists.
> 
>  Create the following empty packages (with `.gitkeep` placeholder files, no stub classes) under `src/main/java/com/atharva/dealership/`:
>  - `auth` — registration, login, JWT issuance
>  - `user` — User entity, roles
>  - `vehicle` — Vehicle entity, CRUD, search
>  - `inventory` — purchase/restock logic
>  - `security` — SecurityConfig, JWT filter
>  - `exception` — global exception handling
>  - `dto` — request/response records
> 
>  Also create the corresponding mirrored structure under `src/test/java/com/atharva/dealership/` for the same packages, so test classes have a home from the start.
> 
>  Do not create any actual Java classes, controllers, services, or stub implementations — only the package folders and `.gitkeep` files. I'll be following strict TDD and want the structure ready before writing any failing tests.
> 
>  After creating the structure, show me the resulting file tree so I can verify it matches.
> 
> ---
> 
## 2. MySQL / Environment Setup
> 
> **Goal:** Configuration only — no Java code, no entities, no controllers.
> 
>  Do the following, configuration only — no Java code, no entities, no controllers:
> 
>  1. Add `com.h2database:h2` as a `runtimeOnly` (or `testRuntimeOnly`) dependency in `build.gradle`, scoped so it's only used for tests.
> 
>  2. In `src/main/resources/application.properties`, configure for MySQL:
>     - datasource URL pointing to `localhost:3306`, database `dealership_db`
>     - username/password read from environment variables `DB_USER` and `DB_PASSWORD`, with local defaults `dealership_user` / `dealership_pass`
>     - `spring.jpa.hibernate.ddl-auto=update`
>     - `spring.jpa.show-sql=true`
>     - correct MySQL dialect property
> 
>  3. Create `src/test/resources/application.properties` configured for H2 in-memory, MySQL compatibility mode (`MODE=MySQL`), so JPA entity behavior matches production MySQL as closely as possible:
>     - `ddl-auto=create-drop`
>     - correct H2 dialect property
>     - this file should apply automatically when running `./gradlew test`, no active profile flag needed
> 
>  4. Create a `docker-compose.yml` in the project root defining a MySQL 8.0 service:
>     - database name, username, password matching what's in `application.properties`
>     - exposed on port `3306`
>     - a named volume for data persistence
> 
>  Do not modify anything outside `build.gradle`, the two `application.properties` files, and `docker-compose.yml`.
> 
> ---
> 
## 3. Register Endpoint
> 
### 3a. Test Case Prompts
> 
> **3a.1 — Happy path unit test (RED)**
> 
>  I'm building the first feature in a brand-new Spring Boot project using strict TDD (red-green-refactor). There are currently zero controllers, services, repositories, or entities anywhere in the codebase — only empty folders organized by feature, each containing a `.gitkeep`.
> 
>  Write ONLY a JUnit 5 + Mockito unit test for `UserService.registerUser(...)`. Do NOT create `UserService`, `UserRepository`, `User`, `RegisterUserRequest`, `PasswordEncoder` bean config, or any other production class. This test should fail to compile — that is the expected "red" step.
> 
>  Use `@ExtendWith(MockitoExtension.class)`. Mock `UserRepository` (a Spring Data JPA repository) and `PasswordEncoder` (from Spring Security's `org.springframework.security.crypto.password.PasswordEncoder`) using `@Mock`, and inject them into `UserService` using `@InjectMocks`.
> 
>  **Requirement (happy path only):**
>  Given a `RegisterUserRequest` with a valid, unique email and a valid raw password, when `userService.registerUser(request)` is called, then:
>  - `passwordEncoder.encode(rawPassword)` is called exactly once with the raw password from the request
>  - `userRepository.save(...)` is called exactly once with a `User` whose password field equals the **encoded** password returned by the mocked `passwordEncoder.encode(...)` — NOT the raw password
>  - The method returns a result whose email matches the input email (decide for yourself whether this should be the `User` entity or a separate `UserResponse`/DTO — document your choice in a comment, don't build the actual class)
> 
>  Do not test duplicate-email handling, validation, or exceptions in this test — that's a separate test case. Keep this test scoped strictly to the happy path.
> 
>  Add a comment block at the top of the test listing every class, method, and field it assumes exists (e.g. `UserService.registerUser(RegisterUserRequest)`, `User` fields, `RegisterUserRequest` fields, `PasswordEncoder.encode(String)`, `UserRepository.save(User)`), so I know exactly what to scaffold in the "green" step.
> 
>  Do not create a build file dependency list this time — assume Mockito and JUnit 5 are already present via `spring-boot-starter-test` (standard for any Spring Boot project), since this is a pure unit test with no Spring context involved.
> 
> **3a.2 — Validation failure test (RED)**
> 
>  Write a failing JUnit test (`{TEST_TYPE}=unit`) for `UserService.register`. Test that when given an invalid email (e.g. `"not-an-email"`) or a password that fails complexity rules (e.g. shorter than 8 characters), the method throws a `ValidationError`. Assert the repository's create method is NEVER called in either failure case — use a mock and check `toHaveBeenCalledTimes(0)`. Only write the test file; do not modify `AuthService`.
> 
> **3a.3 — Duplicate email test (RED)**
> 
>  Write a failing JUnit unit test for `UserService.register`. Mock the repository so that looking up an existing email (e.g. via `findByEmail`) returns an existing user. Assert `UserService.register` throws a `DuplicateEmailError` and that create is never called. Only write the test file.
> 
> **3a.4 — Email normalization test (RED)**
> 
>  Write a failing JUnit unit test for `UserService.register`. Mock the repository so `findByEmail` returns an existing user for `"test@example.com"`. Call register with `"Test@Example.com"` (mixed case) and assert it throws `EmailAlreadyExistsError` — proving the service normalizes email before the uniqueness check, not just relying on a DB constraint. Also assert that when save IS called (in a separate non-conflicting case), the email passed to the repository is the lowercased/trimmed form. Only write the test file.
> 
> **3a.5 — Batch of remaining red-step tests**
> 
>  Write the test cases for the following:
>  1. Edge Case: Hashing Correctness (unit)
>  2. Full Register → Persist → Fetch-Back (integration smoke test)
>  3. Request Shape Validation → HTTP Status Mapping (controller, unit)
> 
>  These are meant to fail as the "red" step in the TDD, so do not implement any solutions to fix it yet.
> 
### 3b. Implementation Prompts (GREEN)
> 
> **3b.1 — Minimum code for happy-path test**
> 
>  Now write ONLY the minimum production code needed to make this ONE test pass — nothing more. This is the "green" step: no extra validation, no duplicate-email checking, no exception handling, no additional fields beyond what the test requires, no controller, no repository implementation beyond the interface, no database migration/schema. If the test doesn't require it, don't write it.
> 
>  Based on that test, create:
>  - `RegisterUserRequest` — a simple request object/record with only the fields the test references
>  - `User` — a JPA entity (or plain class if the test doesn't need persistence annotations yet) with only the fields the test references
>  - `UserRepository` — a Spring Data JPA interface extending `JpaRepository<User, ...> `, with only the method(s) the test actually calls (e.g. `save`)
>  - `UserService` — with a constructor taking `UserRepository` and `PasswordEncoder` (matching what `@InjectMocks` expects), and a `registerUser(RegisterUserRequest request)` method that:
>    - calls `passwordEncoder.encode(...)` on the raw password
>    - builds a `User` with the encoded password
>    - calls `userRepository.save(...)`
>    - returns whatever the test asserts on (document in a comment whether it's the `User` entity or a separate response type, matching what the test expects)
> 
>  Do not add a `PasswordEncoder` bean configuration class unless the test's `@Mock`/`@InjectMocks` setup requires a real Spring bean — for a pure Mockito unit test it shouldn't.
> 
>  After writing this, run the test and confirm it passes. If it doesn't pass on the first attempt, tell me exactly what failed and why, rather than silently adding more code to force it green — I want to understand the failure.
> 
> **3b.2 — Add validation**
> 
>  Add input validation to `UserService.register` to make the failing test in `C:\Atharva\Projects\car-dealership-api\src\test\java\com\atharva\dealership\user\UserServiceTest.java` pass. Validate email format and password complexity (minimum length 8, adjust to match project's existing password policy if one exists in the codebase — check for it first). Throw `ValidationError` with a descriptive message before any repository call. Do not touch the happy-path or conflict logic.
> 
> **3b.3 — Duplicate-email check**
> 
>  Update `UserService.register` to check for an existing user by email before creating a new one, making the failing test in `{test file path}` pass. Throw `EmailAlreadyExistsError` (rename the `DuplicateError`) if found. Ensure this check happens after validation but before hashing/persistence, so invalid emails still fail with `ValidationError` first (don't break the Case 2 test).
> 
> **3b.4 — Email normalization**
> 
>  Update `UserService.register` to trim and lowercase the email before both the `findByEmail` duplicate check and the create call, making the previous failing test pass. Apply normalization as early as possible (right after basic validation) so it's consistent across all downstream logic.
> 
>  Write minimal required code to pass these test cases.
> 
> ---
> 
## 4. Chore: Structured Logging
> 
> **Goal:** Add SLF4J + Lombok `@Slf4j` logging for observability, without touching tests or behavior.
> 
>  Add structured application logging using SLF4J with Lombok's `@Slf4j` annotation wherever it improves observability.
> 
>  **Requirements:**
>  - Do not modify any test files.
>  - Do not add logging to unit tests, integration tests, or test utilities.
>  - Use `@Slf4j` instead of manually creating Logger instances.
>  - Add meaningful logs only at important application flow points; avoid excessive logging.
> 
>  **Add logs in production code where appropriate:**
> 
>  *Service layer:*
>  - Log the start of important business operations.
>  - Log successful completion of operations.
>  - Log important decisions (e.g. duplicate email detection, validation failures, state changes).
>  - Log exceptions before propagating them where it adds debugging value.
> 
>  *Controller layer:*
>  - Log incoming business actions (do not log sensitive request data).
>  - Log successful request handling.
>  - Log failures that result in error responses.
> 
>  *Repository/infrastructure-related code:*
>  - Add logs only if there is custom logic or exception handling.
>  - Do not add unnecessary logs around standard Spring Data repository methods.
> 
>  **Logging guidelines:**
>  - Levels:
>    - `INFO` — successful business events and important lifecycle events.
>    - `WARN` — expected exceptional situations (e.g., duplicate registration attempt).
>    - `ERROR` — unexpected failures with stack traces.
>    - `DEBUG` — detailed diagnostic information useful during development.
>  - Never log:
>    - Raw passwords.
>    - Password hashes.
>    - Tokens.
>    - Sensitive user information.
>  - Prefer parameterized logging:
>    ```java
>    log.info("Registering user with email: {}", email);
>    ```
>    instead of string concatenation.
> 
>  **After adding logs:**
>  - Ensure imports are clean.
>  - Ensure the project compiles.
>  - Do not change business logic or behavior.
>  - Keep the changes limited to observability improvements.
> 
> ---
> 
## 5. Login Endpoint
> 
> **Goal:** Implement JWT-based login, then refactor the refresh token flow to be fully stateless.
> 
### 5a. Plan Prompts
> 
>  I want to implement a login endpoint. It should support JWT-based authentication. Give a plan to implement this with a test-driven development approach.
> 
### 5b. Test Case Prompts (RED)
> 
>  Follow this plan. Start by writing all failing test cases first. Do not write any implementation code yet. Only confirm the changes after I approve.
> 
### 5c. Implementation Prompts (GREEN)
> 
>  All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Do not apply any changes yet. Show me a diff-style preview (`--- current` / `+++ proposed`, with `+`/`-` markers) for every file you intend to create or modify. Do not run, save, commit, or confirm anything on your own. Wait for my explicit review and approval before writing to disk. If I request edits, regenerate the diff and wait again. After I approve and changes are applied, run the test suite and report the actual pass/fail result — don't assume success.
> 
### 5d. Refactor Prompts
> 
>  Refactor the refresh token flow in `AuthService.java` (`com.atharva.dealership.auth`) to be **fully stateless**: refresh tokens should be self-contained signed JWTs with a 7-day expiration, with no database persistence, no `RefreshTokenRepository`, and no revocation mechanism. This is an intentional tradeoff — revocation is explicitly out of scope for this refactor.
> 
>  **Requirements:**
> 
>  1. **Extend `JwtService`** to support refresh token generation and validation, mirroring the access token methods:
>     - `generateRefreshToken(String subject)` — same structure as `generateAccessToken`, but using `refreshTokenExpirationSeconds` for the expiry claim. If the jjwt refactor from earlier is already in place, reuse `Jwts.builder()`; otherwise use the existing signing mechanism.
>     - `extractRefreshSubject(String token)` / `isValidRefreshToken(String token)` — parse and validate a refresh token the same way access tokens are validated (signature, expiration), reusing existing private helpers where possible rather than duplicating logic.
>     - Consider adding a `"type":"refresh"` (or similar) claim to both token types so a refresh token can't be replayed as an access token and vice versa. Validate this claim on parse for each respective method.
> 
>  2. **Update `AuthService`:**
>     - Remove `RefreshTokenRepository`, `RefreshToken` entity usage, `hashRefreshToken`, and the SHA-256/`HexFormat` hashing logic entirely — no longer needed since nothing is persisted.
>     - Remove `generateRefreshToken()` (the `SecureRandom`/Base64 version) and the `secureRandom` field.
>     - `issueTokens(User user)`: generate the refresh token via `jwtService.generateRefreshToken(user.getEmail())` instead of the random-bytes version. No DB save.
>     - `refresh(String rawRefreshToken)`: replace the DB lookup/revocation-check logic with:
>       - Validate the token via `jwtService.isValidRefreshToken(...)`, throwing `AuthenticationError(INVALID_REFRESH_TOKEN_MESSAGE)` if invalid or expired.
>       - Extract the subject (email) and re-fetch the `User` via `userRepository.findByEmail(...)`, throwing the same error if the user no longer exists (e.g. deleted account).
>       - Call `issueTokens(user)` and return the new `AuthResponse` as before.
>       - Remove the `@Transactional` annotation from `refresh()` if nothing in the method touches the DB for writes anymore (keep it only if `findByEmail` needs it for other reasons — check).
> 
>  3. **Delete now-unused code:**
>     - `RefreshToken` entity class.
>     - `RefreshTokenRepository` interface.
>     - Any Flyway/Liquibase migration or schema definition for the refresh token table (leave a note/comment or a follow-up migration to drop the table if it already exists in deployed environments — don't silently leave orphaned schema).
>     - Any Spring config or bean wiring referencing `RefreshTokenRepository`.
> 
>  4. **Logging:** keep existing `log.info`/`log.warn`/`log.debug` statements at equivalent points (start of refresh, invalid token, success), adjusted for the new flow. Do not log token values.
> 
>  5. **Tests:**
>     - Update/replace `AuthService` tests that relied on `RefreshTokenRepository` mocks — remove those mocks and DB-interaction assertions.
>     - Add/update tests:
>       - valid refresh token round-trip issues new access+refresh tokens
>       - expired refresh token is rejected
>       - tampered/malformed refresh token is rejected
>       - a refresh token used where an access token is expected (and vice versa) is rejected, if the `type` claim is added
>       - refresh for a since-deleted user is rejected
>     - Use the injected `Clock` to simulate expiration rather than `Thread.sleep`.
> 
>  6. **Explicitly do not implement:** revocation, reuse detection, logout endpoints that invalidate tokens server-side, or a token blocklist. If a `logout` endpoint currently exists and relies on DB revocation, update it to be a client-side-only operation (i.e., the client discards the tokens) and note in a comment that server-side logout is not possible with this design.
> 
> ---
> 
# Vehicle:
> 
> **Goal:** To add all functionalities related to vehicles.
> 
## 1. Add Vehicle Endpoint
> 
### 1a. Plan prompts:
> 
>  This project is a car dealership inventory system. Now the authentication part is done, Its  time to implement the core functionalities.
> 
> - Vehicles (Protected):
> - ***POST /api/vehicles: Add a new vehicle.***
> - ***GET /api/vehicles: View a list of all available vehicles.***
> - ***GET /api/vehicles/search: Search for vehicles by make, model, category, or price range.***
> - ***PUT /api/vehicles/:id: Update a vehicle's details.***
> - ***DELETE /api/vehicles/:id: Delete a vehicle (Admin only).***
> - ***Each vehicle must have a unique ID, make, model, category, price, and quantity in stock.***
> 
> Knowing this, derive a plan for the first endpoint ( add a new vehicle). Follow the test-driven-development. There should be 3 types of tests. Units tests, integration tests and end to end tests.
> 
> 
### 1b. Test Case Prompts (RED)
> 
>  Write all the necessary failing tests first except end to end tests. Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. These test cases are meant to fail. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### 1c. Implementation Prompts (GREEN)
> 
>  All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Make sure to follow best industry practices and naming conventions.
> 
## 2. View All Vehicles Endpoint
> 
### 2a. Plan prompts:
> 
>  Now to implement a new endpoint - GET /api/vehicles: View a list of all available vehicles. What are some important cases to write tests for this endpoint? 
> 
### 2b. Test case prompts (RED) :
> 
>  Write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### 2c. Implementation prompts (GREEN):
> 
>   All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.
> 
## 3. Search Vehicles Endpoint
> 
### 3a. Plan prompts:
> 
>  Now to implement a new endpoint - GET /api/vehicles/search: Search for vehicles by make, model, category, or price range.. What are some important cases to write tests for this endpoint?
> 
### 3b. Test case prompts (RED) :
> 
>  Write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### 3c. Implementation prompts (GREEN):
> 
>   All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.
> 
## 4. Update Vehicles Endpoint
> 
### 4a. Plan prompts:
> 
>  Now to implement a new endpoint - PUT /api/vehicles/:id: Update a vehicle's details. What are some important cases to write tests for this endpoint?
> 
### 4b. Test case prompts (RED) :
> 
>  Write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### 4c. Implementation prompts (GREEN):
> 
>   All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.
> 
## 5. Delete Vehicles Endpoint
> 
### 5a. Plan prompts:
> 
>  Now to implement a new endpoint -DELETE /api/vehicles/:id: Delete a vehicle (Admin only). What are some important cases to write tests for this endpoint?
> 
> There will be an inventory feature with following endpoints:
> - Inventory (Protected):
> - ***POST /api/vehicles/:id/purchase: Purchase a vehicle, decreasing its quantity.***
> - ***POST /api/vehicles/:id/restock: Restock a vehicle, increasing its quantity (Admin only).***
> How does that affect the related data behaviors?
> 
### 5b. Test case prompts (RED) :
> 
>  Alright. Based on what we have discussed, write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### 5c. Implementation prompts (GREEN):
> 
>   All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.
> 
> 
# Frontend:
> 
## Brainstorming:
> 
> Now inventory endpoints are left which will be done after we setup UI for the current functionalities. The requirements are as follows:
> You must build a modern, single-page application (SPA) to interact with your backend API.
> 
> - **Technology:** You must use HTML5, CSS3, Tailwind, and React
> - **Functionality:**
> - User registration and login forms.
> - A dashboard or homepage to display all available vehicles.
> - Functionality to search and filter vehicles.
> - A "Purchase" button on each vehicle, which should be disabled if the quantity is zero.
> - (For Admin Users) Forms/UI to add, update, and delete vehicles.
> - **Design:** This is a chance to show your creativity. The application should be visually appealing, responsive, and provide a great user experience.
> Suggest a plan for this implementation. Also where should the frontend directory be? Separate from backend or both bundled together?
> 
>  What parts of frontend should follow TDD and where TDD is inefficient?
> 
>  Divide the frontend implementation into smaller steps by feature. It should be easy to implement one step at a time, just like how we did the endpoints, one endpoint at a time.
> 
## Frontend Setup:
> 
>  Implement this step
> Step 1: Frontend Project Setup 
> 
> - Create `frontend/` using Vite + React.
> - Install Tailwind CSS.
> - Configure Tailwind.
> - Add basic app shell:
>     - header
>     - main content area
>     - loading/error styles
> - Configure Vite proxy:
> 
> ```jsx
> /api ->  <http://localhost:3000> 
> ```
> 
> Tests/checks:
> 
> - App starts with `npm run dev`.
> - Tailwind styles apply correctly.
> - A placeholder page renders
> 
>  Make sure you follow industry standard directory structure.

## Brainstorm with Claude:

> Step 2: API Client Layer

> * Create a small API wrapper for backend calls.
> * Add support for:
>    * base `/api` path
>    * JSON request/response handling
>    * error parsing
>    * attaching bearer token when present
> * Add API functions for:
>    * `register`
>    * `login`
>    * `refresh`
>    * `listVehicles`
>    * `searchVehicles`
>    * `createVehicle`
>    * `updateVehicle`
>    * `deleteVehicle`
> 
> TDD recommended here.
> Tests:
> 
> * Login sends the correct payload.
> * Vehicle search builds correct query params.
> * Authenticated requests include `Authorization`.
> 
> 
> Since TDD is recommended here, how should I proceed?

>  Draft a prompt for codex to implement the step 2 based on what we have discussed. It shows follow industry standards, naming conventions, directory structure etc.

## Feature -  API Client Layer:

>  - Create a small API wrapper for backend calls.
> - Add support for:
>     - base `/api` path
>     - JSON request/response handling
>     - error parsing
>     - attaching bearer token when present
> - Add API functions for:
>     - `register`
>     - `login`
>     - `refresh`
>     - `listVehicles`
>     - `searchVehicles`
>     - `createVehicle`
>     - `updateVehicle`
>     - `deleteVehicle`
> 
> TDD recommended here.
> 
> Tests:
> 
> - Login sends the correct payload.
> - Vehicle search builds correct query params.
> - Authenticated requests include `Authorization`.

>  Write tests first then stop. After I approve then only implement the code requried to pass the tests.


## Feature - Authentication State:

### Brainstorm with Claude:

> Step 3: Authentication State
> 
> * Add auth state management.
> * Store after login:
>    * `accessToken`
>    * `refreshToken`
>    * logged-in `email`
> * Restore session from `localStorage` on page load.
> * Add logout.
> * Add admin detection using current backend rule:
>    * email username contains `admin`.
> 
> TDD recommended here.
> Tests:
> 
> * Login stores session.
> * Logout clears session.
> * Admin detection works.
> * Refresh token function can update stored access token.
> 
> 
> Since TDD is recommended here, how should I proceed? It should follow industry standard

>  Write a prompt for Codex to implement this. It should write all the test cases first and then stop and after I approve then write its green code.

### Codex Prompt:

> Implement Step 3 (Authentication State). Follow strict TDD with a red-green-refactor workflow, and STOP for my approval after the red phase — do not write implementation code until I explicitly approve the tests. 
> 
> 
> - Auth state via React Context + custom hook (`useAuth`)
>  
> 
> ## Requirements
> Implement authentication state management with:
> 1. `login(accessToken, refreshToken, email)` — stores all three in both React state and localStorage.
> 2. Session restoration from localStorage on initial mount (page refresh should preserve login state).
> 3. `logout()` — clears accessToken, refreshToken, and email from both state and localStorage.
> 4. `isAdmin` — computed flag, true when the email's username (portion before `@`) contains the substring "admin" (case-insensitive).
> 5. `updateAccessToken(newAccessToken)` — updates only the accessToken in state and localStorage, leaving refreshToken and email untouched. This simulates what a token-refresh flow would call after hitting a refresh endpoint.
> 
> ## File layout
> - `src/auth/AuthContext.tsx` — implementation (do not write yet)
> - `src/auth/AuthContext.test.tsx` — tests (write this first)
> 
> ## STEP 1 — RED (do this now, then stop)
> Write `AuthContext.test.tsx` with these five test cases:
> 1. Login stores accessToken, refreshToken, and email in both state and localStorage.
> 2. On mount, an existing localStorage session is restored into state (isAuthenticated becomes true).
> 3. Logout clears accessToken, refreshToken, and email from both state and localStorage.
> 4. isAdmin is true when the email username contains "admin" (test at least one positive case like `super.admin99@company.com` and one negative case like `jane.doe@company.com`).
> 5. updateAccessToken replaces only the accessToken, leaving refreshToken unchanged, in both state and localStorage.
> 
> Use `renderHook` and `act` from `@testing-library/react`. Import `describe`, `it`, `expect`, `beforeEach` from `vitest` (not Jest globals, unless `globals: true` is confirmed in config). Clear localStorage in a `beforeEach`. Do NOT create `AuthContext.tsx` yet — the tests should fail because the module doesn't exist. Show me the test file and confirm the tests fail for the right reason (missing module, not typos/syntax errors). Then STOP and wait for my approval.
> 
> ## STEP 2 — GREEN (only after I say "approved")
> Implement `AuthContext.tsx` with the minimum code needed to make all five tests pass. Use `createContext`, `useState`, `useEffect` for restoration on mount, and `useCallback` for the exposed functions. Guard localStorage access for SSR safety (`typeof window === 'undefined'`). Do not add any behavior the tests don't require.
> 
> ## STEP 3 — REFACTOR (only after tests are green)
> Once tests pass, suggest any cleanup (e.g., extracting localStorage key constants, extracting the admin-check function) without changing behavior or breaking tests. Re-run tests after refactor and confirm they still pass.
> 
> At each phase, wait for my explicit go-ahead before moving to the next one.

>  Approved. Write the implementation code.


## Feature - Login and Registration UI
> 
> Step 4: Login And Registration UI
> 
> * Build combined auth page with tabs:
>    * Login
>    * Register
> * Add fields:
>    * email
>    * password
> * Show validation and API errors.
> * After successful login, show dashboard.
> * After successful registration, either:
>    * show success and switch to login, or
>    * auto-login only if you later add backend support for that.
> 
> Recommended default:
> 
> * Register shows success and asks user to login.
> 
> Tests:
> 
> * Login form calls login API.
> * Register form calls register API.
> * Errors are shown.
> * Successful login enters dashboard.
> 
> 
> Does this require any testcases to write? Since now its about building the UI. Answer according to the industry practices.

### Test Case Prompts (RED):

>  Write all the failing test cases at once. Do not implement any code to fix it. It should fail. After I approve then only write implementation code.

### Implementation Prompts (GREEN):

>  write the minimum implementation to make tests pass, but do not skimp on visual quality — this needs to look and feel like a polished, modern product, not a bare-bones form. 
>  
>  
>  
>  ## Functional requirements (must satisfy the existing tests — do not change test behavior)
>  - Combined AuthPage with Login/Register tabs
>  - Email + password fields on both, client-side validation before API calls
>  - Login calls loginApi, on success calls useAuth().login() and renders Dashboard
>  - Register calls registerApi, on success shows a success message and switches to Login tab
>  - API and validation errors displayed on screen
>  - Switching tabs clears errors but preserves typed form values
>  
>  ## Visual/UX bar — this is the part I care about most
>  Build this like a real hiring-assessment showpiece, not a tutorial form. Specifically:
>  
>  1. **Layout**: Centered auth card on a full-height page, subtle background treatment (soft gradient or muted color, not plain white/gray). Card has generous padding, rounded corners, soft shadow — should feel like a real SaaS login screen, not a bootstrap default.
>  
>  2. **Tabs**: Animated underline or sliding pill indicator under the active tab (CSS transition, no need for a library). Tab click should feel instant and smooth, not jarring.
>  
>  3. **Typography**: Clear hierarchy — a real heading ("Welcome back" for Login, "Create your account" for Register), not just the tab label repeated as a title. Use font-weight and size intentionally, not just default browser text.
>  
>  4. **Inputs**: Floating or top-aligned labels, visible focus states (ring/border color change), and inline validation messages directly under each field in a distinct color — not a single generic error banner for everything unless it's an API-level error (that one can be a banner at the top of the form).
>  
>  5. **Buttons**: Primary submit button with a hover state and a **loading state** (spinner or disabled+dimmed with "Signing in…" / "Creating account…" text) while the API call is in flight — don't let the user double-submit during the request.
>  
>  6. **Error and success states**: Error banners should look like errors (red/rose tones, icon), success message on registration should look distinctly positive (green tones, checkmark icon) — not identical gray boxes.
>  
>  7. **Motion**: Subtle enter transition when switching tabs (e.g. fade/slide, 150-200ms) so it doesn't feel like an instant jarring swap. Keep it tasteful — this is a login form, not a landing page hero.
>  
>  8. **Responsiveness**: Must look correct on mobile widths (~375px) as well as desktop — card should adapt padding/width, not just shrink awkwardly.
>  
>  9. **Accessibility**: Proper `<label>  ` associations for inputs, `aria-live` region for error/success messages so screen readers announce them, visible focus rings (don't strip them for style), and buttons/tabs reachable via keyboard.
>  
>  Use lucide-react for icons if it's available in the project (checkmark for success, alert-circle for errors, loader for the spinner) — otherwise inline SVGs are fine.
>  
>  ## Constraints
>  - Do not modify the test file.
>  - Do not add scope beyond what's needed to pass tests + the visual requirements above (no forgot-password flow, no social login buttons, no remember-me checkbox) unless I ask separately.
>  - After implementation, run the test suite and confirm a tests pass. If any fail, fix the implementation — do not modify tests to make them pass. 
>  
>  Show me the full contents of AuthPage.tsx (and api.ts / Dashboard.tsx stubs if newly created) once tests are green.
>  
>  Make sure you follow industry standard practices, naming conventions and directory structure.

## Debug:

>  I logged in once and then restarted the frontend , it still stays logged in. Why is that so? Before making any changes show me whats the cause.

## Refactor:

>  Is the current frontend directory structure as per industry standard?

>  Refactor the entire directory structure of frontend according to annotation or what fits the industry standards. It should be scalable, easy to maintain and understand the directory. Before making the changes show me how it will look like, only implement after I approve.


## Feature - Vehicle Dashboard

>  What are the important test cases for this feature?
>  ## Step 5: Vehicle Dashboard
>  
>  - Fetch vehicles using `GET /api/vehicles`.
>  - Display vehicles in responsive cards or a clean grid.
>  - Show:
>      - make
>      - model
>      - category
>      - price
>      - quantity in stock
>  - Add states:
>      - loading
>      - empty inventory
>      - error
>  - Add logout button in header.
>  
>  Tests:
>  
>  - Vehicles render after fetch.
>  - Empty state renders for no vehicles.
>  - API failure renders error state.
>  
>  Are test cases needed here?

### Test Cases Prompt (RED)

>  Write all the failing test cases for this. Do not provide any implementation code to pass those test. Follow industry standard practices, naming conventions and directory structure.

### Implementation Prompt (GREEN)

>  Write the minimum implementation to make tests pass, but do not skimp on visual quality — this needs to look and feel like a polished, modern product.
>  
>  ## Functional Requirements
>  
>  1. **Data fetching**
>     - On mount, call `GET /api/vehicles` to retrieve the vehicle list.
>     - Manage three states explicitly: `loading`, `error`, and `success` (with success further branching into `empty` vs `populated`).
>  
>  2. **Vehicle Cards / Grid**
>     - Render each vehicle in a responsive card grid (e.g. 1 column on mobile, 2 on tablet, 3-4 on desktop).
>     - Each card must display:
>       - Make
>       - Model
>       - Category
>       - Price (formatted as currency)
>       - Quantity in stock
>     - Use a clean, modern card design — subtle shadows/borders, good spacing, clear typographic hierarchy (make/model as the primary heading, category as a badge/pill, price emphasized, stock quantity as a small label or badge, e.g. "3 in stock" / "Out of stock" styling if quantity is 0).
>  
>  3. **UI States**
>     - **Loading state**: show skeleton loaders matching the card grid shape (not just a spinner) so the layout doesn't jump when data arrives.
>     - **Empty state**: when the fetch succeeds but returns zero vehicles, show a friendly empty-state illustration/icon, a clear heading (e.g. "No vehicles in inventory"), and short supporting text.
>     - **Error state**: when the fetch fails, show a clear error message and a "Retry" button that re-triggers the fetch. Do not let the error state crash the rest of the page.
>  
>  4. **Header**
>     - Include a logout button in the dashboard header, positioned top-right, clearly distinguishable (e.g. outline/ghost button style, icon + label).
>     - Wire it to whatever existing auth/logout handler or context the app uses (check for an existing `useAuth`/`AuthContext` or logout API call before creating a new one).
>  
>  ## Design & UX Expectations
>  - This should look and feel like a polished, modern SaaS dashboard — not a bare-bones CRUD table.
>  - Use consistent spacing, rounded corners, and a cohesive color palette pulled from the existing Tailwind config/theme (don't invent new arbitrary colors).
>  - Add tasteful micro-interactions: hover states on cards (slight lift/shadow), smooth fade/slide-in when vehicle data loads, smooth transitions between loading → loaded/empty/error states.
>  - Ensure the dashboard is fully responsive and accessible (proper semantic HTML, alt text/aria-labels on icons and buttons, sufficient color contrast, keyboard-focusable interactive elements).
>  
>  ## Constraints
>  - Match the existing project's component structure, naming conventions, and styling approach — inspect existing components/files before creating new patterns.
>  - Keep the implementation in TypeScript with proper typing for the `Vehicle` shape (make, model, category, price, quantity).
>  - Do not over-engineer: implement exactly what's needed to satisfy the described behavior and pass the tests, using clean, readable code.
>  
>  ## Deliverable
>  - The Vehicle Dashboard component(s) implementing the above.
>  - Any supporting types, hooks (e.g. `useVehicles`), or utility functions needed.
>  - Confirm afterward which test cases now pass and flag anything ambiguous in the test expectations that required an assumption.
>  Make sure you follow industry standard practices, naming conventions and directory structure.

## Feature - Vehicle Search 

> ## Step 6: Vehicle Search And Filters
> 
> - Add filter controls:
>     - make
>     - model
>     - category
>     - minimum price
>     - maximum price
> - Add `Search` and `Reset` buttons.
> - Use `GET /api/vehicles/search`.
> - Reset returns to `GET /api/vehicles`.
> - Keep filters responsive and easy to scan.
> 
> TDD recommended for query behavior.
> 
> Tests:
> 
> - Search sends correct query params.
> - Empty fields are omitted from query.
> - Reset clears filters and reloads all vehicles
> Suggest important test cases for this feature.

### Test Cases Prompt (RED)

> Write all the failing test cases. Do not write any implementation code for this feature. Follow industry practices , naming conventions and directory structure. After I approve then only write the implementation code.

### Implementation Prompt (GREEN)

> Write the minimum implementation to make tests pass, but do not skimp on visual quality — this needs to look and feel like a polished, modern product, not a bare-bones form.  
> ## Functional Requirements
> 
> 1. **Filter Controls**
>    Add a filter bar/panel above the Vehicle Dashboard grid with the following inputs:
>    - Make (text input or select, depending on whether an existing enum/list of makes is available in the app — check before deciding)
>    - Model (text input)
>    - Category (select/dropdown, matching existing category values used elsewhere in the app)
>    - Minimum price (numeric input)
>    - Maximum price (numeric input)
> 
> 2. **Search and Reset buttons**
>    - **Search** button triggers a call to `GET /api/vehicles/search` with the current filter values serialized as query params.
>    - **Reset** button clears all filter fields back to empty/default AND re-triggers `GET /api/vehicles` (the unfiltered list), not the search endpoint.
> 
> 3. **Query param behavior (critical — this is what the tests target)**
>    - Only include query params for fields that have a non-empty value. Empty string, `undefined`, or `null` fields must be omitted entirely from the request — do not send empty query params like `?make=&model=`.
>    - Param names should match whatever the backend contract expects (check existing API client/types for the search endpoint — e.g. `make`, `model`, `category`, `minPrice`, `maxPrice` — align naming with what's already defined there rather than inventing new keys).
>    - Numeric fields (min/max price) should only be included if they are valid numbers.
>    - Build the query string construction as a small, pure, testable utility function (e.g. `buildVehicleSearchParams(filters): URLSearchParams` or similar) rather than inlining it in the component, so it's easy to unit test in isolation.
> 
> 4. **State management**
>    - Keep filter state local to the filter component (or a small custom hook, e.g. `useVehicleFilters`), and lift up only the final "search" trigger to whatever is fetching/rendering the vehicle list.
>    - Reuse the existing vehicle-fetching hook/logic from the Vehicle Dashboard where possible rather than duplicating fetch logic — e.g. extend `useVehicles` to accept optional filters, or add a sibling hook that shares the same loading/error/empty state handling patterns already established there.
> 
> ## Design & UX Expectations
> - Filter bar should be visually distinct from the vehicle grid (e.g. a card/panel with subtle background or border) but not visually heavy — this is a utility control, not the star of the page.
> - Lay out the five fields in a responsive row/grid: wrap to multiple rows on smaller screens, single tidy row on desktop. Min/max price can be grouped visually as a compact range pair.
> - Search and Reset buttons should be clearly differentiated (e.g. Search as a primary filled button, Reset as a ghost/outline button), positioned together at the end of the filter row.
> - Add subtle micro-interactions: smooth transition when results update after search, a brief loading indicator on the Search button while the request is in flight (disable the button during that time to prevent double-submits).
> - Maintain accessibility: proper `<label>`s for every input, keyboard support (Enter key in any field triggers Search), visible focus states.
> - Ensure the filter bar collapses gracefully on mobile (e.g. stacked fields, full-width buttons) without feeling cramped.
> 
> ## Constraints
> - Match the existing project's component structure, naming conventions, and styling approach — inspect the Vehicle Dashboard component and API client before creating new patterns.
> - Keep everything in TypeScript with proper typing for the filter shape (make, model, category, minPrice, maxPrice — all optional).
> - Do not over-engineer: implement exactly what's needed to satisfy the described behavior and pass the tests.
> 
> ## Deliverable
> - Filter bar component(s) and any supporting hook(s).
> - The query-param-building utility function, written to be independently unit-testable.
> - Wiring into the Vehicle Dashboard so Search calls `/api/vehicles/search` and Reset calls `/api/vehicles`.
> - Confirm afterward which test cases now pass and flag anything ambiguous in the test expectations that required an assumption.
> Make sure you follow industry standard practices, naming conventions and directory structure.


# Remaining Backend Endpoints:

## Purchase Vehicle Endpoint:

###  Plan prompts:

> Now its time to implement the remaining backend endpoints.
> 
> - Inventory (Protected):
> - ***POST /api/vehicles/:id/purchase: Purchase a vehicle, decreasing its quantity.***
> - ***POST /api/vehicles/:id/restock: Restock a vehicle, increasing its quantity (Admin only).***
> Lets start with the first one. Suggest some test cases for it.
>
### Test case prompts (RED) :
> 
>  Write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### Implementation prompts (GREEN):
> 
>   All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.
> 
## Restock Vehicle Endpoint:

###  Plan prompts:

> Now the last endpoint POST /api/vehicles/:id/restock: Restock a vehicle, increasing its quantity (Admin only).
>
> Suggest important test cases for this
>
### Test case prompts (RED) :
> 
>  Write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.
> 
### Implementation prompts (GREEN):
> 
>   All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.

## Refactor - Admin User

> Right now there is no way to become an admin other than the email contain the literal word "admin" which is not a good practice. Suggest some way through which we can have an admin. Only the admin will be allowed to add, update, delete , restock vehicles. Also this is an assessment project which will be evaluated by evaluators, how will they get the admin access?

### Test case prompts (RED) :

> # Admin Access Plan
> 
> ## Summary
> Replace the current “email contains admin” rule with a real role-based authorization model. Users will have a persisted role, JWTs will carry that role, and vehicle inventory mutations will require `ADMIN`.
> 
> ## Key Changes
> - Add a `role` field to `User`, probably as an enum: `CUSTOMER` and `ADMIN`.
> - Default all normal registrations to `CUSTOMER`.
> - Add admin authority from the stored user role, not from the email address.
> - Include the role in `AuthResponse` so the frontend can show admin-only UI accurately.
> - Update JWT generation/validation to include a `role` claim, or have the auth filter load the user by email and derive authorities from the database. For this project, a JWT `role` claim is simpler and evaluator-friendly.
> 
> ## Vehicle Permissions
> - Require `ROLE_ADMIN` for:
>   - `POST /api/vehicles`
>   - `PUT /api/vehicles/{id}`
>   - `DELETE /api/vehicles/{id}`
>   - `POST /api/vehicles/{id}/restock`
> - Keep these available to authenticated non-admin users:
>   - `GET /api/vehicles`
>   - `GET /api/vehicles/search`
>   - `POST /api/vehicles/{id}/purchase`
> 
> ## Evaluator Admin Access
> Seed one admin account from environment variables at application startup:
> - `ADMIN_EMAIL`
> - `ADMIN_PASSWORD`
> 
> On startup, if `ADMIN_EMAIL` and `ADMIN_PASSWORD` are present and no user with that email exists, create that user with role `ADMIN`.
> 
> For assessment submission, document example credentials in the README or deployment notes, for example:
> - Email: `evaluator.admin@example.com`
> - Password: `Evaluator@12345`
> 
> This gives evaluators a reliable admin login without depending on a bad email-name trick or manual database access.
> 
> ## Test Plan
> - Registration creates `CUSTOMER` users only.
> - Seeded admin user is created when env vars are present.
> - Seeded admin user is not duplicated on restart.
> - Admin JWT/AuthResponse includes `ADMIN`.
> - Customer JWT/AuthResponse includes `CUSTOMER`.
> - Customer receives `403 Forbidden` for add, update, delete, and restock.
> - Admin succeeds for add, update, delete, and restock.
> - Unauthenticated users still receive `401/403` according to existing security behavior.
> 
> ## Assumptions
> - No public “register as admin” endpoint will be added.
> - Evaluators can be given test credentials through README, submission notes, or hosted app instructions.
> - This is acceptable for an assessment project; for production, admin creation should use a private admin console, invitation flow, or managed identity provider.
> 
>  Write 3 types of failing test cases i.e. unit, integration and end to end test cases for the cases u described earlier.  Make sure these test cases cover important edgse cases. Do not write any implementation logic or create controller, service or repository files. Follow strict Test Driven Development. Follow the best indsutry practices and naming conventions.

### Implementation Prompts (GREEN):

> All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Add logs at the start of each method and upon successful execution of that method, also where necessary for debug or error purposes. Also handle the errors in the global exception handler. Make sure to follow best industry practices and naming conventions.


## Feature - Purchase Button UI

### Test Case Prompts (RED):

## Step 7: Purchase Button UI
> 
> - Add `Purchase` button to each vehicle.
> - Disable button when:
> 
> ```jsx
> vehicle.quantityInStock === 0
> ```
> 
> - Since purchase/inventory endpoints are not implemented yet:
>     - clicking enabled purchase button shows a friendly “purchase flow coming soon” message.
>     - no stock mutation happens.
> 
> Tests:
> 
> - Button is disabled for zero stock.
> - Button is enabled for stock above zero.
> - Clicking enabled button shows message.
>   
>  Write all the failing test cases. The test cases should be meaningful. Do not write any implementation code for this feature. Follow industry practices , naming conventions and directory structure. After I approve then only write the implementation code.

### Implementation Prompts (GREEN):

> All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Make sure to follow best industry practices and naming conventions.

> The UI should be consistent with the current UI theme, do not cut slack in the designing the UI. It should feel modern, responsive and provide a great user experience.

### Refactor:

> The endpoints have been implemented for purchase. It decreases the stock by one upon clicking the purchase button. Remove the "puchase flow comming soon" message and implement the decrement of stock by 1. It should not affect any other functionalities. Also update the test cases accordingly.

## Feature - Vehicle Add Form (Admin Only)

### Test Case Prompts (RED):

> ## Step 8: Admin Vehicle Add Form
>
> - Show admin panel only for admin users.
> - Add vehicle form fields:
>     - make
>     - model
>     - category
>     - price
>     - quantity in stock
> - Submit to `POST /api/vehicles`.
> - Refresh vehicle list after success.
> - Show validation/API errors.
> 
> Tests:
> 
> - Non-admin users do not see form.
> - Admin users see form.
> - Submit sends correct payload.
> - Success refreshes list.
> Write all the failing test cases. The test cases should be meaningful. Do not write any implementation code for this feature. Follow industry practices , naming conventions and directory structure. After I approve then only write the implementation code.

### Implementation Prompts (GREEN):

> All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Make sure to follow best industry practices and naming conventions.

> The UI should be consistent with the current UI theme, do not cut slack in the designing the UI. It should feel modern, responsive and provide a great user experience.


## Feature - Vehicle Update (Admin Only)

### Test Case Prompts (RED):
> 
> ## Step 9: Admin Vehicle Update UI
> 
> - Add edit action on each vehicle for admin users.
> - Use inline edit form or modal.
> - Reuse the same fields as create.
> - Submit to `PUT /api/vehicles/{id}`.
> - Refresh vehicle list after success.
> - Cancel returns to normal display.
> 
> Tests:
> 
> - Edit form opens with existing values.
> - Save sends correct payload.
> - Cancel does not call API.
> - Success updates displayed list.
>  Write all the failing test cases. The test cases should be meaningful. Do not write any implementation code for this feature. Follow industry practices , naming conventions and directory structure. After I approve then only write the implementation code.

### Implementation Prompts (GREEN):

> All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Make sure to follow best industry practices and naming conventions.

> The UI should be consistent with the current UI theme, do not cut slack in the designing the UI. It should feel modern, responsive and provide a great user experience.

### Brainstorm:

> Right now, only the vehicles in stock are visible to customer and admin, is that fine? I think user should also be able to see out of stock cars

## Feature - Vehicle Delete (Admin Only)

### Test Case Prompts (RED):
> 
> ## Step 10: Admin Vehicle Delete UI
> 
> - Add delete action for admin users.
> - Ask for confirmation before deleting.
> - Call `DELETE /api/vehicles/{id}`.
> - Remove from UI or refresh list after success.
> - Show error if delete fails.
> 
> Tests:
> 
> - Delete is hidden for non-admin users.
> - Confirmation is required.
> - Confirm calls delete API.
> - Success removes vehicle from dashboard.
> 
> Write all the failing test cases. The test cases should be meaningful. Do not write any implementation code for this feature. Follow industry practices , naming conventions and directory structure. After I approve then only write the implementation code.

### Implementation Prompts (GREEN):

> All test cases are currently failing (red). Write the minimum production code needed to make them pass — nothing more. Make sure to follow best industry practices and naming conventions.

> The UI should be consistent with the current UI theme, do not cut slack in the designing the UI. It should feel modern, responsive and provide a great user experience.

### Fixes:

> Right the on the top left corner, it says dealership adim for both admin and customer role users. Change it such that is only shows dealership admin for admin only. suggest an alternative for customers before implementing.

> Implement Find Your Car. This should not be text change in UI , it should not affect bussiness logic nor tests

> Add the following categories for vehicles, supercars and sports cars. Rename "Sedan cars" to "Sedans".

> The price is shown in dollars, it should be rupees, before implementing show me where the changes will be required.

> Implement the changes.

> The login page says sign in to manage inventory , listings and dealership workflows. Suggest some different lines as now it will be for customers.

> Implement "Sign in to browse cars and purchase with ease".


# README.md

> A comprehensive README.md file that includes: 
> - A clear explanation of the project.
> - Detailed instructions on how to set up and run the project locally (both backend and frontend).
> - The mandatory "My AI Usage" section.
> Don't implement the readme, show me its content.
> 
> Add deployment information:
> 
> The backend is deployed on render via dockerfile
> The frontend is hosted on vercel
> The mysql db is hosted on aiven (aiven.io)
> 
> Include the website links for these platforms. Do not change anything else in the readme.