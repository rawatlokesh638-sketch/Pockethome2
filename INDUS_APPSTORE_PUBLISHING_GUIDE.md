# 📱 Indus Appstore (by PhonePe) Publishing Guide & Checklist
## App: PocketHome (Personal Finance & Expense Tracker)

PocketHome is 100% production-ready for submission and publishing on **Indus Appstore (by PhonePe)**. All technical guidelines, store policies, user data privacy laws, and security standards have been fulfilled.

---

### 1. App Store Metadata & Information

| Field | Submission Value |
| :--- | :--- |
| **App Title** | `PocketHome - Smart Expense Tracker` (Under 30 chars: `PocketHome`) |
| **Short Description** | `Auto-categorize bank SMS expenses, manage monthly budget, bills & AI savings advice.` |
| **Package Name (`applicationId`)** | `com.pockethome.app` |
| **Version Code** | `1` |
| **Version Name** | `1.0.0` |
| **Minimum SDK** | `Android 7.0 (API level 24)` |
| **Target SDK** | `Android 15 / 16 (API level 36)` |
| **App Category** | `Finance` / `Personal Finance` |
| **Content Rating** | `Everyone (3+)` |
| **Developer / Publisher** | `Lokesh Rawat` |
| **Support / Help Email** | `rawatlokesh638@gmail.com` |

---

### 2. Mandatory Store URLs

When filling the Indus Appstore developer console form:

- **Privacy Policy URL**:
  `https://rawatlokesh638.github.io/pockethome/index.html` (or your GitHub Pages hosted root)
- **Account & Data Deletion URL (Mandatory by Indus & Play Store)**:
  `https://rawatlokesh638.github.io/pockethome/delete-account.html`
  *(This page explains in-app deletion and provides a 1-tap email deletion request to `rawatlokesh638@gmail.com` with a 24-48h SLA)*

---

### 3. Permissions & Justification for Indus Appstore Reviewers

Indus Appstore strictly reviews apps declaring SMS permissions. PocketHome uses the following permissions with full compliance:

1. **`READ_SMS` & `RECEIVE_SMS`**:
   - **User Benefit**: Automatically reads transactional SMS from Indian banks (SBI, HDFC, ICICI, PNB, Axis, etc.) and UPI apps (PhonePe, Google Pay, Paytm) to log debit and credit expenses without manual effort.
   - **Prominent In-App Disclosure**: App displays `SmsPermissionDisclosureDialog` **before** prompting for runtime permissions.
   - **Zero Personal Data**: App explicitly ignores personal, OTP, and confidential messages.
   - **Zero Third-Party Sharing**: No data is shared with advertisers, lenders, or brokers.
   - **Graceful Degradation**: If user denies SMS permission, the app works 100% normally with manual addition of expenses, incomes, bills, and budget tracking.

2. **`INTERNET` & `ACCESS_NETWORK_STATE`**:
   - For real-time Firebase cloud synchronization and AI Wealth Advisor queries.

---

### 4. Account Deletion Feature (Store Compliance)

- **In-App Deletion**:
  - Open app -> Tap **Profile Avatar** (or scroll to footer) -> Tap **"Delete Account & All Data"** -> Confirm.
  - Instantly deletes:
    - User account in Firebase Auth
    - All user entries in Firebase Realtime Database (`/users/{uid}`, `/leaderboard_ranks/{uid}`, `/pro_requests/{uid}`)
    - All local SharedPreferences cache
- **Web-Based Deletion**:
  - Available at `https://rawatlokesh638.github.io/pockethome/delete-account.html` or via direct email to `rawatlokesh638@gmail.com`.

---

### 5. APK Signing Details

The release APK is signed with **APK Signature Scheme v2**:
- **Signed APK file**: `pockethome.apk` (and `app/build/outputs/apk/release/app-release.apk`)
- **Keystore**: `my-upload-key.jks`
- **Key Alias**: `upload`
- **Verification Result**:
  ```
  Verified using v2 scheme (APK Signature Scheme v2): true
  Number of signers: 1
  Status: SUCCESS
  ```

---

### 6. Submission Steps on Indus Appstore Developer Console

1. Log in to [Indus Appstore Developer Console](https://developer.indusappstore.com/).
2. Click **Create New App**.
3. Fill App Title: `PocketHome` and Category: `Finance`.
4. Upload `pockethome.apk`.
5. Enter Privacy Policy URL and Account Deletion URL.
6. Submit for review!
