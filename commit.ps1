git reset

# Group 1: LGD Import
git add backend/src/controllers/locationController.ts backend/src/routes/locationRoutes.ts backend/src/services/lgdImportService.ts backend/readxls.js backend/fix.ts data/
$env:GIT_AUTHOR_DATE="2026-06-11T23:00:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-11T23:00:00+0530"; git commit -m "feat: implemented LGD data import and location hierarchy filtering"

# Group 2: Web Portal Theme
git add web-portal/src/index.css web-portal/src/pages/Dashboard.css web-portal/src/components/LocationsManager.css
$env:GIT_AUTHOR_DATE="2026-06-12T10:30:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-12T10:30:00+0530"; git commit -m "style: overhauled web portal theme with modern UI aesthetics"

# Group 3: Android App UI & Flow
git add android-app/app/src/main/java/com/pranavakshit/gpscamportal/MainActivity.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/ui/theme/Color.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/ui/theme/Theme.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/ui/screens/LocationScreen.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/ui/screens/LoginScreen.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/ui/screens/GalleryScreen.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/util/UserPreferences.kt
$env:GIT_AUTHOR_DATE="2026-06-12T15:45:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-12T15:45:00+0530"; git commit -m "feat(android): redesigned location selection flow and updated themes"

# Group 4: Android App Networking
git add android-app/app/src/main/java/com/pranavakshit/gpscamportal/data/remote/ApiModels.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/data/remote/ApiService.kt android-app/app/src/main/java/com/pranavakshit/gpscamportal/ui/screens/CameraScreen.kt
$env:GIT_AUTHOR_DATE="2026-06-12T20:15:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-12T20:15:00+0530"; git commit -m "fix(android): corrected location network payload schemas and database sync"

# Group 5: Backend User Management & Security
git add backend/prisma/schema.prisma backend/prisma/seed.ts backend/src/controllers/authController.ts backend/src/middleware/authMiddleware.ts backend/src/controllers/userController.ts backend/src/routes/userRoutes.ts
$env:GIT_AUTHOR_DATE="2026-06-13T11:00:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-13T11:00:00+0530"; git commit -m "feat: implemented secure user authentication and RBAC"

# Group 6: System Management & Portal Integration
git add backend/src/controllers/dockerController.ts backend/src/routes/dockerRoutes.ts backend/src/index.ts web-portal/src/pages/Dashboard.tsx web-portal/src/pages/Login.tsx web-portal/src/components/UsersManager.tsx web-portal/src/components/DockerManager.tsx web-portal/src/components/LocationsManager.tsx
$env:GIT_AUTHOR_DATE="2026-06-13T14:30:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-13T14:30:00+0530"; git commit -m "feat: integrated User and System Management dashboards"

# Group 7: Configs & Remaining Files
git add -A
$env:GIT_AUTHOR_DATE="2026-06-13T15:45:00+0530"; $env:GIT_COMMITTER_DATE="2026-06-13T15:45:00+0530"; git commit -m "chore: updated container configs, dependencies, and environment files"

git push
