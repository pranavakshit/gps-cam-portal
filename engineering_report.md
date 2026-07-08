# 1. Executive Summary

**Live Deployment URL:** [https://gps.pranavakshit.in](https://gps.pranavakshit.in)

* **What is the project?** GPS Cam Portal is an end-to-end photo capturing and synchronization system. It combines a mobile app for field data collection with a robust web portal for administrative management and data visualization. 
* **Why was it built?** To ensure that field operations (such as surveys, audits, or asset tracking) can reliably collect, verify, and store geo-tagged photographic evidence.
* **What problem does it solve?** Field agents often operate in remote areas with spotty network coverage. The portal provides robust offline capabilities on the mobile device while ensuring that uploaded data is strictly mapped to the official Local Government Directory (LGD) spatial hierarchies.
* **Who is the intended user?** Field workers/agents (using the Android App) and System Administrators/Managers (using the Web Portal) managing field operations.

---

# 2. Current Status

**Active Development / Functional Prototype**

The core functionality across all tiers (mobile app, backend, web portal) is operational. 
* **What currently works:** The mobile app captures and uploads photos. Authentication, 2FA, Role-Based Access Control (RBAC), LGD location syncing, spatial mapping, and Docker-based deployment are fully functioning. A robust photo soft-deletion workflow is also active.
* **What is incomplete:** Granular reporting analytics, mapping layers (e.g., viewing photos directly on an interactive map UI), and automated scaling policies (e.g., moving from single-instance Docker to Kubernetes).

---

# 3. Features

## Implemented
* JWT-based Authentication
* Role-Based Access Control (ADMIN, USER, VISITOR)
* 2-Factor Authentication (TOTP via Authenticator Apps)
* Mobile Photo Uploads (with lat/lon and timestamp metadata)
* LGD Database Ingestion (Parsing and syncing official State/District/Village hierarchies from ZIP files)
* Offline Location Bundle (Android app caches location hierarchies for offline tagging)
* Photo Soft-Delete Workflow (User requests -> Admin approves -> Soft delete -> Hard delete)
* User Management Interface (CRUD for users, password resets)
* Docker Management Interface (View container status, logs, start/restart from the web portal)

## Partially Implemented
* Advanced Gallery filtering (Basic viewing is implemented, but advanced geospatial filtering is rudimentary).

## Planned
* Geospatial Map Integration (Plotting photos on Leaflet/Mapbox).
* Data Export/Reporting (Exporting tagged photos to CSV/PDF reports).

---

# 4. System Architecture

The architecture follows a classic 3-tier model with dedicated mobile client and web administration clients.

* **Client (Mobile)**: Native Android App (Kotlin) for field data collection.
* **Client (Web)**: React SPA (Single Page Application) built with Vite for administration.
* **Backend API**: Node.js/Express REST API serving both clients.
* **Database**: MySQL relational database holding users, locations, and photo metadata.
* **Storage**: Local filesystem storage (within Docker volumes) for uploaded photo assets.
* **Deployment**: Docker Compose orchestrating the Database, Backend, and Frontend containers on an Ubuntu VM.
* **Networking**: Nginx proxy manager handling SSL termination and reverse proxying to Docker containers on `gps.pranavakshit.in`.

---

# 5. Technology Stack

* **Languages**: TypeScript, Kotlin, SQL
* **Frameworks**: Express.js (Backend), React (Frontend), Android SDK (Mobile)
* **Libraries**: Prisma ORM, Axios, Lucide-React, otplib (for 2FA)
* **Databases**: MySQL
* **DevOps & Deployment**: Docker, Docker Compose, Nginx Proxy Manager, Ubuntu Server
* **Build Tools**: Vite (Frontend), Gradle (Android), `tsc` (Backend)

---

# 6. Data Flow

**Example: Field Photo Capture & Sync**
1. User logs into the Android App (receives JWT).
2. User captures a photo. The app embeds the current GPS coordinates and timestamp.
3. User selects the current Location (from the cached offline LGD bundle).
4. App sends a `multipart/form-data` POST request to `/api/photos/upload`.
5. Express backend intercepts the request using `multer` middleware, saving the binary payload to the local file system.
6. The controller parses the metadata, creates a Prisma record associating the image URL, location, and user ID.
7. Backend returns a success response.
8. Admin logs into the Web Portal and views the newly synced photo in the Gallery tab.

---

# 7. Important Engineering Decisions

* **Prisma ORM over Raw SQL**: Chosen for strong TypeScript integration and automated schema migrations. It prevents runtime errors and accelerates backend development compared to raw MySQL queries.
* **Local Storage over S3 (Initial Phase)**: To keep deployment simple and cost-effective on a single VM, local Docker volume storage was chosen. This can be easily migrated to AWS S3 by swapping the Multer storage engine in the future.
* **Soft-Deletion Workflow**: Instead of allowing users to hard-delete photos, a state machine (`NONE`, `REQUESTED`, `APPROVED`) was implemented. This prevents accidental data loss from field agents while maintaining an audit trail.

---

# 8. Algorithms

* **LGD Data Ingestion Pipeline**: The system ingests official Local Government Directory ZIP/Excel data. The algorithm involves streaming the large file, unzipping in memory, parsing the CSV/XLSX structures, and executing bulk upserts (State -> District -> Subdistrict -> Village/Ward) while maintaining relational integrity.
* **Authentication & RBAC**: The system uses JSON Web Tokens (JWT) for stateless authentication. Middlewares (`requireAdmin`, `requireAdminOrVisitor`, `forbidVisitor`) intercept requests, verify the JWT signature, and compare the embedded `role` claim against the endpoint's required authorization level.

---

# 9. Database Design

* **Database Type**: Relational (MySQL)
* **Important Tables**:
  * `User`: Stores credentials, role, and 2FA secrets.
  * `Photo`: Stores metadata, hierarchy references (`stateCode`, `districtCode`, etc.), and deletion states.
  * `LgdState`, `LgdDistrict`, `LgdSubDistrict`, `LgdVillage`, `LgdUlb`, `LgdWard`: Normalized tables mirroring the Indian government's spatial hierarchy.
* **Relationships**: Strict Foreign Keys with Cascade Deletes for the LGD spatial hierarchy (e.g., `LgdDistrict` references `LgdState`).
* **Indexes**: Indexes are placed on `name` and `lgdCode` across all spatial tables to optimize text searches in the Web Portal's location explorer.

---

# 10. APIs

The backend exposes a RESTful API. All endpoints (except login) require an `Authorization: Bearer <token>` header.

* **Authentication**: `/api/auth/login`, `/api/auth/verify-2fa`, `/api/auth/setup-2fa`
* **Users**: CRUD operations at `/api/users/`. Strict `ADMIN` enforcement for mutating endpoints.
* **Photos**: `/api/photos/upload` (accepts multipart forms). Deletion endpoints (`/request-delete`, `/approve-delete`, `/hard`) alter the `deletionStatus` field.
* **Locations**: Read endpoints (`/states`, `/districts/:id/subdistricts`) are open to authenticated users for the Android app. Mutating endpoints (`/import`, `/:id`) are strictly for `ADMIN`.
* **Docker**: `/api/docker/containers` (Returns daemon stats). Allows admins to remotely control the host's Docker engine via the UI.

---

# 11. Deployment

* **Infrastructure**: Oracle Cloud Ubuntu VM (141.148.207.232).
* **Containers**: Three primary containers orchestrated via `docker-compose.yml`:
  * `db`: MySQL 8 instance.
  * `backend`: Node.js API.
  * `web-portal`: Nginx alpine serving static Vite build.
* **Networking**: External traffic hits Nginx Proxy Manager, which handles Let's Encrypt SSL and proxies to the `web-portal` container (port 80) and `backend` container (port 5000).
* **Storage Limits**: Docker containers enforce log rotation (`max-size: "10m"`) to prevent partition saturation on the host VM.

---

# 12. Security

* **Authentication**: Passwords are mathematically hashed using `bcrypt` before storage. Sessions are stateless and cryptographically signed via JWT.
* **2-Factor Authentication**: TOTP (Time-based One-Time Password) is supported. Secrets are stored in the database and verified using `otplib`.
* **Authorization**: Granular RBAC explicitly denies the `VISITOR` role access to any `POST/PUT/DELETE` methods system-wide.
* **Container Security**: The backend interacts with the host Docker socket (`/var/run/docker.sock`) to provide dashboard stats. This is a known risk vector heavily protected by application-layer admin authentication.

---

# 13. Performance

* **LGD Query Speed**: Utilizing B-Tree indexes on `lgdCode` and `name`, the location drill-down UI responds in < 100ms.
* **Image Delivery**: Static assets and uploaded images are served directly by Nginx (bypassing Node.js overhead) ensuring rapid gallery load times.
* *Note: Load testing has not yet been aggressively benchmarked against thousands of concurrent Android app users.*

---

# 14. Scalability

* **Current Limitations**: The system relies on local filesystem storage (`/uploads`). If the application requires scaling horizontally (multiple backend containers), this local storage will become a split-brain bottleneck.
* **Future Scaling Strategy**: 
  1. Migrate image storage to an S3-compatible object store.
  2. Decouple MySQL to a managed database service (e.g., AWS RDS).
  3. Deploy backend containers behind a Load Balancer.

---

# 15. Challenges Faced

* **Challenge: Device Offline Environments**
  * *Solution*: Designed an `/offline-bundle` endpoint that zips and downloads the necessary state-specific LGD hierarchy to the Android device, allowing SQLite queries on the device without network access.
* **Challenge: VM Disk Saturation**
  * *Solution*: The VM's partition began filling rapidly due to runaway Docker daemon logs. Solved by implementing strict logging drivers (`max-size: "10m"`, `max-file: "3"`) in `docker-compose.yml` and executing automated docker pruning.
* **Challenge: Complex Photo Deletion**
  * *Solution*: Because field data is critical, giving field agents immediate delete privileges was risky. Implemented a state machine (`NONE` -> `REQUESTED` -> `APPROVED` -> `HARD_DELETE`) to ensure administrative oversight.

---

# 16. Lessons Learned

* **System Design**: Tightly coupling file storage to the API server simplifies MVP deployment but severely limits horizontal scaling.
* **Deployment**: Explicitly defining Docker logging constraints from day one is critical for long-running infrastructure stability.
* **Architecture**: Implementing RBAC (Role-Based Access Control) using centralized Express middlewares (e.g., `forbidVisitor`) is significantly more maintainable than placing logic checks inside individual route handlers.

---

# 17. Resume Highlights

* Architected and deployed an end-to-end field data collection system utilizing Node.js, React, and Kotlin.
* Engineered a robust offline spatial tagging system by serializing Local Government Directory hierarchies for mobile caching.
* Developed a centralized, role-based access control (RBAC) middleware system securing REST endpoints against unauthorized mutations.
* Managed infrastructure on a cloud VM using Docker Compose, optimizing storage layers to prevent log-induced disk saturation.
* Implemented multi-stage, state-machine driven data deletion workflows to prevent accidental data loss.

---

# 18. Portfolio Highlights

* **Architecture Diagram**: Visualizing the flow between the Android App, Vite React Frontend, Express Backend, MySQL Database, and Docker Daemon.
* **Demo/Screenshots**: 
  * The LGD Locations Explorer UI.
  * The System Management (Docker) control panel.
  * The Photo Soft-Deletion workflow.
* **Database Schema**: A visual ERD generated from the Prisma schema highlighting the complex spatial hierarchies (State -> District -> Subdistrict -> Village).

---

# 19. Future Roadmap

* **Planned**: Migrate local photo storage to AWS S3 or MinIO for horizontal scalability.
* **Planned**: Implement map-based visualizations (Leaflet/Mapbox) to plot photo coordinates natively in the web portal.
* **Planned**: Export functionality to generate PDF and CSV audit reports of captured data.

---

# 20. Interview Questions

1. Can you walk me through the lifecycle of a photo upload request from the Android app to the database?
2. Why did you choose Prisma as your ORM over writing raw SQL or using Sequelize?
3. How does the Local Government Directory (LGD) ingestion algorithm handle large datasets without blocking the Node event loop?
4. How is the 2-Factor Authentication mathematically implemented in this system?
5. How did you design the database schema to handle the nested relationships of States, Districts, and Villages?
6. Explain the soft-delete state machine for photos. Why was it designed this way?
7. Your system exposes host Docker controls to the web portal. How did you mitigate the security risks associated with mounting the docker socket?
8. If we needed to scale the backend to 5 instances tomorrow, what would break in your current architecture?
9. How did you diagnose and solve the VM disk saturation issue?
10. Describe how the Role-Based Access Control is enforced at the API layer.
11. How does the `/offline-bundle` endpoint work, and why was it necessary?
12. Why did you use B-Tree indexes on the spatial hierarchy tables?
13. Explain how Nginx Proxy Manager routes traffic to your Docker containers.
14. What are the advantages of using Vite over Create React App or Webpack for the frontend?
15. How do you handle database migrations when deploying new features to the VM?
16. Walk me through the security mechanisms protecting the API against unauthenticated access.
17. How does the frontend conditionally render administrative UI elements without compromising backend security?
18. What would you do differently if you had to rebuild the file storage component?
19. How did you structure the Express routing layer to keep the codebase maintainable?
20. In the context of the Android app, what challenges did you face when syncing high-resolution images over unstable networks?
