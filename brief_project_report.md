# Project Report: GPS Cam Portal

**Live Deployment URL:** [https://gps.pranavakshit.in](https://gps.pranavakshit.in)

## 1. Project Objective
**GPS Cam Portal** is an end-to-end field data collection and synchronization system. The primary goal of the project is to allow field agents operating in remote environments (often with unstable network connectivity) to capture photographic evidence securely tagged with precise GPS coordinates and official Local Government Directory (LGD) spatial hierarchies.

## 2. System Architecture
The system employs a classic three-tier architecture, fully containerized for deployment:
* **Mobile Client (Frontend):** A Native Android application written in Kotlin. It utilizes an offline-first design, caching massive LGD datasets on-device using SQLite to allow seamless field operation without internet access.
* **Administrative Portal (Frontend):** A responsive Single Page Application (SPA) built with React and Vite. It serves as the central hub for administrators to view synced data, manage users, and monitor system health.
* **Backend API & Database:** A RESTful Node.js (Express) API powered by Prisma ORM, interfacing with a MySQL 8 relational database. The backend handles complex data ingestion, user authentication, and multi-stage photo deletion workflows.

## 3. Core Features Implemented
* **Robust Offline Synchronization:** Photos taken offline are securely queued and synced to the central server via `multipart/form-data` uploads once a connection is re-established.
* **Role-Based Access Control (RBAC):** Strict JWT-based authentication enforcing three distinct roles (`ADMIN`, `USER`, and a read-only `VISITOR`).
* **Two-Factor Authentication (2FA):** Enhanced security for administrative accounts using TOTP (Time-Based One-Time Passwords) compatible with standard authenticator apps.
* **Data Integrity Workflows:** A state-machine driven "Soft Delete" protocol ensures field agents cannot permanently wipe critical evidence without explicit administrative approval.
* **Official Data Ingestion:** Algorithms designed to dynamically parse and ingest official Indian LGD (Local Government Directory) ZIP/Excel files into normalized database relations.

## 4. Technologies & Infrastructure
* **Languages:** Kotlin, TypeScript, SQL
* **Frameworks/Libraries:** Express.js, React, Prisma, Axios
* **Infrastructure:** The entire suite (Backend, Frontend, Database) is orchestrated via **Docker Compose** on an Ubuntu Virtual Machine. It utilizes Nginx Proxy Manager for reverse proxying and Let's Encrypt SSL termination, ensuring secure `HTTPS` data transmission.

## 5. Conclusion
The GPS Cam Portal successfully demonstrates the practical application of full-stack software engineering principles. It solves real-world operational challenges by combining resilient native mobile capabilities with a scalable, secure, and easily deployable web infrastructure.
