## 2024-05-17 - Prevent Sensitive Data Logging in AI Clients
**Vulnerability:** The AI client implementation (`ProgramGenerationAiClient.kt`) was logging the complete prompt sent to the LLM as well as the complete raw JSON response using `Log.d`.
**Learning:** Even if data is not considered a high-level secret (like an API key), logging complete conversational turns, prompts, and large payloads from AI generation processes can unintentionally leak user-specific data, patterns, or PII into the system logcat. This data could then be exposed if logs are aggregated or accessed by other means.
**Prevention:** Avoid logging complete request/response payloads in production code, especially those interacting with third-party APIs or AI models where user context is included. Only log necessary metadata or error details needed for debugging without exposing the core data payload.

## 2024-05-18 - Prevent EncryptedSharedPreferences Backup Crashes
**Vulnerability:** The application was not explicitly excluding its `EncryptedSharedPreferences` files from Android device backups and transfers. When restoring to a new device, the keystore encryption keys are not transferred, resulting in app crashes on startup because the preferences cannot be decrypted.
**Learning:** `EncryptedSharedPreferences` relies on the Android Keystore system. Because keystore keys are device-specific and typically do not migrate across devices, backing up the encrypted preferences file without the corresponding keys leads to a fatal desynchronization.
**Prevention:** Always explicitly exclude files relying on device-specific Keystore encryption (like `LiftLabPreferencesEncrypted.xml`) in both `data_extraction_rules.xml` (for API 31+) and `backup_rules.xml` (for older devices).
