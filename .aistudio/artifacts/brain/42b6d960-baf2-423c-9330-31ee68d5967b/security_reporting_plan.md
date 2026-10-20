# Implementation Plan: Security, Privacy & Reporting

## Goal
To strengthen user trust through robust security measures and provide actionable financial insights through comprehensive reporting and export functionality.

## Phase 1: Security & Privacy
- **App Lock/PIN**: 
  - Create `PinScreen` for initial set-up and authentication.
  - Implement `DataStore` to securely store the encrypted PIN hash.
  - Integrate a check in `MainActivity` to show `PinScreen` upon app launch.
- **Permission Management**: 
  - Refactor SMS permission requests to be requested only when the user explicitly triggers a sync or enables auto-tracking, rather than on launch.
- **Data Protection**: 
  - Ensure Firebase security rules restrict access to data based on authenticated `userId`.
  - Add a "Clear All Data" and "Export My Data" option within settings to give users full control.

## Phase 2: Reports & Export
- **Comprehensive Reports**:
  - Extend `ReportsScreen` to show weekly and monthly breakdowns.
  - Improve existing income-vs-expense charts with better visual feedback.
- **Export Functionality**:
  - Implement a CSV generator to export transactions.
  - Integrate a PDF generation library to create shareable monthly financial reports.

## Implementation Steps

### Phase 1: Security Implementation
- Create `SecurityManager` for PIN handling.
- Build `PinScreen` Composable.
- Update `MainActivity` navigation to gate access behind PIN.

### Phase 2: Enhanced Reporting & Export
- Create `ReportExporter` utility to generate CSV/PDF formats.
- Add `Export` buttons in `ReportsScreen` and `TransactionsScreen`.
- Refine charts using `Vico` or existing charting libraries.

## Expected Outcome
The app will meet professional standards for data security and empower users with tools to visualize their long-term financial health and share reports externally.
