# Rental Manager Mobile App

A local-first, premium Android application designed to manage rental properties, rooms, tenants, contracts, utility bills, and incident reports.

## Features

- **Local-first Architecture**: Robust offline capabilities with localized storage (`LocalAppStore`) and local session management (`SessionStore`).
- **Modern Jetpack Compose UI**: Clean, responsive design with dynamic theme support, animated transitions (`AnimatedContent`), and native gesture support.
- **Comprehensive Management Modules**:
  - **Properties & Rooms**: Manage housing lists, room occupancy, status tracking, and details.
  - **Utility Billing**: Track water and electricity meter indexes, calculate monthly usage, and generate detailed invoices.
  - **Contracts & Requests**: Complete flow for rental requests, contract generation, and automated lease renewals.
  - **Incident & Issue Reporting**: Real-time reporting and status updates for room issues (e.g., plumbing, electrical maintenance).
  - **Notifications**: Automated notice generation for payments, house rules, and custom alerts.

## Project Structure

```
app/src/main/java/com/example/myapplication/
│
├── MainActivity.kt          # App entry point
├── data/
│   ├── local/               # Local data store (LocalAppStore, SessionStore)
│   ├── remote/              # API clients and network models (ApiClient, NetworkModels)
│   └── repository/          # Repository pattern implementation (RentalRepository)
│
├── domain/
│   ├── model/               # Core business models (UserSession, UserRole, AppModels)
│   └── util/                # Formatting and validation utilities (AppFormat)
│
└── ui/
    ├── app/                 # Main App UI flow, Dialogs, and Screens (AuthScreens, DashboardScreen)
    └── theme/               # Color, Typography, and Theme specifications
```

## Getting Started

1. Open the project in **Android Studio**.
2. Sync the project with Gradle files.
3. Run the application on an emulator or a physical device (API level 24 or higher).
