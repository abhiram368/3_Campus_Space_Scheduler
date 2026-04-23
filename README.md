# Campus Space Scheduler

![Platform](https://img.shields.io/badge/Platform-Android-green)
![Backend](https://img.shields.io/badge/Backend-Firebase-orange)
![Language](https://img.shields.io/badge/Language-Java-blue)
![Status](https://img.shields.io/badge/Status-Active-success)

Campus Space Scheduler is a mobile-first Android application designed to manage and schedule campus spaces such as classrooms, laboratories, and halls. It provides a centralized and conflict-free booking system with role-based access and approval workflows.

---

## Problem Statement

Manual scheduling systems often lead to:

* Booking conflicts
* Inefficient resource usage
* Lack of transparency

This application solves these issues through automation and centralized control.

---

## Features

* Classroom, Lab, and Hall booking
* Conflict-free scheduling
* Multi-level approval workflow
* Role-based access control
* Real-time availability tracking
* Booking history and notifications

---

## Tech Stack

### Frontend

* Java (Android)
* XML (Layouts)
* RecyclerView

### Backend

* Firebase Authentication
* Firebase Realtime Database
* Firebase Cloud Functions

### Tools

* Android Studio
* GitHub
* Firebase CLI

---

## System Architecture

* Client–Cloud Architecture
* Android app communicates directly with Firebase
* Role-based authentication controls access
* Cloud Functions handle secure operations

---

## Project Structure

```
3_Campus_Space_Scheduler/
│
├── app/
│   ├── src/main/
│   │   ├── java/...        # Activities, adapters, models
│   │   ├── res/            # Layouts, drawables, values
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   └── values/
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Installation & Setup

### Prerequisites

* Android Studio installed
* Firebase account
* Android device or emulator

### Steps

1. Clone the repository:

   ```
   git clone https://github.com/abhiram368/3_Campus_Space_Scheduler.git
   ```

2. Open project in Android Studio

3. Connect Firebase:

   * Go to Firebase Console
   * Create a project
   * Add Android app
   * Download `google-services.json`
   * Place it inside `/app`

4. Enable Firebase services:

   * Authentication (Email/Google)
   * Realtime Database

5. Sync Gradle and run the app

---

## Screenshots

*Add your app screenshots here*

```
/screenshots/
   ├── login.png
   ├── dashboard.png
   ├── booking.png
   └── approval.png
```

---

## User Roles

* Student – Book spaces, upload LOR, view history
* Faculty – Book labs/halls
* Staff In-Charge – Approve/reject requests
* Faculty In-Charge – Escalation approvals
* HOD – Final approval
* Admin – Manage users, roles, schedules

---

## Workflow

1. User logs in using Firebase Authentication
2. Selects space and submits booking request
3. Request flows through approval hierarchy
4. Final approval updates schedule
5. Notifications are triggered

---

## Team Contributions

| Name                | Roll Number | Contribution                  |
| ------------------- | ----------- | ----------------------------- |
| Ch. Teja            | B230267CS   | Student and Faculty module, Poster design, SRS, Testing            |
| K. Abhiram          | B230669CS   | App Admin module, Firebase, repo maintenance, SRS, Testing |
| A. Somanath         | B230154CS   | Lab Admin and Staff, Hall Incharge module, Poster design, Testing                  |
| B. Venkata Subbaiah | B230871CS   | Faculty Incharge and HOD module, ppt preparation, Testing                 |
| B. Prashanth        | B2202226CS  | CSED Staff module, Testing     |

---

## Branching Strategy

* feature/<feature-name>
* fix/<bug-name>
* docs/<docs-name>
* refactor/<upgrade, change-name>
* UI/<ui-name>

---

## Contribution Workflow

1. Create a branch from `main`
2. Commit changes with clear messages
3. Push branch
4. Create Pull Request
5. Get review before merge

---

## Code Standards

* Maintain clean and readable code
* Follow consistent naming conventions
* Comment important logic

---

## Future Enhancements

* Google Calendar integration
* Email notifications
* Web admin dashboard
* Analytics for space utilization

---

## Documentation

* SRS Document (Version 2.0) 

---

## License

This project is developed for academic purposes.

---

## Academic Integrity

All contributions are original and developed as part of coursework.
