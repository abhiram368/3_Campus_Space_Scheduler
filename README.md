# Campus Space Scheduler

Campus Space Scheduler is a cross-platform, mobile-first campus space scheduling system developed using Java (Android), XML (Android Layouts) and Firebase designed to manage and schedule the booking of academic spaces such as classrooms, laboratories, and halls within a campus. It helps avoid scheduling conflicts, improves space utilization, and provides a centralized platform for students, faculty, and administrators.

## Problem Statement
Manual or fragmented scheduling of campus spaces often leads to conflicts, inefficient utilization, and lack of transparency. This system aims to provide a structured and automated solution for managing campus space bookings.

## Features
- Booking of classrooms, labs, and halls
- Conflict-free scheduling
- Role-based access (Admin / Faculty / Student)
- View availability of campus spaces
- Manage bookings and schedules centrally

# Tech Stack:
### Frontend (Client Application)
- Java (Android) – Core application logic, activities, adapters, scheduling logic.
- XML (Android Layouts) – UI layout design for screens such as login, profile, schedule view, and admin panels.
- RecyclerView / Custom Views – Display weekly schedules and lists of bookings.
### Backend / Cloud Services
- Firebase Authentication – User login and identity management.
- Firebase Realtime Database – Stores users, roles, bookings, and schedule data.
- Firebase Cloud Functions (Node.js) – Admin operations such as creating users or enforcing backend logic.
### Data & Access Control
- Firebase Security Rules – Role-based access control (e.g., admin vs normal user).
- JSON Data Structure – Data stored in hierarchical format in Realtime Database.
### Development Tools
- Android Studio – Main IDE for development.
- Git + GitHub – Version control and collaboration.
- Firebase CLI – Deploy cloud functions and manage backend.
### Optional Supporting Libraries
- Firebase SDK for Android
- Material Components for Android – UI components and theming.
### System Architecture
- Client–Cloud architecture
- Android app communicates directly with Firebase services through SDK.
- Authentication controls access.
- Cloud Functions handle privileged operations.
## Project Structure
```
campus-space-scheduler/
├── docs/
├── src/
├── README.md
├── CONTRIBUTING.md
└── .gitignore
```

## Branching Rules
- Do NOT work directly on the `main` branch.
- Create a new branch for each task or feature.

Branch naming convention:
- `feature/<feature-name>`
- `fix/<bug-name>`
- `docs/<documentation-name>`

Example:

## Workflow
1. Create a new branch from `main`.
2. Make changes and commit regularly with clear messages.
3. Push your branch to GitHub.
4. Create a Pull Request to `main`.
5. Wait for at least one review before merging.

## Commit Message Format
Use clear and meaningful commit messages:

## Code Standards
- Keep code clean and readable.
- Comment important logic.
- Do not commit unused or broken code.

## Reporting Issues
- Use GitHub Issues to report bugs or suggest improvements.
- Provide clear steps to reproduce issues when possible.

## Academic Integrity
This project is part of a college assignment. All contributions must be original and properly credited within the team.
