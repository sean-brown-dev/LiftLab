## 2024-05-18 - [Compose Recomposition Optimization]
**Learning:** Frequent timer updates (e.g. `timerState.time` emitting every second) read in a parent composable will cause the entire screen and all unstable child components (like lists of complex models) to recompose unnecessarily. Also, using `LaunchedEffect` to copy passed-in properties to local `mutableStateOf` just for `Text` is an anti-pattern that triggers extra composition phases.
**Action:** Unbox frequent state updates into lambdas (e.g., `durationProvider: () -> String`) and pass the lambdas to child composables. This defers state reading to the specific component that needs it, skipping recomposition for the heavy parent and sibling lists. Use `derivedStateOf` to observe specific sub-properties like `running` without recomposing on every tick. Compute values inline instead of using `LaunchedEffect` syncing where possible.

## 2024-05-18 - [List Allocation Optimization]
**Learning:** Using `.filter { condition }.size` on collections results in the allocation of an unnecessary intermediate `ArrayList` simply to count matching elements.
**Action:** Always replace `.filter { condition }.size` with `.count { condition }` to iterate the collection exactly once and eliminate the intermediate memory allocation.
