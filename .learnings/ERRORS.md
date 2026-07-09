# Errors

Command failures and integration errors.

---


## [ERR-20260701-001] gradle-compile

**Logged**: 2026-07-01T08:45:00+08:00
**Priority**: low
**Status**: fixed
**Area**: android/kotlin

### Summary
assembleDebug failed because HomeFragment history click used h.subtitle outside the history loop scope.

### Fix
Use item subtitle only for display was wrong too; changed history click to pass the raw WatchHistoryItem subtitle via VideoCard.historySubtitle.

---


## [ERR-20260701-002] gradle-compile

**Logged**: 2026-07-01T09:13:00+08:00
**Priority**: low
**Status**: fixed
**Area**: android/kotlin

### Summary
CategoryFragment rewrite missed androidx.lifecycle.lifecycleScope import, causing compile failure.

### Fix
Add lifecycleScope import when converting Leanback BrowseSupportFragment to VerticalGridSupportFragment with coroutine loading.

---


## [ERR-20260701-003] suspend-call-in-non-suspend

**Logged**: 2026-07-01T09:31:00+08:00
**Priority**: low
**Status**: fixed
**Area**: android/kotlin

### Summary
HomeFragment.fetchHotVideos called SourceRepository.loadAllSites() from a non-suspend helper despite being invoked inside withContext.

### Fix
Mark helper as suspend (or call loadAllSites before entering non-suspend code).

---

