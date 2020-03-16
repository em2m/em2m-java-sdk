/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________
 *
 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.
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
package io.em2m.search.bean

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.IOException
import java.util.*
import java.util.function.Consumer

data class Movie(val id: String, val type: String? = null, val fields: Map<String, Any> = emptyMap()) {

    companion object {
        @Throws(IOException::class)
        fun load(): MutableMap<String, Movie> {
            val mapper = jacksonObjectMapper()
            val listType: JavaType = mapper.typeFactory.constructCollectionType(MutableList::class.java, Movie::class.java)
            val movies = mapper.readValue<List<Movie>>(Movie::class.java.getResourceAsStream("/moviedata2.json"), listType)
            val result: MutableMap<String, Movie> = HashMap()
            movies.forEach(Consumer { movie: Movie -> result[movie.id] = movie })
            return result
        }
    }
}