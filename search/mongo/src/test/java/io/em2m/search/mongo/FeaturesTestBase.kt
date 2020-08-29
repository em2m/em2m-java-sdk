package io.em2m.search.mongo

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.em2m.geo.feature.Feature
import io.em2m.geo.feature.FeatureCollection
import io.em2m.geo.feature.FeatureCollectionHandler
import io.em2m.geo.geojson.GeoJsonParser
import io.em2m.search.core.model.FnIdMapper
import io.em2m.search.core.model.SyncDao
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.junit.Assert
import org.junit.Before
import java.io.FileInputStream
import java.util.*
import kotlin.properties.Delegates

open class FeaturesTestBase : Assert() {

    private var schemaMapper: SimpleSchemaMapper by Delegates.notNull()
    var syncDao: SyncDao<Feature> by Delegates.notNull()
    private val config: Config = ConfigFactory.load()
    private val idMapper = FnIdMapper<Feature>("id", { it.id!! }, { f, id -> f.id = id; f })


    @Before
    fun syncDaoSetup() {

        schemaMapper = SimpleSchemaMapper("test")
        schemaMapper.withMapping("properties.mag", Double::class.java)
        schemaMapper.withMapping("_id", String::class.java)
        schemaMapper.withMapping("properties.time", Date::class.java)


        // Mongo Database
        val mongoUri = config.getString("mongo.uri")
        val mongoDb = config.getString("mongo.db")
        val client = MongoClient(MongoClientURI(mongoUri))
        val database = client.getDatabase((mongoDb))
        val collection = database.getCollection("test")

        syncDao = MongoSyncDao(idMapper, JacksonDocumentMapper(Feature::class.java), collection, schemaMapper)

        (syncDao as MongoSyncDao).dropCollection()
        for (feature in earthquakes().features) {
            //feature.properties = feature.properties.filter { it.value != null }
            syncDao.save(feature.id!!, feature)
        }
    }


    private fun earthquakes(): FeatureCollection {
        val handler = FeatureCollectionHandler()
        val parser = GeoJsonParser()
        parser.handler(handler)
        parser.parse(FileInputStream("src/test/resources/earthquakes_2.5_day.geojson"))
        val result = handler.collection
        result.features.forEach {
            // it.properties["time"] = (it.properties["time"] as Long) * 1000
            // it.properties["updated"] = (it.properties["updated"] as Long) * 1000
            if (it.properties["alert"] == null) {
                it.properties.remove("alert")
            }
        }
        assertEquals(46, result.features.size)
        return result
    }

}