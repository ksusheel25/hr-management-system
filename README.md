# HR Attendance SaaS - Spring Boot Project

## Overview
This is a **HR Attendance Management System** built with **Java Spring Boot**, designed for managing:

- Employee attendance (check-in/check-out)
- Office & remote work tracking (WFH)
- Leave management & approval workflow
- Role-based authentication & authorization
- Audit logging and notifications for managers
- Daily, weekly, monthly attendance reports

All APIs are **secure** and **role-based**.

---

## Features

1. **Employee Attendance**
    - Automatic presence/absence detection
    - Office check-in via biometric simulation
    - Remote attendance automatically marked
    - Nightly job (11:59 PM) calculates present/absent

2. **Shift & Work Management**
    - Shift tracking
    - WFH & leave tracking
    - Manager approval workflow
    - Notifications to managers on leave/WFH applications

3. **Authentication & Authorization**
    - Role-based access control (RBAC)
    - JWT-based authentication
    - All APIs secured

4. **Reports**
    - Daily, weekly, and monthly attendance reports
    - From-date and to-date filter
    - List of objects returned for easy UI display

5. **Admin APIs**
    - Upload employee data via Excel
    - Handles missing Apache POI dependency gracefully
    - ObjectMapper bean for JSON serialization

6. **Audit & Logging**
    - Logs all critical actions
    - Role-based audit tracking

---

## Tech Stack

- **Backend:** Java 25, Spring Boot
- **Database:** H2 (for dev/testing), MySQL (for production)
- **Build Tool:** Maven
- **Security:** Spring Security, JWT
- **Scheduler:** Spring Scheduled
- **Notifications:** Email/Slack (configurable)

---

## Getting Started

### Prerequisites

- Java 25+
- Maven
- Git
- (Optional) Docker for containerized deployment

### Clone Repository

```bash
git clone <your_repo_url>
cd <project_root>