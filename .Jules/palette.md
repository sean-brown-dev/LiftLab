## 2024-05-14 - Add content descriptions to icon buttons
**Learning:** Found multiple instances where IconButtons containing `Icons.Default.MoreVert` (used in IconDropdown) and `Icons.AutoMirrored.Filled.ArrowBack` (used in menu items) lack proper `contentDescription`s.
**Action:** Adding meaningful content descriptions to these common navigation and interaction patterns makes the app more screen-reader friendly and follows best accessibility practices.
