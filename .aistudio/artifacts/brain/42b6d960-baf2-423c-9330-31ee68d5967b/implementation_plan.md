# Indus Appstore Publishing & Store Readiness Plan

Complete roadmap of compulsory technical requirements, store assets, and high-value features for submitting **Pocket Home (com.pockethome.app)** to **Indus Appstore (by PhonePe)**.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The app already satisfies 100% of compulsory technical rules for Indus Appstore (Package ID `com.pockethome.app`, signed APK with release keystore, `targetSdk = 36`, `minSdk = 24`, least-privilege permissions, in-app + web account deletion, and SMS disclosure dialog).

- **Confirmed Decision 1**: Store-ready Signed APK (`pockethome.apk`) compiled with `v2` signature scheme and tested for zero-crash startup.
- **Confirmed Decision 2**: In-app account deletion + Web deletion URL (`https://pocket-home-42b6d.web.app/delete-account.html`) fully integrated.
- **Proposed Feature Addition**: Add an **Offline JSON & CSV Data Backup & Restore** utility so users can export their expense data locally before switching phones or uninstalling.

---

## 1. Overview & Core Concept

- **App Name**: Pocket Home (PocketHome)
- **Package ID**: `com.pockethome.app`
- **Target Platform**: Indus Appstore (by PhonePe) & Google Play Store
- **Key Value**: A smart, fast, privacy-first personal expense manager with offline capability, real-time SMS auto-tracking, AI money growth advisor, and zero data leakage.

---

## 2. Compulsory Store Submission Checklist (Indus Appstore)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      INDUS APPSTORE CHECKLIST                          │
├───────────────────────────────┬─────────────────────────────────────────┤
│ Mandatory Requirement         │ Status & Implementation                 │
├───────────────────────────────┼─────────────────────────────────────────┤
│ 1. Signed Release APK / AAB   │ ✅ `pockethome.apk` (v2 Signed)         │
│ 2. targetSdkVersion           │ ✅ 36 (Android 16 API)                  │
│ 3. minSdkVersion              │ ✅ 24 (Android 7.0+)                    │
│ 4. Package Name               │ ✅ `com.pockethome.app`                 │
│ 5. Account & Data Deletion    │ ✅ In-App Deletion + Web Deletion Link │
│ 6. SMS Permission Disclosure  │ ✅ Prominent Dialog before request      │
│ 7. Privacy Policy & Terms     │ ✅ Support email + Web Privacy Page     │
│ 8. Least-Privilege Manifest  │ ✅ Camera & Microphone removed          │
└───────────────────────────────┴─────────────────────────────────────────┘
```

---

## 3. Recommended New Features to Add

### Feature 1: Local Data Export & Backup (JSON & CSV)
- **Why**: Indus Appstore users frequently request local backups without requiring cloud login.
- **Implementation**:
  - Add "Export Data (CSV / JSON)" button in Settings / Profile dialog.
  - Generates structured CSV containing Date, Title, Amount, Category, Type, and Payment Method.
  - Option to import a previously backed-up JSON file to restore transactions seamlessly.

### Feature 2: Complete Store Listing Metadata Kit
- **Short Pitch**: "Aapke rozana kharchon aur aamdani ka smart hisaab-kitaab."
- **Full App Description**: 4000-character store description covering SMS auto-sync, AI advisor, monthly budgets, and zero data sharing promise.
- **Keywords**: expense manager, kharcha manager, upi tracker, budget planner, phonepe expense, daily expense tracker india.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          SYSTEM ARCHITECTURE                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌───────────────────────┐       ┌──────────────────────────────────┐  │
│  │   UI Components       │       │    Data & Persistence Layer     │  │
│  │  - HomeScreen         │       │  - SharedPreferences (Local)     │  │
│  │  - TransactionsScreen │ ◄───► │  - Firebase RTDB / Auth (Cloud) │  │
│  │  - ReportsScreen      │       │  - BackupExporter (CSV / JSON)   │  │
│  │  - Budget & Bills     │       └──────────────────────────────────┘  │
│  └───────────────────────┘                                              │
│              │                                                          │
│              ▼                                                          │
│  ┌───────────────────────┐                                              │
│  │   Indus Appstore      │                                              │
│  │   Submission Package  │                                              │
│  │  - pockethome.apk     │                                              │
│  │  - delete-account.html│                                              │
│  └───────────────────────┘                                              │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Next Execution Steps

1. Implement **Data Export & Import Utility** (CSV/JSON download and restore) in `Settings` / `Profile`.
2. Generate the **Indus Appstore Metadata Kit** in a markdown file for easy copy-pasting during store upload.
3. Perform final `gradle :app:assembleRelease` verification to guarantee APK readiness.
