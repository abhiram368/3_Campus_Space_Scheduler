# Steps of Requirements Elicitation

## 1. Identify Stakeholders
### Primary Stakeholders
Students and Faculty
- View available spaces (classrooms/labs/halls)
- Make booking requests
- Track booking status (approved / pending / rejected)
- View schedules
### Secondary Stakeholders
1. Department / Lab In-Charge
- Approve or manage bookings
- Handle conflicts
- Update room details
2. App Admin
- Manage users and roles
- Configure spaces (classrooms/labs/halls)
- Oversee system usage

## Hierarchy
- Category-1: Students and Faculty
- Category-2: Lab Admin (student or faculty)
- Category-3: Lab In-Charge (faculty)
- Category-4: App Admin

## 2. Gather Requirements
### 2.1 Functional Requirements
- The system shall allow students/faculty to login to the application using college mail id.
- Allow users to view real-time availability of campus spaces.
- Allow users to book classrooms, laboratories, and halls.
- Display schedule details including date, time, and purpose.
- Automatically assign the appropriate in-charge based on the selected space and time.
- Allow administrators to edit space and schedule details.
- Provide booking status updates (approved / pending / rejected). (optional)
- The system shall allow admin to manage conflicted bookings.
- Provide notifications for booking updates.
### 2.2 Non-Functional Requirements
- Provide a quick response time for viewing availability and booking.
- Quick updation of availability of space in real time.
- Shall have an easy-to-use interface suitable for mobile devices.
- Shall ensure reliable data storage and retrieval.
- Support simultaneous access by multiple users.
### 2.3 Optional / Enhancement Requirements
- Display the location of spaces on a campus map.
- Support future expansion to web or iOS platforms.

## 3. Prioritize Requirements
### 3.1 Must Have (Core requirements)
- View real-time availability of classrooms, labs, and halls
- View schedules (date, time, space, purpose)
- Book campus spaces
- Prevent booking conflicts (no double booking)
- Automatic allotment of lab/hall in-charge
- Role-based access (student / faculty / admin)
- Store and retrieve booking data reliably (Firebase)
### 3.2 Should Have (Important but not critical)
- Booking status display (pending / approved / rejected)
- Easy editing of booking and space details by admin
- Quick response time optimization
- Basic validation and error handling
### 3.3 Could Have (Optional / Enhancements)
- Space location display on campus map
- Notifications for booking updates
- Advanced search and filters
- Usage analytics for administrators
### 3.4 Won’t Have (For current version)
- iOS or Web deployment
- Integration with external university systems
- Payment or fee handling

## 4. Categorize Feasibility
### 4.1 Technical Feasibility
| Requirement                   | Feasibility         | Remarks                         |
|-------------------------------|---------------------|---------------------------------|
| Real-time availability        | Feasible            | Firebase real-time updates      |
| Booking                       | Feasible            | Firestore CRUD                  |
| Conflict prevention           | Feasible            | Time-slot validation            |
| Automated in-charge allotment | Feasible            | Rule-based mapping              |
| Booking status                | Feasible            | Simple state management         |
| Campus map view               | Partially feasible  | Requires map API integration    |
### 4.2 Operational Feasibility
| Aspect         | Feasibility | Remarks                                  |
|----------------|-------------|------------------------------------------|
| Ease of use    | Feasible    | Mobile-friendly Flutter UI               |
| User acceptance| Feasible    | Similar to existing booking practices    |
| Admin control  | Feasible    | Role-based access supported              |
### 4.3 Economic Feasibility
| Factor              | Feasibility | Remarks                      |
|---------------------|-------------|------------------------------|
| Development cost    | Feasible    | Open-source tools            |
| Infrastructure cost | Feasible    | Firebase free tier           |
| Maintenance cost    | Feasible    | Minimal for academic use     |

### 4.4 Schedule Feasibility
| Phase            | Feasibility     | Remarks                        |
|------------------|-----------------|--------------------------------|
| Core features    | Feasible        | Achievable within semester     |
| Optional features| Time-dependent  | Implement if time permits      |
### 4.5 Legal and Ethical Feasibility
| Aspect        | Feasibility | Remarks                                       |
|---------------|-------------|-----------------------------------------------|
| Data privacy  | Feasible    | Uses Firebase Authentication                  |
| Licensing     | Feasible    | Flutter & Firebase are free for academic use  |

## 5. Constraints and Assumptions
- Only campus users with verified IDs can book spaces.
- The system is developed as a college academic project.
- The initial deployment targets Android devices.
- Internet connectivity is required for system usage.
## 6. Summary
The elicited requirements define the core functionality and constraints of the Campus Space Scheduler and serve as the foundation for system design and development.