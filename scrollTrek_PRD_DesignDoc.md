# scrollTrek — Product Requirements & Design Document
**Version 2.0 | Refined Edition**

---

## 1. Product Vision

> *"Every swipe tells a story. scrollTrek makes you feel the weight of it."*

scrollTrek is a **passive digital wellness companion** for Android that silently measures how far your finger travels while scrolling — across every app on your phone — and maps that invisible distance to iconic real-world landmarks. When you've scrolled as far as the height of the Eiffel Tower, you know. When you've crossed the width of the Grand Canyon, you feel it. The goal isn't shame — it's **awareness through tangible scale**.

---

## 2. The Problem

| Symptom | Reality |
|---|---|
| "I barely use my phone" | Average user scrolls 300+ feet per day |
| "I don't doomscroll that much" | One TikTok session = height of a skyscraper |
| Screen time stats feel abstract | "3h 42m" means nothing viscerally |
| No social hook for digital wellness | Nobody shares their screen time report |

Abstract screen-time data doesn't change behavior. Physical distance does. scrollTrek converts invisible thumb habits into visceral, shareable metaphors.

---

## 3. Target Users

### Primary Persona — "The Curious Skeptic"
- Age 22–35, smartphone-native
- Self-aware about phone usage but doesn't act on it
- Loves gamification (Duolingo streaks, Strava segments)
- Would share a funny/shocking milestone card on Instagram
- Motivation: **bragging rights + mild self-reflection**

### Secondary Persona — "The Wellness Seeker"
- Age 28–45, already uses screen time tools
- Wants actionable insight, not just data
- Would pay for premium features
- Motivation: **behavioral change, accountability**

### Tertiary Persona — "The Social Competitive"
- Any age, motivated by leaderboards
- Would compete with friends on weekly scroll totals
- Motivation: **winning, community**

---

## 4. Core Value Propositions

1. **Zero active effort** — set it up once, it runs forever silently
2. **Visceral metrics** — "3.2 km scrolled" > "4 hours screen time"
3. **Beautiful milestone cards** — shareable art, not boring screenshots
4. **Gamification with depth** — landmark progression, streaks, badges
5. **Privacy-first** — everything local, zero data leaves the device

---

## 5. Feature Scope (v1.0)

### 5.1 Must-Have (P0)

| Feature | Description |
|---|---|
| **Global Scroll Tracking** | Accessibility Service captures scroll distance across all apps, all the time |
| **Pixel → Meter Conversion** | Hardware-accurate, per-device DPI calculation — never approximate |
| **Landmark Milestone System** | 50+ curated global landmarks across tiers; unlocked progressively |
| **Home Dashboard** | Today / Week / Lifetime distance; current landmark progress; recent unlocks |
| **Milestone Unlock Cards** | Auto-generated shareable art cards when a landmark is reached |
| **Persistent Background Daemon** | Foreground Service + battery optimization exemption flow |
| **Onboarding & Permission Flow** | Clear, trust-building setup for Accessibility + Notification permissions |

### 5.2 Should-Have (P1)

| Feature | Description |
|---|---|
| **Floating Overlay** | Optional HUD showing live session distance + current pace |
| **Daily / Weekly Streaks** | Consecutive days tracked; streak freeze mechanic |
| **Per-App Breakdown** | Which apps contributed most to today's scroll distance |
| **Scroll Heatmap** | Time-of-day distribution of scrolling intensity |
| **Widget Support** | Home screen widget showing today's distance + landmark progress |
| **Dark / Light / AMOLED themes** | Full theming support |

### 5.3 Nice-to-Have (P2)

| Feature | Description |
|---|---|
| **Friends Leaderboard** | Compare weekly scroll with opted-in friends (local code-share, no account required) |
| **Scroll Goals** | Set a max daily target; notification when approaching limit |
| **World Tour Mode** | Guided "journey" — sequentially scroll across a virtual route (e.g. Route 66) |
| **Historical Replay** | Animated replay of a past week's scroll journey across landmarks |
| **Export Data** | Export personal scroll CSV for power users |

### 5.4 Explicitly Out of Scope (v1.0)
- Cloud sync or accounts
- iOS support
- Content of what was scrolled (privacy boundary — we only track distance)
- Social network integrations beyond native Android share sheet

---

## 6. Landmark Milestone System

### 6.1 Tier Structure

Landmarks are organized into **four rarity tiers**, creating a sense of progression and reward:

| Tier | Name | Color | Unlock Feel |
|---|---|---|---|
| 1 | **Common** | Stone gray | Everyday achievements, first week |
| 2 | **Notable** | Copper bronze | Weekly milestones |
| 3 | **Landmark** | Gold | Monthly achievements |
| 4 | **Legendary** | Holographic prismatic | Lifetime achievements |

### 6.2 Curated Landmark Examples

**Vertical (Height-based)**
| Landmark | Height | Tier |
|---|---|---|
| Standard Door | 2.1 m | Common |
| Basketball Hoop | 3.05 m | Common |
| Telephone Pole | 12 m | Common |
| Statue of Liberty (torch) | 93 m | Notable |
| Big Ben Clock Tower | 96 m | Notable |
| Eiffel Tower | 330 m | Notable |
| Empire State Building | 443 m | Landmark |
| Burj Khalifa | 828 m | Landmark |
| Mount Everest | 8,849 m | Legendary |
| Kármán Line (Space) | 100 km | Legendary |

**Horizontal (Distance-based)**
| Landmark | Distance | Tier |
|---|---|---|
| Olympic Pool Length | 50 m | Common |
| Football Field | 91 m | Common |
| Golden Gate Bridge | 2.7 km | Notable |
| Manhattan Island (length) | 21.6 km | Notable |
| English Channel | 33.8 km | Landmark |
| Nile River | 6,650 km | Legendary |

**Cumulative (Lifetime-based)**
| Landmark | Distance | Tier |
|---|---|---|
| Earth's Circumference | 40,075 km | Legendary |
| Earth to Moon | 384,400 km | Legendary |

### 6.3 Landmark Card Data Schema

```
LandmarkCard {
  id: String (UUID)
  name: String              // "Burj Khalifa"
  location: String          // "Dubai, UAE"
  distanceMeters: Double    // 828.0
  tier: Enum(COMMON, NOTABLE, LANDMARK, LEGENDARY)
  orientation: Enum(VERTICAL, HORIZONTAL)
  funFact: String           // "Tallest building in the world since 2010"
  illustration: DrawableRes  // Custom vector art
  cardGradientStart: Color
  cardGradientEnd: Color
  unlockedAt: Long?          // Epoch ms, null if not yet unlocked
  shareMessage: String       // "I just scrolled as high as the Burj Khalifa 🏙️"
}
```

---

## 7. Scroll Tracking — Technical Requirements

### 7.1 Accuracy Requirements

| Metric | Target |
|---|---|
| Pixel-to-meter error margin | < 0.5% |
| Minimum detectable scroll | Touch slop threshold (device-specific, ~8dp) |
| Max event processing latency | < 2ms on-thread |
| DB flush interval | Every 30 seconds or 10m of scroll data, whichever comes first |

### 7.2 Event Interception Strategy (Priority Order)

1. **Primary:** `TYPE_VIEW_SCROLLED` + `getScrollDeltaY()` — works for most standard apps
2. **Fallback A:** `TYPE_WINDOW_CONTENT_CHANGED` + node tree delta comparison — for apps suppressing standard events
3. **Fallback B:** Touch event velocity heuristic via `VelocityTracker` — last resort for custom OpenGL/Vulkan renderers (TikTok, games)

### 7.3 Conversion Formula

```
physicalInches = scrollDeltaPx / display.ydpi
physicalMeters = physicalInches × 0.0254
```

Where `display.ydpi` is fetched from `WindowManager.currentWindowMetrics` (API 30+) or `Display.getRealMetrics()` for legacy.

### 7.4 Noise Filtering Rules

| Filter | Rule |
|---|---|
| Touch slop | Ignore deltas < `ViewConfiguration.getScaledTouchSlop()` |
| App exclusion list | User can exclude apps (e.g., maps, keyboards) |
| Screen-off guard | Pause tracking when display is off |
| Direction filter | Track only downward scroll by default (configurable) |
| Duplicate event guard | Deduplicate events with identical timestamp + delta within 16ms |

---

## 8. Background Execution Architecture

### 8.1 Process Structure

```
App Process (UI)
    └── MainActivity, Dashboard, Settings
    
:tracker Process (isolated)
    ├── ScrollTrackingAccessibilityService
    ├── TrackingForegroundService
    └── ScrollEventProcessor (Coroutine scope)
    
Shared IPC
    └── Room Database (multi-process access via ContentProvider)
```

### 8.2 Battery Optimization Strategy

| Layer | Mechanism |
|---|---|
| Process isolation | `android:process=":tracker"` prevents OOM kill cascade |
| Foreground Service | Persistent notification → OS treats as high-priority |
| Battery exemption | Onboarding prompts user to whitelist via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| Doze handling | Service re-registers on `ACTION_DEVICE_IDLE_MODE_CHANGED` broadcast |
| App Standby recovery | `JobScheduler` watchdog pings service every 15 min |
| Data buffering | In-memory ring buffer (max 500 events); flush on any of: time, count, or app background |

### 8.3 Foreground Notification Design

```
[scrollTrek icon]  scrollTrek is tracking
                   Today: 247m · 🏔 Eiffel Tower in 83m
                   [Pause]  [Open App]
```

The notification updates every 60 seconds with progress toward the current landmark. This turns a mandatory transparency requirement into a **delight touchpoint**.

---

## 9. Data Architecture

### 9.1 Database Schema (Room)

**`scroll_events`** — raw time-series (kept 30 days, then pruned)
```sql
id          INTEGER PRIMARY KEY
timestamp   INTEGER NOT NULL
delta_px    REAL    NOT NULL
delta_m     REAL    NOT NULL
app_package TEXT    NOT NULL
session_id  TEXT    NOT NULL
```

**`daily_aggregates`** — pre-computed daily rollups
```sql
date_key        TEXT PRIMARY KEY   -- "2026-05-24"
total_meters    REAL NOT NULL
top_app         TEXT
scroll_sessions INTEGER
active_minutes  INTEGER
```

**`milestones`** — achievement state
```sql
landmark_id     TEXT PRIMARY KEY
unlocked_at     INTEGER            -- epoch ms, NULL if locked
notified        INTEGER DEFAULT 0  -- boolean
card_generated  INTEGER DEFAULT 0
```

**`streak_state`** — single-row state table
```sql
current_streak      INTEGER
longest_streak      INTEGER
last_active_date    TEXT
freeze_tokens       INTEGER DEFAULT 1
```

### 9.2 Computed Metrics

All complex derived metrics (weekly total, per-app breakdown, heatmap data) are computed as `@DatabaseView` or `Flow<List<T>>` queries — never cached in application memory beyond a single composition frame.

---

## 10. UX & Information Architecture

### 10.1 Screen Map

```
App Launch
    └── Onboarding (first run only)
            ├── Step 1: Value prop / Hero animation
            ├── Step 2: Accessibility permission request (with clear explanation)
            ├── Step 3: Notification permission
            ├── Step 4: Battery optimization exemption
            └── Step 5: "All set" celebration screen

Main Navigation (Bottom bar: 3 tabs)
    ├── 🏠 Home (Dashboard)
    │       ├── Today's distance card
    │       ├── Current landmark progress arc
    │       ├── Recent unlocks carousel
    │       └── Weekly bar chart
    │
    ├── 🗺 Journey (Landmarks)
    │       ├── Locked / Unlocked grid
    │       ├── Landmark detail sheet
    │       └── Share card generator
    │
    └── 📊 Insights
            ├── Per-app breakdown
            ├── Time-of-day heatmap
            ├── Streak tracker
            └── Settings (nested)
```

### 10.2 Critical UX Principles

1. **Zero required interaction after setup** — the app should work without ever opening it
2. **Delight on open, not on setup** — every dashboard open should surface something surprising
3. **The milestone card IS the product** — it must be beautiful enough to post without cropping
4. **No guilt, only perspective** — copy is always neutral or playful, never shaming
5. **Floating overlay is opt-in and non-intrusive** — appears only when user explicitly enables it; never blocks taps

### 10.3 Onboarding Trust Principles

Accessibility permission is the #1 friction point. The onboarding must:
- Explicitly state **what is tracked** (scroll distance) and **what is NOT** (content, text, screenshots)
- Show a clear, visual diagram of how the service works
- Use the phrase "scrollTrek never reads what you see — only how far you scroll"
- Display a privacy badge ("100% local. No servers. No accounts.")
- Allow users to review the open-source accessibility event handling code (link to GitHub)

---

## 11. Milestone Share Cards — Design Specification

### 11.1 Card Format
- **Dimensions:** 1080 × 1080px (square, Instagram-optimized) + 1080 × 1920px (story variant)
- **Generation:** Headless Jetpack Compose rendering via `ComposeView` + `PixelCopy` API
- **File output:** JPEG at 90% quality to `/Pictures/scrollTrek/`

### 11.2 Card Anatomy

```
┌─────────────────────────────────┐
│  [Subtle gradient background]   │
│                                 │
│  scrollTrek                (logo)│
│                                 │
│      [Landmark Illustration]    │
│           (full-bleed art)      │
│                                 │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                 │
│  I scrolled as far as           │
│  THE BURJ KHALIFA               │  ← Display font, large
│                                 │
│  828 meters of pure thumbwork   │  ← Subtitle
│                                 │
│  Dubai, UAE  ·  Tier: Landmark  │
│                                 │
│  [Fun fact about the landmark]  │
│                                 │
│  scrolltrek.app                 │  ← Acquisition hook
└─────────────────────────────────┘
```

### 11.3 Visual Language Per Tier

| Tier | Background Style | Typography Feel | Animation on Reveal |
|---|---|---|---|
| Common | Muted linen texture | Clean, sans | Simple fade |
| Notable | Copper foil gradient | Bold, structured | Slide up |
| Landmark | Dark gold with grain | Editorial, large | Cinematic scale-in |
| Legendary | Holographic prismatic shimmer | Condensed display, all-caps | Particle burst |

---

## 12. Floating Overlay — Design Specification

### 12.1 States

| State | Visual |
|---|---|
| Compact (default) | Pill: `↕ 47m  🏔 83m left` |
| Expanded (tap) | Card: distance, pace, current target landmark, mini progress bar |
| Minimized | Small dot with pulse animation |

### 12.2 Behavior Rules
- Renders via `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY`
- Touch-pass-through except on the overlay itself (`FLAG_NOT_TOUCH_MODAL | FLAG_WATCH_OUTSIDE_TOUCH`)
- Draggable to any screen position; position persists to SharedPreferences
- Auto-hides during full-screen video (detects `FLAG_FULLSCREEN` window flag)
- Never appears on lock screen

---

## 13. Monetization Strategy

### 13.1 Model: Freemium

**Free Tier**
- Full scroll tracking (forever)
- First 15 landmarks
- Basic dashboard
- Standard share cards

**scrollTrek Pro** (~$2.99/month or $19.99/year)
- All 50+ landmarks (including Legendary tier)
- Animated share cards
- Per-app breakdown
- Floating overlay
- Home screen widget
- Streak freeze tokens (3/month)
- World Tour Mode (v1.5)

### 13.2 Monetization Principles
- Core tracking is **always free** — the app's value prop must never be paywalled
- No ads, ever — ads would undermine the anti-doomscrolling positioning
- Pro upsell surfaces only on natural moments (unlocking tier-3, trying overlay)

---

## 14. Privacy & Security Specification

| Concern | Mitigation |
|---|---|
| Content snooping | Service only subscribes to `TYPE_VIEW_SCROLLED`; no text content events |
| Data exfiltration | Zero network permissions in Manifest; no internet connectivity |
| Sensitive apps | Pre-loaded exclusion list (banking apps, health apps); user can add more |
| Data retention | Raw events auto-purge after 30 days; only aggregates kept indefinitely |
| Data portability | User can export or delete all data from Settings |
| Accessibility abuse | Service config is minimal, auditable, and open-source |

---

## 15. Engineering Task Breakdown

### Phase 1: Foundation (Weeks 1–3)
| Task | Effort |
|---|---|
| Accessibility Service scaffold + manifest config | 2d |
| Scroll event interception (primary path) | 3d |
| Fallback heuristic (node tree delta) | 4d |
| DPI-aware pixel-to-meter conversion | 1d |
| Noise filtering (touch slop, dedup, screen-off) | 2d |
| Room DB schema + DAOs + Migrations | 2d |
| In-memory buffer + periodic flush | 2d |
| Foreground Service + notification | 2d |
| Process isolation setup | 1d |

### Phase 2: Core UX (Weeks 4–6)
| Task | Effort |
|---|---|
| Onboarding flow (5 steps + permission flows) | 4d |
| Home Dashboard screen (Compose) | 3d |
| Journey / Landmarks grid screen | 3d |
| Landmark data model + 50-entry dataset | 3d |
| Landmark detail bottom sheet | 2d |
| Insights screen (heatmap, per-app breakdown) | 4d |
| Streak logic + UI | 2d |

### Phase 3: Delight (Weeks 7–9)
| Task | Effort |
|---|---|
| Share card headless renderer | 4d |
| Card visual design (all tiers, all orientations) | 5d |
| Milestone unlock notification + animation | 2d |
| Floating overlay (draggable, states, auto-hide) | 4d |
| Home screen widget | 3d |
| Battery optimization onboarding flow | 1d |
| App exclusion list UI | 2d |

### Phase 4: Polish + Launch (Weeks 10–12)
| Task | Effort |
|---|---|
| Dark / Light / AMOLED theme pass | 2d |
| Accessibility audit (ironic but important) | 2d |
| Performance profiling (Systrace, memory) | 3d |
| Doze / Standby regression testing | 2d |
| Play Store listing + screenshots | 2d |
| Beta rollout + crash monitoring setup | 2d |

**Total estimate: ~12 weeks, 1–2 engineers**

---

## 16. Success Metrics (Launch KPIs)

| Metric | Target (30 days post-launch) |
|---|---|
| D1 Retention | > 45% |
| D7 Retention | > 25% |
| Accessibility permission grant rate | > 70% |
| Milestone cards shared (per user) | > 0.5/week |
| Pro conversion rate | > 4% |
| Crash-free session rate | > 99.5% |
| Background kill rate (tracking stopped unexpectedly) | < 2% |

---

## 17. Open Questions & Decisions Needed

| Question | Options | Recommendation |
|---|---|---|
| Scroll direction | Down only vs. all directions | Down-only default, all-direction option in settings |
| Landmark unlocking | Sequential vs. any-order | Any-order (more satisfying) with a "next closest" highlight |
| Onboarding gate | Require accessibility permission to proceed vs. optional | Required — app is useless without it; be transparent about why |
| Overlay default state | On vs. Off | Off by default; prompt once at 100m mark |
| Friends feature | v1 vs. v1.5 | v1.5 — get core tracking rock-solid first |
| Landmark art style | Photography vs. flat illustration vs. 3D render | Flat illustration — consistent, scalable, brandable |

---

*scrollTrek PRD v2.0 — Refined for engineering & design handoff*
*Last updated: May 2026*
