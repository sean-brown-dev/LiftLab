package com.browntowndev.liftlab.core.common

interface FlowRowFilterChipSection {
    val sectionName: String
    val filterChipOptions: Lazy<List<FilterChipOption>>
}