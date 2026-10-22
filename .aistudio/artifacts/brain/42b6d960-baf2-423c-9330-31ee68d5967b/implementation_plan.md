# Pockethome Gamification: 10-Tier Level System & Real-Time Leaderboard

Introducing a comprehensive 10-tier financial gamification system (**Iron** to **God**) paired with an anonymous real-time **Leaderboard**, helping users build disciplined savings habits through XP progress and friendly rank competition.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the following design choices for the level system and leaderboard:

- **Confirmed Level Calculation**: Tiers unlocked based on **Financial Health Score** (0-100) and **Monthly Savings Percentage** (20%+ savings rule, budget adherence, and zero-overspend streaks).
- **Confirmed Leaderboard Scope**: Anonymous global & household ranking based on total Financial Health XP, protecting financial privacy while providing competitive motivation.
- **Confirmed UI Placement**: A new **Dedicated Gamification / Leaderboard Tab** in navigation plus a prominent **Level Badge** on the Profile & Dashboard Header.

---

## 1. Overview & Core Concept

### The 10 Tier Progression Model

| Tier Level | Name | Target Financial Health Score & Criteria | Badge Visual Style |
| :--- | :--- | :--- | :--- |
| **Level 1** | **Iron 🪙** | XP 0 – 150 (Score < 30) | Brushed Slate / Dark Iron |
| **Level 2** | **Bronze 🥉** | XP 150 – 350 (Score 30-40) | Metallic Warm Bronze |
| **Level 3** | **Copper 🟤** | XP 350 – 600 (Score 40-50) | Polished Copper / Amber Accent |
| **Level 4** | **Silver 🥈** | XP 600 – 1,000 (Score 50-60) | High-Gleam Silver Sheen |
| **Level 5** | **Gold 🥇** | XP 1,000 – 1,500 (Score 60-70) | Luminous Gold Gradient |
| **Level 6** | **Platinum 💠** | XP 1,500 – 2,200 (Score 70-80) | Platinum Ice Blue |
| **Level 7** | **Diamond 💎** | XP 2,200 – 3,000 (Score 80-88) | Prismatic Cyan Diamond |
| **Level 8** | **Ruby 🔴** | XP 3,000 – 4,000 (Score 88-94) | Deep Crimson Ruby Glow |
| **Level 9** | **Grandmaster 👑** | XP 4,000 – 5,500 (Score 94-98) | Royal Violet & Gold Crown |
| **Level 10** | **God ⚡** | XP 5,500+ (Score 99-100) | Cosmic Gold Shimmer & Sparkles |

### How XP is Earned
- **Daily Log-in & Expense Tracking**: +20 XP / day
- **Budget Adherence**: +100 XP / month if total spending remains within budget
- **20%+ Monthly Savings Rule**: +150 XP for saving 20% or more of income
- **Emergency Runway Milestone**: +250 XP when liquid emergency fund reaches 3+ months

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Header & Profile Badge**:
   - The user's current level badge (e.g. `Gold 🥇 • Level 5`) appears next to their profile name on the Dashboard header.
   - Tapping the badge opens the **Gamification Tab**.

2. **Gamification Tab (Leaderboard & Progress)**:
   - **Top Card**: Current Tier Progress Bar (e.g., `Gold 🥇 - 1,250 / 1,500 XP to Platinum 💠`), level perks, and daily/monthly streak badges.
   - **Podium Section**: 1st, 2nd, and 3rd place highlight cards displaying avatars, tier badges, and anonymous handle names (e.g., *Saver_#8492*).
   - **Rank List**: Infinite scroll list displaying user's current global position, score, and rank difference.

3. **Tier Level-Up Celebration**:
   - Animated full-screen celebration dialog when a user transitions from one tier to the next (e.g., Bronze -> Copper or Platinum -> Diamond).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Anonymous Handles on Leaderboard**
  - *Rationale*: Net worth and exact savings figures are private. Only XP score, rank title (e.g., *Gold Saver*), and randomized avatar handle (e.g., *SmartSaver_392*) are shared on the global leaderboard.
- **Decision 2: Local & Firebase Hybrid Sync**
  - *Rationale*: XP is calculated locally in `DashboardViewModel` from actual budget/transaction data and cached in `SharedPreferences`, then synced to Firebase `leaderboard_ranks` node for real-time global ranking.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    DashboardViewModel                       │
│  - Calculates Financial Health Score & Monthly Savings %   │
│  - Computes XP Points & Current Level Tier (Iron -> God)    │
└──────────────────────────────┬──────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌──────────────────────────────┐       ┌───────────────────────┐
│     Local SharedPreferences  │       │   Firebase RTDB       │
│  - cached_user_xp            │       │   leaderboard_ranks/  │
│  - cached_level_tier         │       │    └─ {userId}:       │
└──────────────────────────────┘       │        - handleName   │
                                       │        - xpPoints     │
                                       │        - levelTier    │
                                       │        - healthScore  │
                                       └───────────────────────┘
```

### Proposed Code Changes

1. **`com.example.model.ExpenseModels.kt`**:
   - Add `UserLevelTier` enum and `LeaderboardEntry` data class with 10 tiers (IRON, BRONZE, COPPER, SILVER, GOLD, PLATINUM, DIAMOND, RUBY, GRANDMASTER, GOD).

2. **`com.example.data.ExpenseRepository.kt`**:
   - Add methods to calculate & sync XP and fetch top 50 leaderboard entries from Firebase RTDB `leaderboard_ranks`.

3. **`com.example.ui.screens.LeaderboardScreen.kt`**:
   - Create a dedicated Jetpack Compose screen with podium cards, progress bar, 10 level tier roadmap, and rank list.

4. **`com.example.ui.DashboardScreen.kt`**:
   - Add `AppTab.LEADERBOARD` to navigation tabs and display user level badge on top bar.

---

## Verification Plan

- Run `compile_applet` to ensure seamless compilation.
- Verify XP calculation based on health score and monthly savings percentage.
- Verify tier transition logic across all 10 tiers (Iron to God) and leaderboard list rendering.
