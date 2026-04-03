## 2024-04-03 - Missing contentDescriptions on Icon-only Buttons
**Learning:** Found a recurring pattern in custom composables (like `CustomKeyboard`, `ActionsMenu`, and `InputChipFlowRow`) where `IconButton` components containing `Icon`s are missing `contentDescription`s (often set to `null`). This makes these interactive elements inaccessible to screen readers.
**Action:** When creating or modifying custom composables with icon-only buttons, always ensure a meaningful `contentDescription` is provided via a `stringResource` to improve accessibility.
