package com.example.hrnext.model

data class DocField(
    val fieldname: String,
    val label: String,
    val fieldtype: String,
    val options: String?,
    val reqd: Boolean,
    val inListView: Boolean,
    val hidden: Boolean,
    val readOnly: Boolean,
) {
    val isLayoutOnly: Boolean
        get() = fieldtype in setOf("Section Break", "Column Break", "Tab Break")
}
