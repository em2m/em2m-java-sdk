package io.em2m.search.mongo

import com.mongodb.ConnectionString
import com.mongodb.async.client.MongoClientSettings
import com.mongodb.connection.ClusterSettings
import com.mongodb.rx.client.MongoClients
import com.scaleset.geo.Feature
import com.scaleset.geo.FeatureCollection
import com.scaleset.geo.FeatureCollectionHandler
import com.scaleset.geo.geojson.GeoJsonParser
import com.typesafe.config.ConfigFactory
import io.em2m.search.core.model.IdMapper
import io.em2m.search.core.model.SearchDao
import io.em2m.search.core.parser.SimpleSchemaMapper
import org.junit.Assert
import org.junit.Before
import java.io.FileInputStream
import kotlin.properties.Delegates

open class FeaturesTestBase : Assert() {

    private var schemaMapper: SimpleSchemaMapper by Delegates.notNull()
    var searchDao: SearchDao<Feature> by Delegates.notNull()
    val config = ConfigFactory.load()

    @Before
    @Throws(Exception::class)
    fun before() {
        schemaMapper = SimpleSchemaMapper("test")
        schemaMapper.withMapping("properties.mag", Double::class.java)
        schemaMapper.withMapping("_id", String::class.java)


        // Mongo Database
        val mongoUri = config.getString("mongo.uri")
        val mongoDb = config.getString("mongo.db")
        val settings = MongoClientSettings.builder()
                .clusterSettings(ClusterSettings.builder().applyConnectionString(ConnectionString(mongoUri)).build())
                .build()
        val client = MongoClients.create(settings)
        val database = client.getDatabase((mongoDb))
        val collection = database.getCollection("test")

        searchDao = MongoSearchDao(FeatureIdMapper(), JacksonDocumentMapper(Feature::class.java), collection, schemaMapper)
        (searchDao as MongoSearchDao).dropCollection().toBlocking().first()

        for (feature in earthquakes().features) {
            //feature.properties = feature.properties.filter { it.value != null }
            feature.properties.remove("alert", null)
            searchDao.save(feature.id, feature).toBlocking().first()
        }
    }

    fun earthquakes(): FeatureCollection {
        val handler = FeatureCollectionHandler()
        val parser = GeoJsonParser()
        parser.handler(handler)
        parser.parse(FileInputStream("src/test/resources/earthquakes_2.5_day.geojson"))
        val result = handler.collection
        Assert.assertEquals(46, result.features.size)
        return result
    }

    class FeatureIdMapper : IdMapper<Feature> {

        override val idField = "_id"

        override fun getId(obj: Feature): String {
            return obj.id
        }

        override fun setId(obj: Feature, id: String): Feature {
            obj.id = id
            return obj
        }
    }

}