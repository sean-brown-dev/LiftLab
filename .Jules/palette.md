## 2024-05-14 - Add content descriptions to icon buttons
**Learning:** Found multiple instances where IconButtons containing `Icons.Default.MoreVert` (used in IconDropdown) and `Icons.AutoMirrored.Filled.ArrowBack` (used in menu items) lack proper `contentDescription`s.
**Action:** Adding meaningful content descriptions to these common navigation and interaction patterns makes the app more screen-reader friendly and follows best accessibility practices.

## 2024-05-24 - Accessibility for Icon-Only Buttons
**Learning:** Found that some icon-only interactive elements (like the skip button in `ProgressCountdownTimer` and the close button in `RowMultiSelect`) had `contentDescription = null`. This prevents screen readers from announcing their purpose, reducing accessibility.
**Action:** When adding new icon-only interactive elements (e.g., buttons, clickable icons), ensure they have a meaningful `contentDescription` using `stringResource`. Only use `null` for decorative icons or when the text adjacent to the icon fully describes the action.
