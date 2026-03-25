package com.browntowndev.liftlab.ui.models.controls

interface FlowRowFilterChipSection {
    val sectionName: String
    val filterChipOptions: Lazy<List<FilterChipOption>>
}