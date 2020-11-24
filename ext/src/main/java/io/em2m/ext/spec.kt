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
package io.em2m.sdk.ext

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import io.em2m.simplex.model.ConditionExpr

data class ExtensionSpec(
        val id: String? = null,
        val ref: String? = null,
        val description: String? = null,
        val priority: Int = 0,
        val data: Map<String, Any?> = emptyMap(),
        @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED])
        val target: List<String> = emptyList(),
        val filter: ConditionExpr? = null)

data class BundleSpec(
        val id: String,
        val description: String? = null,
        val filter: ConditionExpr? = null,
        val extensions: Map<String, List<ExtensionSpec>> = emptyMap())
