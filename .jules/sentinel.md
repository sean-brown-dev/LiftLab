## 2024-05-17 - Prevent Sensitive Data Logging in AI Clients
**Vulnerability:** The AI client implementation (`ProgramGenerationAiClient.kt`) was logging the complete prompt sent to the LLM as well as the complete raw JSON response using `Log.d`.
**Learning:** Even if data is not considered a high-level secret (like an API key), logging complete conversational turns, prompts, and large payloads from AI generation processes can unintentionally leak user-specific data, patterns, or PII into the system logcat. This data could then be exposed if logs are aggregated or accessed by other means.
**Prevention:** Avoid logging complete request/response payloads in production code, especially those interacting with third-party APIs or AI models where user context is included. Only log necessary metadata or error details needed for debugging without exposing the core data payload.
## 2025-04-16 - Prevent Keystore Desync Crashes on Restore
**Vulnerability:** Encrypted SharedPreferences file backed up and restored to new devices without its KeyStore dependencies.
**Learning:** `EncryptedSharedPreferences` relies on keys securely stored in the Android Keystore. These keys are deliberately not included in backups. If the preferences XML is backed up and restored to a new device without the key, the app crashes when attempting to read the corrupted data.
**Prevention:** Explicitly exclude the encrypted file (e.g. `LiftLabPreferencesEncrypted.xml`) from both cloud backups and device transfers in `data_extraction_rules.xml` (API 31+) and `backup_rules.xml` (legacy).
