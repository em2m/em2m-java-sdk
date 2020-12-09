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

import io.em2m.simplex.model.Expr
import java.io.File


data class Bundle(
        val id: String,
        val filter: Expr? = null,
        val dir: File,
        val extensions: List<Extension>) {

    fun findExtension(type: String): List<Extension> {
        return extensions
            .filter { it.type == type }
    }

    fun findExtension(type: String, context: Map<String, Any?> = emptyMap()): List<Extension> {
        return extensions
                .filter { it.type == type }
                .filter { it.filter(context) }
    }

    fun findExtension(predicate: (Extension) -> Boolean, context: Map<String, Any?> = emptyMap()): List<Extension> {
        return extensions
                .filter { predicate(it) }
                .filter { it.filter(context) }
    }

    fun filter(context: Map<String, Any?>): Boolean {
        val value = try {
            filter?.call(context) ?: true
        } catch (ex: Exception) {
            false
        }

        return when (value) {
            is Boolean -> value
            is String -> value.isNotBlank()
            is Number -> (value != 0) || (value != Double.NaN)
            else -> true
        }
    }

}
