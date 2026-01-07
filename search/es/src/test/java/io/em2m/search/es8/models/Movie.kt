package io.em2m.search.es8.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.core.model.IdMapper
import java.util.*

data class MovieFields(val directors: List<String>,
                       val release_date: Date,
                       val rating: Double,
                       val genres: List<String>,
                       val image_url: String,
                       val plot: String,
                       val title: String,
                       val rank: Int,
                       val running_time_secs: Int,
                       val actors: List<String>,
                       val year: Int)

data class Movie(var id: String, val type: String, val fields: MovieFields) {

    companion object {

        fun load(objectMapper: ObjectMapper = jacksonObjectMapper()): List<Movie> {
            return GenericListLoader(
                "movie.json",
                Movie::class.java).load(objectMapper)
        }

    }

}

class MovieIdMapper : IdMapper<Movie> {
    override val idField: String
        get() = "id"

    override fun getId(obj: Movie): String {
        return obj.id
    }

    override fun setId(
        obj: Movie,
        id: String
    ): Movie {
        return obj.apply { this.id = id }
    }
}
