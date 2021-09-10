package io.em2m.geo.geojson

import io.em2m.geo.math.TileCoordinate
import io.em2m.geo.math.TileMath
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TileCoordTest {

    private val tm = TileMath.std256

    @Test
    fun testPath() {
        val coord = TileCoordinate(4653, 10129, 14)
        println("Z,X,Y: ${coord.z},${coord.x},${coord.y}")
        val code = coord.toCode()
        println("Path: $code")
        assertNotNull(code)
        val coord2 = TileCoordinate.fromCode(code)
        assertEquals(coord, coord2)
        println("Bbox: ${coord2.bboxLngLat()}")
    }

    @Test
    fun testCoordinates() {
        val coordinate = Coordinate(-77.364375, 38.9555)
        val tileCoordinate = tm.lngLatToTileCoordinate(coordinate, 20)
        val code = tileCoordinate.toCode()
        val codeSet = tileCoordinate.toCodeSet()
        val bbox = tileCoordinate.bboxLngLat()
        println("Coord: $coordinate")
        println("Tile: $tileCoordinate")
        println("Code: $code")
        println("CodeSet: $codeSet")
        println("Bbox(LL): $bbox")
    }

    @Test
    fun testDecode() {
        val code = "6"
        val tileCoordinate = TileCoordinate.fromCode(code)
        println("Code: $code")
        println("Tile: $tileCoordinate")
        println("Bbox: ${tileCoordinate.bboxLngLat()}")
    }

}
