## 2025-04-05 - Missing Content Descriptions on Action Icons
**Learning:** Icon-only interactive elements, such as the cancellation button on the `ProgressCountdownTimer`, occasionally lack `contentDescription`s (set to `null`), preventing screen readers from identifying their purpose.
**Action:** Always verify `contentDescription` on interactive components and add a string resource (e.g., `accessibility_cancel_timer`) to ensure consistent screen reader accessibility in Compose views.
