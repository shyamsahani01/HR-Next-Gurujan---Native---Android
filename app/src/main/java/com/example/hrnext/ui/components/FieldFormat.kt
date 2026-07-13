package com.example.hrnext.ui.components

import com.example.hrnext.model.DocField
import com.google.gson.JsonObject

/** Renders a raw Frappe field value as a short, human-friendly string for list rows and read-only views. */
fun formatFieldValue(doc: JsonObject, field: DocField): String {
    val element = doc.get(field.fieldname)
    if (element == null || element.isJsonNull) return "—"
    if (!element.isJsonPrimitive) return "—"

    return when (field.fieldtype) {
        "Check" -> if (runCatching { element.asInt }.getOrDefault(0) == 1) "Yes" else "No"
        "Currency", "Float", "Percent" -> runCatching {
            val value = element.asDouble
            "%,.2f".format(value)
        }.getOrDefault(element.asString)
        else -> element.asString
    }
}
