package com.example.hrnext.network

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Thin wrapper over Frappe's REST API (`/api/method/...` and `/api/resource/...`). Doc payloads
 * are kept as raw [JsonObject] rather than fixed data classes because the same endpoints serve
 * every HRMS doctype, each with a different, server-defined field set.
 */
interface FrappeApi {

    @FormUrlEncoded
    @POST("api/method/login")
    suspend fun login(@Field("usr") usr: String, @Field("pwd") pwd: String): Response<JsonObject>

    @POST("api/method/logout")
    suspend fun logout(): Response<JsonObject>

    @GET("api/resource/User/{name}")
    suspend fun getUser(@Path("name") name: String, @Query("fields") fields: String): Response<JsonObject>

    @GET("api/resource/DocType/{name}")
    suspend fun getDocTypeMeta(@Path("name") name: String): Response<JsonObject>

    @GET("api/resource/DocType")
    suspend fun listDocTypes(
        @Query("filters") filters: String,
        @Query("fields") fields: String,
        @Query("limit_page_length") limit: Int = 0,
    ): Response<JsonObject>

    @GET("api/resource/Workspace")
    suspend fun listWorkspaces(
        @Query("filters") filters: String,
        @Query("fields") fields: String,
        @Query("limit_page_length") limit: Int = 0,
    ): Response<JsonObject>

    @GET("api/resource/{doctype}")
    suspend fun getList(
        @Path("doctype") doctype: String,
        @Query("fields") fields: String,
        @Query("filters") filters: String? = null,
        @Query("or_filters") orFilters: String? = null,
        @Query("limit_page_length") limit: Int = 20,
        @Query("limit_start") start: Int = 0,
        @Query("order_by") orderBy: String = "modified desc",
    ): Response<JsonObject>

    @GET("api/resource/{doctype}/{name}")
    suspend fun getDoc(@Path("doctype") doctype: String, @Path("name") name: String): Response<JsonObject>

    @Headers("Content-Type: application/json")
    @POST("api/resource/{doctype}")
    suspend fun createDoc(@Path("doctype") doctype: String, @Body body: JsonObject): Response<JsonObject>

    @Headers("Content-Type: application/json")
    @PUT("api/resource/{doctype}/{name}")
    suspend fun updateDoc(
        @Path("doctype") doctype: String,
        @Path("name") name: String,
        @Body body: JsonObject,
    ): Response<JsonObject>

    @DELETE("api/resource/{doctype}/{name}")
    suspend fun deleteDoc(@Path("doctype") doctype: String, @Path("name") name: String): Response<JsonObject>
}
