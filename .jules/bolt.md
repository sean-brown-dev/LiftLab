## 2026-04-06 - [Kotlin Collection Memory Profiling]
**Learning:** Found an anti-pattern in `ChartModel.kt` where intermediate list allocation from chained calls (`.filter {}.size` and `.map {}.sum()`) occurred inside mapping functions of chart generation causing memory bloat and GC pauses.
**Action:** Always prefer combined operations like `.count { }` and `.sumOf { }` to iterate only once without creating intermediate collections.
