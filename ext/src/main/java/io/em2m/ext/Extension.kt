/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________
 *
 * Copyright (c) 2013-2020 Elastic M2M Incorporated, All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated
 *
 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.ext

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.em2m.simplex.model.Expr
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import javax.inject.Singleton

@Singleton
data class Extension(
    val bundle: String,
    val id: String? = null,
    val type: String,
    val filter: Expr? = null,
    val ref: String? = null,
    val priority: Int = 0,
    val target: List<String> = emptyList(),
    val data: Map<String, Any?> = emptyMap(),
    val dir: File) {

    var instance: Any? = null

    fun filter(context: Map<String, Any?>): Boolean {
        return when (val value = filter?.call(context) ?: true) {
            is Boolean -> value
            is String -> value.isNotBlank()
            is Number -> (value != 0) || (value != Double.NaN)
            else -> true
        }
    }

    fun <T> instance(creator: (Extension) -> T?): T? {
        if (instance == null) {
            instance = creator(this)
        }
        @Suppress("UNCHECKED_CAST")
        return instance as? T
    }

    fun <T> instance(mapper: ObjectMapper, type: Class<T>): T? {
        return try {
            val file = refFile()
            if (file?.exists() == true) {
                val tree = ymlMapper.readTree(file) as ObjectNode
                val id = id ?: file.nameWithoutExtension
                tree.put("id", id)
                mapper.convertValue(tree, type)
            } else {
                val tree = data
                    .plus("id" to id)
                    .plus("priority" to priority)
                mapper.convertValue(tree, type)
            }
        } catch (ex: Exception) {
            val label = if (id != null) {
                "id = ${id}"
            } else if (ref != null) {
                "ref = ${ref}"
            } else ""
            log.error("Error attempting to read extension ($label), messages = ${ex.message}")
            null
        }
    }

    fun refFile(): File? {
        return if (ref != null) {
            dir.resolve(ref)
        } else null
    }

    fun resource(path: String): URL? {
        val file = dir.resolve(path)
        return if (file.exists()) file.toURI().toURL() else null
    }

    companion object {
        val log = LoggerFactory.getLogger(Extension::class.java)
        private val ymlMapper = ObjectMapper(YAMLFactory())
    }

}
