package com.example.hrnext.model

data class DocTypeMeta(
    val name: String,
    val module: String,
    val titleField: String?,
    val fields: List<DocField>,
) {
    val listViewFields: List<DocField>
        get() = fields.filter { it.inListView && !it.hidden && !it.isLayoutOnly }
            .ifEmpty { fields.filter { !it.hidden && !it.isLayoutOnly }.take(3) }
}
