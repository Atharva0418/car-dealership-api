# Car Dealership API

A full-stack car dealership inventory application with a Spring Boot backend and a React frontend.

The project supports user registration, login, JWT-based authentication, role-based authorization, and vehicle inventory workflows. Customers can browse/search vehicles and purchase available stock, while admins can create, update, delete, and restock vehicles.

## Live Project

The project is live at: [https://car-dealership-api.vercel.app/](https://car-dealership-api.vercel.app/)

### Admin Login

```text
Email: admin@dealership.com
Password: admin123
```

## Tech Stack

### Backend

- Java 21
- Spring Boot 4.1
- JWT authentication
- MySQL

### Frontend

- React 18
- TypeScript
- Vite
- Tailwind CSS
- Vitest

### Local Infrastructure

- Docker Compose
- MySQL 8.0

## Prerequisites

Install the following before running the project locally:

- Install java 21 from [Oracle official website](https://www.oracle.com/in/java/technologies/downloads/#jdk21-windows). Download according to your OS. It is required to compile and run the project.


- Install Gradle from [Gradle official website](https://gradle.org/install/#manually). Download the files according to your OS. It is used for compiling, testing and packaging applications.


- Install Node.js and npm from [Node.js official website](https://nodejs.org/). Download according to your OS. npm is included with Node.js. It is required to install dependencies and run the frontend project.


- Install Docker Desktop from [Docker official website](https://www.docker.com/products/docker-desktop/). Download according to your OS. It is required to run the local MySQL database with Docker Compose.


- Clone the git repository from the Github to your local machine. You can do this by

  ```
  git clone https://github.com/Atharva0418/car-dealership-api.git
  ```

## Environment Variables for Backend

Before you run backend:

Create a `.env` file in the project root.

```properties
DB_URL=jdbc:mysql://localhost:3307/dealership_db
DB_USER=dealership_user
DB_PASSWORD=dealership_pass

JWT_SECRET=replace-this-with-a-long-secure-secret
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600
JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=604800

ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-password

FRONTEND_ORIGINS=http://localhost:5173
```

`ADMIN_EMAIL` and `ADMIN_PASSWORD` are optional, but when both are provided the backend seeds an admin user automatically on startup. You can use these credentials to log in as admin.

## Run The Backend Locally

Start MySQL:

```powershell
docker compose up
```

Run the Spring Boot backend:

```powershell
.\gradlew bootRun
```

The backend runs on:

```text
http://localhost:3000
```

## Environment Variables for Frontend

Before you run frontend:

Create a `.env` file in the /frontend directory.

```properties
VITE_API_PROXY_TARGET=http://localhost:3000
```

## Run The Frontend Locally

Open a second terminal and move into the frontend directory:

```powershell
cd frontend
```

Install dependencies:

```powershell
npm install
```

Start the Vite dev server:

```powershell
npm run dev
```

The frontend runs on:

```text
http://localhost:5173
```

During local development, Vite proxies `/api` requests to:

```text
http://localhost:3000
```

You can override this with:

```properties
VITE_API_PROXY_TARGET=http://localhost:3000
```



## API Overview



### Authentication


| Method | Endpoint             | Description                                  |
| ------ | -------------------- | -------------------------------------------- |
| `POST` | `/api/auth/register` | Register a customer user                     |
| `POST` | `/api/auth/login`    | Log in and receive access/refresh tokens     |
| `POST` | `/api/auth/refresh`  | Exchange a refresh token for new auth tokens |


Authenticated requests should include:

```http
Authorization: Bearer <access-token>
```



### Vehicles


| Method   | Endpoint                      | Role          | Description                                               |
| -------- | ----------------------------- | ------------- | --------------------------------------------------------- |
| `GET`    | `/api/vehicles`               | Authenticated | List vehicles                                             |
| `GET`    | `/api/vehicles/search`        | Authenticated | Search vehicles by make, model, category, and price range |
| `POST`   | `/api/vehicles`               | Admin         | Create a vehicle                                          |
| `PUT`    | `/api/vehicles/{id}`          | Admin         | Update a vehicle                                          |
| `DELETE` | `/api/vehicles/{id}`          | Admin         | Delete a vehicle                                          |
| `POST`   | `/api/vehicles/{id}/restock`  | Admin         | Add stock to a vehicle                                    |
| `POST`   | `/api/vehicles/{id}/purchase` | Customer      | Purchase one unit of a vehicle                            |




## Testing

Run backend tests from the project root:

```powershell
.\gradlew  test
```

Run frontend tests from the `frontend` directory:

```powershell
npm test
```



## Docker

The project includes a `Dockerfile` for building the backend application image. This is for deployment purposes only.

Build the backend image:

```powershell
docker build -t car-dealership-api .
```



## Deployment

- Backend: deployed on Render using the project `Dockerfile` ([render.com](https://render.com)).
- Frontend: hosted on Vercel ([vercel.com](https://vercel.com)).
- MySQL database: hosted on Aiven ([aiven.io](https://aiven.io)).



## My AI Usage

I used AI assistance to help draft and organize this README, especially the setup steps, project explanation, and documentation structure. I reviewed the generated content against the actual project files and remain responsible for the final documentation.
