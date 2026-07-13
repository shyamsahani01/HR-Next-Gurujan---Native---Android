package com.example.hrnext.data

import com.example.hrnext.model.DocField
import com.example.hrnext.model.DocTypeMeta
import com.example.hrnext.model.ModuleItem
import com.example.hrnext.model.ModuleSection
import com.example.hrnext.network.FrappeApi
import com.example.hrnext.util.boolOrFalse
import com.example.hrnext.util.orEmptyElements
import com.example.hrnext.util.stringOrNull

/** HR/Payroll are the modules the Frappe HRMS app registers its doctypes under. */
private val HRMS_MODULES = """["HR","Payroll"]"""

/** Whole dashboard sections hidden per product decision — not relevant to this app's users. */
private val EXCLUDED_SECTIONS = setOf(
    "fleet management", "setup", "settings",
    "training", "daily work summary", "onboarding", "exemption",
    "tax setup", "taxation", "benefit", "benefits",
    "overtime", "shift", "shifts", "time", "time tracking", "timesheet",
    "appointment", "appointments", "master", "masters",
    "allocation", "allocations", "travel", "incentive", "incentives",
    "accounting", "job", "jobs", "interview", "interviews",
)

/** Individual doctypes hidden from every section even when their parent section stays visible. */
private val EXCLUDED_DOCTYPES = setOf(
    "expense claim type",
    "payment entry",
    "journal entry",
    "additional salary",
    "compensatory leave request",
)

/**
 * Discovers "every HRMS doctype" by reading Frappe's own metadata rather than a hardcoded list:
 * Workspace docs (the same data that drives the HR desk sidebar) group doctypes under real
 * section headings; if that's unavailable for any reason we fall back to a flat DocType query.
 */
class MetaRepository(private val api: FrappeApi) {

    private val docTypeMetaCache = mutableMapOf<String, DocTypeMeta?>()

    suspend fun fetchDashboardSections(): List<ModuleSection> {
        val fromWorkspaces = runCatching { fetchFromWorkspaces() }.getOrNull().orEmpty()
        val sections = fromWorkspaces.ifEmpty { runCatching { fetchFromDocTypeModules() }.getOrDefault(emptyList()) }
        return filterHiddenModules(sections)
    }

    private fun filterHiddenModules(sections: List<ModuleSection>): List<ModuleSection> =
        sections
            .filter { it.title.trim().lowercase() !in EXCLUDED_SECTIONS }
            .map { section -> section.copy(items = section.items.filter { it.label.trim().lowercase() !in EXCLUDED_DOCTYPES && it.doctype.trim().lowercase() !in EXCLUDED_DOCTYPES }) }
            .filter { it.items.isNotEmpty() }
            .mergeDuplicateTitles()

    /** Different Frappe workspaces can legitimately contribute a card-break section with the same
     * label (e.g. two "Reports" groups) — combine those into one so the same heading never repeats. */
    private fun List<ModuleSection>.mergeDuplicateTitles(): List<ModuleSection> {
        val order = LinkedHashMap<String, ModuleSection>()
        for (section in this) {
            val key = section.title.trim().lowercase()
            val existing = order[key]
            order[key] = if (existing == null) section else existing.copy(items = existing.items + section.items)
        }
        return order.values.toList()
    }

    suspend fun fetchDocTypeMeta(doctype: String): DocTypeMeta? {
        docTypeMetaCache[doctype]?.let { return it }
        val result = runCatching { loadDocTypeMeta(doctype) }.getOrNull()
        docTypeMetaCache[doctype] = result
        return result
    }

    private suspend fun fetchFromWorkspaces(): List<ModuleSection> {
        val listResponse = api.listWorkspaces(
            filters = """[["module","in",$HRMS_MODULES]]""",
            fields = """["name"]""",
        )
        if (!listResponse.isSuccessful) return emptyList()
        val names = listResponse.body()?.getAsJsonArray("data")
            ?.mapNotNull { it.asJsonObject.get("name")?.asString }
            .orEmpty()
        if (names.isEmpty()) return emptyList()

        val sections = mutableListOf<ModuleSection>()
        val seenDoctypes = mutableSetOf<String>()

        for (workspaceName in names) {
            val docResponse = runCatching { api.getDoc("Workspace", workspaceName) }.getOrNull() ?: continue
            if (!docResponse.isSuccessful) continue
            val data = docResponse.body()?.getAsJsonObject("data") ?: continue
            val links = data.getAsJsonArray("links") ?: continue

            var currentTitle: String? = null
            var currentItems = mutableListOf<ModuleItem>()

            fun flush() {
                val title = currentTitle
                if (title != null && currentItems.isNotEmpty()) {
                    sections += ModuleSection(title, currentItems.toList())
                }
                currentItems = mutableListOf()
            }

            for (element in links) {
                val row = element.asJsonObject
                when (row.stringOrNull("type")) {
                    "Card Break" -> {
                        flush()
                        currentTitle = row.stringOrNull("label") ?: "More"
                    }
                    "Link" -> {
                        val linkType = row.stringOrNull("link_type")
                        val linkTo = row.stringOrNull("link_to")
                        if (linkType == "DocType" && !linkTo.isNullOrBlank() && seenDoctypes.add(linkTo)) {
                            currentItems += ModuleItem(doctype = linkTo, label = row.stringOrNull("label") ?: linkTo)
                        }
                    }
                }
            }
            flush()
        }
        return sections.filter { it.items.isNotEmpty() }
    }

    private suspend fun fetchFromDocTypeModules(): List<ModuleSection> {
        val response = api.listDocTypes(
            filters = """[["module","in",$HRMS_MODULES],["istable","=",0],["issingle","=",0]]""",
            fields = """["name","module"]""",
        )
        if (!response.isSuccessful) return emptyList()
        val rows = response.body()?.getAsJsonArray("data")?.map { it.asJsonObject }.orEmpty()
        return rows.groupBy { it.stringOrNull("module") ?: "Other" }
            .toSortedMap()
            .map { (module, items) ->
                ModuleSection(
                    title = module,
                    items = items.mapNotNull { obj -> obj.stringOrNull("name")?.let { ModuleItem(it, it) } }
                        .sortedBy { it.label },
                )
            }
    }

    private suspend fun loadDocTypeMeta(doctype: String): DocTypeMeta? {
        val response = api.getDocTypeMeta(doctype)
        if (!response.isSuccessful) return null
        val data = response.body()?.getAsJsonObject("data") ?: return null
        val fields = data.getAsJsonArray("fields").orEmptyElements().mapNotNull { element ->
            val f = element.asJsonObject
            val fieldname = f.stringOrNull("fieldname") ?: return@mapNotNull null
            DocField(
                fieldname = fieldname,
                label = f.stringOrNull("label") ?: fieldname,
                fieldtype = f.stringOrNull("fieldtype") ?: "Data",
                options = f.stringOrNull("options"),
                reqd = f.boolOrFalse("reqd"),
                inListView = f.boolOrFalse("in_list_view"),
                hidden = f.boolOrFalse("hidden"),
                readOnly = f.boolOrFalse("read_only"),
            )
        }
        return DocTypeMeta(
            name = data.stringOrNull("name") ?: doctype,
            module = data.stringOrNull("module") ?: "",
            titleField = data.stringOrNull("title_field"),
            fields = fields,
        )
    }
}
