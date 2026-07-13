package com.example.hrnext.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"

    const val DOC_LIST = "doclist/{doctype}"
    const val DOC_DETAIL = "docdetail/{doctype}/{name}"

    /** Sentinel used in place of a real docname on the detail route when creating a new record. */
    const val NEW_DOC_NAME = "__new__"

    fun docList(doctype: String) = "doclist/${encode(doctype)}"
    fun docDetail(doctype: String, name: String) = "docdetail/${encode(doctype)}/${encode(name)}"
    fun docCreate(doctype: String) = docDetail(doctype, NEW_DOC_NAME)

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")
}
