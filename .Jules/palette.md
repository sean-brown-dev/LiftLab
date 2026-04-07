## 2024-05-14 - Add content descriptions to icon buttons
**Learning:** Found multiple instances where IconButtons containing `Icons.Default.MoreVert` (used in IconDropdown) and `Icons.AutoMirrored.Filled.ArrowBack` (used in menu items) lack proper `contentDescription`s.
**Action:** Adding meaningful content descriptions to these common navigation and interaction patterns makes the app more screen-reader friendly and follows best accessibility practices.

## 2024-05-18 - Missing Accessibility Labels on Clickable Icons
**Learning:** Found that `Icon` components with a `Modifier.clickable` block were missing their `contentDescription` property. Even if they are just icons, when they are clickable, they act as buttons and require an ARIA equivalent (`contentDescription` in Android) so screen readers can interpret them properly. In this case, `ProgressCountdownTimer` had a skip icon acting as a button.
**Action:** When creating or auditing clickable custom elements, always ensure `contentDescription` is set using `stringResource` when the component acts interactively.
