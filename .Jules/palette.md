## 2024-05-14 - Add content descriptions to icon buttons
**Learning:** Found multiple instances where IconButtons containing `Icons.Default.MoreVert` (used in IconDropdown) and `Icons.AutoMirrored.Filled.ArrowBack` (used in menu items) lack proper `contentDescription`s.
**Action:** Adding meaningful content descriptions to these common navigation and interaction patterns makes the app more screen-reader friendly and follows best accessibility practices.
## 2024-05-18 - Missing Accessibility Labels on Icon-only Action Buttons
**Learning:** In Jetpack Compose, `Icon` composables used within `clickable` modifiers or `IconButton`s often have `contentDescription = null` set by default or omitted. This makes them entirely invisible or meaningless to screen readers like TalkBack, leading to a frustrating experience for visually impaired users.
**Action:** When implementing or reviewing icon-only interactive elements (like the skip rest timer button in `ProgressCountdownTimer`), always ensure a meaningful `contentDescription` is provided by adding a `stringResource` to `strings.xml` instead of passing `null`.
