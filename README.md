# GPS Cam Portal

# GPS Cam Portal

This project is a two-part system: an offline-first Android app for capturing GPS-stamped photos, and a web portal (React + Node.js + MySQL) to view and manage them.

## Core Features

- **Android App:** Takes photos offline, embedding GPS coordinates and hierarchical location data (from the Local Government Directory) into a local SQLite database. Syncs to the backend when online.
- **Location Hierarchy:** Pick your State, District, Sub-District, Block, and Village directly in the app.
- **Web Portal:** A React dashboard for viewing uploads, protected by JWT authentication and Role-Based Access Control (RBAC).
- **Admin Tools:** Admins can manage users and monitor/restart the underlying Docker containers via a mapped Docker Socket.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop) (for running the database and web services)
- [Node.js 18+](https://nodejs.org/) (if running manually without Docker)
- [Android Studio](https://developer.android.com/studio) (for building the Android app)

---

## 🚀 Starting the Web Portal & Server

The entire web stack (MySQL Database, Express Backend, and React Frontend) has been fully Dockerized for development. It includes live hot-reloading for rapid prototyping.

Simply run the unified startup script for your platform from the root directory:

**Windows:**
```powershell
# Using PowerShell
.\start.ps1

# Or using Command Prompt
start.bat
```

**Linux / macOS:**
```bash
./start.sh
```

These scripts will automatically:
1. Spin up the MySQL, Backend, and Frontend containers.
2. Run database migrations and automatically seed the database with LGD data and the default admin account.
3. Print the local URLs to access the portal.
4. Tail the live logs until you press **`q`** to gracefully shut everything down.

### Accessing the Portal
Once running, open your browser to:
- **Web Portal:** [http://localhost:5173](http://localhost:5173)
- **Backend API:** [http://localhost:5000](http://localhost:5000)

**Default Secure Login:**
- Username: `admin`
- Password: `admin`
*(Note: These credentials can be overridden using the `ADMIN_USERNAME` and `ADMIN_PASSWORD` environment variables in `docker-compose.yml` for production deployments).*

---

## 📱 Setting up the Android App

The Android app captures photos with embedded locations and securely syncs them to the backend when a network connection is available.

1. **Install via APK:**
   You can download the pre-compiled debug APK from the GitHub releases page, or build it manually below.

2. **Open the Project:**
   Open the `android-app` directory in **Android Studio**.

3. **Configure Backend Network (If testing on a physical device):**
   By default, the app is configured to communicate with the Android Emulator's loopback address (`http://10.0.2.2:5000`). 
   If you are installing the app on a **real physical device** over local Wi-Fi, update the `BASE_URL` in `android-app/app/src/main/java/com/pranavakshit/gpscamportal/data/remote/ApiService.kt` to point to your computer's local Wi-Fi IPv4 address (e.g., `http://192.168.x.x:5000`). For production, point this to your deployed domain name (e.g., `https://api.yourdomain.com/`).

4. **Build and Run:**
   Connect your device or start an emulator, and click the **Run** button (Shift + F10) in Android Studio.

---

## Production Deployment

A full Oracle Cloud Virtual Machine deployment walkthrough is provided. See the internal docs for instructions on connecting your domains, configuring Nginx for dual-subdomain routing, injecting existing SSL certificates, and spinning up the Docker stack in an isolated, secure server environment.

---

## License

This software is licensed under the **PolyForm Noncommercial License 1.0.0**. 

You may not use this software for any commercial purposes (e.g., selling it, providing commercial services, or incorporating it into a commercial product). Please see the `LICENSE` file for full details and liability disclaimers.