## 2024-05-17 - Prevent Sensitive Data Logging in AI Clients
**Vulnerability:** The AI client implementation (`ProgramGenerationAiClient.kt`) was logging the complete prompt sent to the LLM as well as the complete raw JSON response using `Log.d`.
**Learning:** Even if data is not considered a high-level secret (like an API key), logging complete conversational turns, prompts, and large payloads from AI generation processes can unintentionally leak user-specific data, patterns, or PII into the system logcat. This data could then be exposed if logs are aggregated or accessed by other means.
**Prevention:** Avoid logging complete request/response payloads in production code, especially those interacting with third-party APIs or AI models where user context is included. Only log necessary metadata or error details needed for debugging without exposing the core data payload.
## 2026-04-08 - [EncryptedSharedPreferences Backup Exclusion]
**Vulnerability:** EncryptedSharedPreferences (LiftLabPreferencesEncrypted) was not explicitly excluded from auto-backup and device transfer rules.
**Learning:** Backing up EncryptedSharedPreferences can cause crashes on app restore because the data is encrypted via Keystore, which is hardware-backed and device-specific. Restoring it to a different device renders it unreadable.
**Prevention:** Explicitly exclude the EncryptedSharedPreferences XML file in both `data_extraction_rules.xml` and `backup_rules.xml`.
