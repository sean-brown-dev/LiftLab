## 2024-05-18 - Missing Input Validation on Authentication
**Vulnerability:** The Create Account and Login endpoints passed raw input directly to Firebase Auth without local validation.
**Learning:** While Firebase Auth does server-side validation, lacking local input validation leads to unnecessary network requests and potentially unhandled error states if not caught appropriately. Furthermore, email inputs weren't trimmed which often causes unexpected authentication failures for users.
**Prevention:** Always validate and sanitize user inputs (e.g. trim email, validate email format, check for empty passwords) before sending to external services.
