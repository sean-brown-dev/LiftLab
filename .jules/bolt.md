## 2024-05-24 - Intermediate List Allocation in Kotlin
**Learning:** Found instances of `.map { ... }.sum()` which allocates an intermediate list before summing, causing unnecessary O(N) memory allocation and an extra iteration pass.
**Action:** Always replace `.map { ... }.sum()` with `.sumOf { ... }` when possible. For `Float` values, since `sumOf` doesn't directly support `Float`, map to `Double` via `.sumOf { it.toDouble() }.toFloat()`.
