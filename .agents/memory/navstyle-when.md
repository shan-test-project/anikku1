---
name: NavStyle Exhaustive When
description: UpdatesTab uses an exhaustive when expression on NavStyle; adding a new enum entry requires updating UpdatesTab too
---

**Rule:** `UpdatesTab.kt` has an exhaustive `when (currentNavigationStyle())` block that assigns tab index. Adding a new `NavStyle` enum entry will cause a compile error unless the new case is also handled in `UpdatesTab.kt`.

**Why:** Kotlin `when` on sealed/enum without `else` is exhaustive — every case must be covered.

**How to apply:** After adding any new `NavStyle.MOVE_X_TO_MORE` entry, immediately add the corresponding branch in `UpdatesTab.kt`'s `when` block. `HistoryTab.kt` uses `else` so it does not need updating.
