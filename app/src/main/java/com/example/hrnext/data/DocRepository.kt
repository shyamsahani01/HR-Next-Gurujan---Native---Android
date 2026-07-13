package com.example.hrnext.data

import com.example.hrnext.network.FrappeApi
import com.google.gson.Gson
import com.google.gson.JsonObject

/** Generic CRUD over `/api/resource/<doctype>` — works for any HRMS doctype. */
class DocRepository(private val api: FrappeApi) {

    private val gson = Gson()

    suspend fun list(
        doctype: String,
        fields: List<String>,
        filters: List<List<Any>>? = null,
        orFilters: List<List<Any>>? = null,
        searchText: String? = null,
        titleField: String? = null,
        orderBy: String = "modified desc",
        start: Int = 0,
        pageSize: Int = PAGE_SIZE,
    ): Result<List<JsonObject>> = runCatching {
        val fieldsJson = gson.toJson((fields + "name").distinct())
        val searchFilter = searchText?.takeIf { it.isNotBlank() }?.let { text ->
            val field = titleField ?: "name"
            listOf(field, "like", "%$text%")
        }
        val combinedFilters = filters.orEmpty() + listOfNotNull(searchFilter)
        val filtersJson = combinedFilters.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
        val orFiltersJson = orFilters?.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
        val response = api.getList(
            doctype = doctype,
            fields = fieldsJson,
            filters = filtersJson,
            orFilters = orFiltersJson,
            limit = pageSize,
            start = start,
            orderBy = orderBy,
        )
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
        response.body()?.getAsJsonArray("data")?.map { it.asJsonObject }.orEmpty()
    }

    suspend fun getDoc(doctype: String, name: String): Result<JsonObject> = runCatching {
        val response = api.getDoc(doctype, name)
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
        response.body()?.getAsJsonObject("data") ?: throw IllegalStateException("Empty response from server")
    }

    suspend fun create(doctype: String, body: JsonObject): Result<JsonObject> = runCatching {
        val response = api.createDoc(doctype, body)
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
        response.body()?.getAsJsonObject("data") ?: throw IllegalStateException("Empty response from server")
    }

    suspend fun update(doctype: String, name: String, body: JsonObject): Result<JsonObject> = runCatching {
        val response = api.updateDoc(doctype, name, body)
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
        response.body()?.getAsJsonObject("data") ?: throw IllegalStateException("Empty response from server")
    }

    suspend fun delete(doctype: String, name: String): Result<Unit> = runCatching {
        val response = api.deleteDoc(doctype, name)
        if (!response.isSuccessful) throw ApiException(response.code(), response.errorBody()?.string())
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
