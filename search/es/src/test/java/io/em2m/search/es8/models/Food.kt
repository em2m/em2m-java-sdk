package io.em2m.search.es8.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class Food(val name: String,
                    @param:JsonProperty("satiety_score") val satietyScore: Double) {

    companion object {

        fun load(objectMapper: ObjectMapper = jacksonObjectMapper()): List<Food> {
            return GenericListLoader(
                "food.json",
                Food::class.java).load(objectMapper)
        }

    }

}

data class FoodCategory(val category: String, val items: List<Food>) {

    companion object {

        fun load(objectMapper: ObjectMapper = jacksonObjectMapper()): List<FoodCategory> {
            return GenericListLoader(
                "food_category.json",
                FoodCategory::class.java).load(objectMapper)
        }

    }

}
