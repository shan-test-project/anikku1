---
name: Weekly Airing Schedule feature
description: How the AiringSchedule tab is wired into the app — navigation, DI, settings, and API.
---

## Location
`app/src/main/java/mihon/feature/airingschedule/`

## Wiring

- **Tab in nav**: Added `AiringScheduleTab` to `NavStyle.kt` `tabs` list (between HistoryTab and BrowseTab at index 3).
- **DI**: `SchedulePreferences(get())` registered in `PreferenceModule.kt` via `addSingletonFactory`.
- **Settings entry**: Added to `SettingsMainScreen.kt` items list using `Icons.Outlined.CalendarMonth`, routes to `SettingsScheduleScreen`.
- **API**: `AiringScheduleRepository` uses `networkHelper.client` (unauthenticated OkHttp) + `injectLazy()` pattern. AniList GraphQL endpoint: `https://graphql.anilist.co/`.

## Key decisions
- Tab is self-contained (`AiringScheduleTab.kt`) — no separate Screen file needed.
- `GetExtensionsByType` is already registered in `DomainModule.kt` — usable directly in `SettingsScheduleScreen`.
- Empty state uses `tachiyomi.i18n.MR.strings.information_no_airing_today` (added to strings.xml).

**Why:** Self-contained tab avoids duplication. Unauthenticated AniList API is public — no OAuth needed for schedule data.
