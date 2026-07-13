package com.example.hrnext.model

data class ModuleItem(
    val doctype: String,
    val label: String,
)

data class ModuleSection(
    val title: String,
    val items: List<ModuleItem>,
)
