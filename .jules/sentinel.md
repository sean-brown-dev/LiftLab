## 2026-04-06 - [Added Input Validation for Firebase Auth]
**Vulnerability:** Missing local input validation (email format, empty password) for account creation and login before passing data to Firebase Auth.
**Learning:** Sending unsanitized/unvalidated input directly to external services creates unnecessary network requests and unhandled error potential.
**Prevention:** Always trim and validate format locally (e.g., android.util.Patterns.EMAIL_ADDRESS) before external API calls.
