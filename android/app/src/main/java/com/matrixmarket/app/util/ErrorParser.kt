package com.matrixmarket.app.util

import com.google.gson.JsonParser

// Turns an API error body into a readable sentence. The API returns
// { "message": "..." }; ASP.NET validation errors return { "title": ..., "errors": {...} }.
object ErrorParser {

    fun parse(rawBody: String?, httpCode: Int, fallback: String): String {
        if (rawBody.isNullOrBlank()) return friendlyForCode(httpCode, fallback)
        return try {
            val json = JsonParser.parseString(rawBody)
            when {
                json.isJsonObject -> {
                    val obj = json.asJsonObject
                    when {
                        obj.has("message") && obj.get("message").isJsonPrimitive -> obj.get("message").asString
                        obj.has("errors") && obj.get("errors").isJsonObject -> {
                            val first = obj.getAsJsonObject("errors").entrySet().firstOrNull()?.value
                            if (first != null && first.isJsonArray && first.asJsonArray.size() > 0) {
                                first.asJsonArray[0].asString
                            } else {
                                friendlyForCode(httpCode, fallback)
                            }
                        }
                        obj.has("title") && obj.get("title").isJsonPrimitive -> obj.get("title").asString
                        else -> friendlyForCode(httpCode, fallback)
                    }
                }
                json.isJsonPrimitive -> json.asString
                else -> friendlyForCode(httpCode, fallback)
            }
        } catch (e: Exception) {
            // Plain-text body (e.g. "Invalid email or password.")
            if (rawBody.length <= 200 && !rawBody.trimStart().startsWith("<")) rawBody.trim()
            else friendlyForCode(httpCode, fallback)
        }
    }

    private fun friendlyForCode(code: Int, fallback: String): String = when (code) {
        401 -> "Your session has expired. Please log in again."
        403 -> "You don't have permission to do that."
        404 -> "That item could not be found."
        in 500..599 -> "The server had a problem. Please try again in a moment."
        else -> fallback
    }
}
