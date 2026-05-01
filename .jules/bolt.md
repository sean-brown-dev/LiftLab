## 2024-05-18 - Compose fastMap usage
**Learning:** While `fastMap` is an excellent optimization for reducing iterator allocations during UI state mapping in Compose, replacing standard `.map` calls across multiple files requires careful attention to imports. Omitting `import androidx.compose.ui.util.fastMap` will result in unresolved reference errors during compilation.
**Action:** Always verify `fastMap` replacements with `git diff` to ensure the import is present, and confirm compilation via `assembleDebug` or `test` tasks.
