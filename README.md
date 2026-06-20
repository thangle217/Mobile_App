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
    │   ├── SplashScreen.kt           # Animated launch screen
    │   ├── SearchBar.kt              # Reusable search input
    │   ├── FilterTabRow.kt           # Pill-style horizontal filter tabs
    │   ├── StatusBadges.kt           # Room & contract status chips
    │   ├── MetricSummaryCard.kt      # Dashboard KPI cards
    │   ├── EmptyStatePlaceholder.kt  # Empty list state UI
    │   ├── ShimmerComponents.kt      # Skeleton loading animations
    │   ├── AppSnackbar.kt            # Custom in-app notifications
    │   ├── TenantAvatar.kt           # Initials avatar with HSL colors
    │   ├── GradientHeaderBanner.kt   # Gradient banner for detail pages
    │   ├── OccupancyRingIndicator.kt # Animated circular progress
    │   └── ConfirmationBottomSheet.kt# Destructive action confirmation
    └── theme/               # Color, Typography, Spacing, Shape, and Theme specifications
        ├── Color.kt
        ├── Type.kt
        ├── Theme.kt
        ├── Spacing.kt        # 4dp grid spacing tokens
        └── Shape.kt          # Corner radius tokens
```

## UI Component Library

The `feature/ui-improvements` branch introduces a rich set of reusable UI components:

| Component | Description |
|---|---|
| `SplashScreen` | Animated launch screen with fade + scale |
| `SearchBar` | M3 outlined search input |
| `FilterTabRow` | Pill-style scrollable filter tabs |
| `RoomStatusBadge` | Occupied / Vacant / Maintenance chip |
| `ContractStatusBadge` | Active / Expired / Pending / Terminated chip |
| `MetricSummaryCard` | Dashboard KPI card with gradient container |
| `EmptyStatePlaceholder` | Empty state with icon, title, and optional CTA |
| `ShimmerBox / ShimmerListItemCard` | Skeleton loading animations |
| `AppSnackbar` | Animated SUCCESS / ERROR / WARNING / INFO snackbar |
| `TenantAvatar` | Initials avatar with deterministic HSL color |
| `GradientHeaderBanner` | Gradient header for detail screens |
| `OccupancyRingIndicator` | Animated circular occupancy gauge |
| `ConfirmationBottomSheet` | Bottom sheet for destructive actions |

## Getting Started

1. Open the project in **Android Studio**.
2. Sync the project with Gradle files.
3. Run the application on an emulator or a physical device (API level 24 or higher).
