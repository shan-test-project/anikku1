---
name: Preference collectAsState Pattern
description: How to observe Preference<T> as Compose State in this codebase
---

**Rule:** To observe a `Preference<T>` object as Compose state, use `.changes().collectAsState(initial = pref.get())` — NOT `.collectAsState()` directly on the Preference.

**Why:** The standard `androidx.compose.runtime.collectAsState` only has overloads for `StateFlow<T>` and `Flow<T>` with an initial value. Calling `.collectAsState()` directly on a `Preference<T>` causes a compiler error: "Cannot infer type for type parameter 'T'" / "receiver type mismatch".

**How to apply:**
```kotlin
val myPref = prefs.somePreference()
val myValue by myPref.changes().collectAsState(initial = myPref.get())
```

Alternatively, import `tachiyomi.presentation.core.util.collectAsState` which provides a Preference-specific overload used in screens like HomeScreen:
```kotlin
import tachiyomi.presentation.core.util.collectAsState
val myValue by prefs.somePreference().collectAsState()
```

The `.changes()` approach is safer since it avoids import ambiguity in screens that also import `androidx.compose.runtime.collectAsState` for Flow collection.
