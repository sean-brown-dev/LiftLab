## 2024-05-17 - Prevent Sensitive Data Logging in AI Clients
**Vulnerability:** The AI client implementation (`ProgramGenerationAiClient.kt`) was logging the complete prompt sent to the LLM as well as the complete raw JSON response using `Log.d`.
**Learning:** Even if data is not considered a high-level secret (like an API key), logging complete conversational turns, prompts, and large payloads from AI generation processes can unintentionally leak user-specific data, patterns, or PII into the system logcat. This data could then be exposed if logs are aggregated or accessed by other means.
**Prevention:** Avoid logging complete request/response payloads in production code, especially those interacting with third-party APIs or AI models where user context is included. Only log necessary metadata or error details needed for debugging without exposing the core data payload.

## 2024-05-24 - Exception Logging Data Leakage
**Vulnerability:** Logging exception objects (e.g., `SerializationException`) from external APIs/AI models directly to Android Logcat.
**Learning:** Serializing and logging full exception objects can unintentionally leak raw JSON payloads or sensitive contextual data present in the exception message or stack trace.
**Prevention:** Only log aggregate data, specific identifiers, or generic error messages to standard output and Crashlytics. Avoid passing the raw exception object (`it`) to `Log.e` when the exception may contain sensitive deserialization data.
