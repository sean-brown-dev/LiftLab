package com.browntowndev.liftlab.ui.models

interface FlowRowFilterChipSection {
    val sectionName: String
    val filterChipOptions: Lazy<List<FilterChipOption>>
}