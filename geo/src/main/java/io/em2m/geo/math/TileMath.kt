package io.em2m.geo.math

import io.em2m.geo.math.WebMapMath.Companion.lngLatToMeters
import io.em2m.geo.math.WebMapMath.Companion.metersToLngLat
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

import java.awt.geom.AffineTransform

class TileMath
/**
 * Construct a new projection with
 *
 * @param tileSize The size of a square tile in pixels
 */
@JvmOverloads constructor(private val tileSize: Int = 256) {

    // 156543.03392804062 for tileSize 256 Pixels
    private val initialResolution: Double = 2.0 * Math.PI * 6378137.0 / tileSize

    // 20037508.342789244
    private val originShift: Double = 2.0 * Math.PI * 6378137.0 / 2.0

    // -20037508.342789244, 20037508.342789244
    private val topLeft = Coordinate(-originShift, originShift)


    /**
     * Returns the tile coordinate of the LngLat coordinate
     *
     * @param coord     The LngLat coordinate
     * @param zoomLevel
     * @return
     */
    fun lngLatToTile(coord: Coordinate, zoomLevel: Int): Coordinate {
        return metersToTile(lngLatToMeters(coord), zoomLevel)
    }

    fun matrixSize(zoomLevel: Int): Int {
        return 1 shl zoomLevel
    }


    /**
     * Converts EPSG:3857 to pyramid pixel coordinates in given zoom level
     *
     * @param mx        the X coordinate in meters
     * @param my        the Y coordinate in meters
     * @param zoomLevel the zoom level
     * @return pixel coordinates for the coordinate
     */
    fun metersToPixels(mx: Double, my: Double, zoomLevel: Int): Coordinate {
        val res = resolution(zoomLevel)

        val px = (mx + originShift) / res
        val py = (my + originShift) / res

        return Coordinate(px, py)
    }

    /**
     * Create a transform that converts meters to tile-relative pixels
     *
     * @param tx        The x coordinate of the tile
     * @param ty        The y coordinate of the tile
     * @param zoomLevel the zoom level
     * @return AffineTransform with meters to pixels transformation
     */
    fun metersToTilePixelsTransform(tx: Int, ty: Int, zoomLevel: Int): AffineTransform {
        val result = AffineTransform()
        val scale = 1.0 / resolution(zoomLevel)
        val nTiles = 2 shl zoomLevel - 1
        val px = tx * -256
        val py = (nTiles - ty) * -256
        // flip y for upper-left origin
        result.scale(1.0, -1.0)
        result.translate(px.toDouble(), py.toDouble())
        result.scale(scale, scale)
        result.translate(originShift, originShift)
        return result
    }


    /**
     * Returns the tile coordinate of the meters coordinate
     */
    fun metersToTile(coord: Coordinate, zoomLevel: Int): Coordinate {
        val pixels = metersToPixels(coord.x, coord.y, zoomLevel)
        val tx = pixels.x.toInt() / 256
        val ty = pixels.y.toInt() / 256
        return Coordinate(tx.toDouble(), ty.toDouble(), zoomLevel.toDouble())
    }

    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:3857
     *
     * @param px        the X pixel coordinate
     * @param py        the Y pixel coordinate
     * @param zoomLevel the zoom level
     * @return The coordinate transformed to EPSG:3857
     */
    fun pixelsToMeters(px: Double, py: Double, zoomLevel: Int): Coordinate {
        val res = resolution(zoomLevel)
        val mx = px * res - originShift
        val my = -py * res + originShift

        return Coordinate(mx, my)
    }

    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     *
     * @param zoomLevel the zoom level
     * @return the resolution for the given zoom level
     */
    fun resolution(zoomLevel: Int): Double {
        return initialResolution / matrixSize(zoomLevel)
    }

    /**
     * Compute the scale denominator of the resolution in degrees / pixel
     *
     * @param zoomLevel The zoom level
     * @return The scale denominator for the given zoom level
     */
    fun scaleDenominator(zoomLevel: Int): Double {
        return resolution(zoomLevel) * (1 / PIXEL_SIZE)
    }

    /**
     * Returns the EPSG:3857 bounding of the specified tile coordinate
     *
     * @param tx        The tile x coordinate
     * @param ty        The tile y coordinate
     * @param zoomLevel The tile zoom level
     * @return the EPSG:3857 bounding box
     */
    fun tileBbox(tx: Int, ty: Int, zoomLevel: Int): Envelope {
        val topLeft = tileTopLeft(tx, ty, zoomLevel)
        // upperLeft of tx+1,ty+1 == lowRight
        val lowerRight = tileTopLeft(tx + 1, ty + 1, zoomLevel)
        return Envelope(topLeft, lowerRight)
    }

    /**
     * Returns the EPSG:4326 bounding of the specified tile coordinate
     *
     * @param tx        The tile x coordinate
     * @param ty        The tile y coordinate
     * @param zoomLevel The tile zoom level
     * @return the EPSG:4326 bounding box
     */
    fun tileBboxLngLat(tx: Int, ty: Int, zoomLevel: Int): Envelope {
        val topLeft = metersToLngLat(tileTopLeft(tx, ty, zoomLevel))
        val lowerRight = metersToLngLat(tileTopLeft(tx + 1, ty + 1, zoomLevel))
        return Envelope(topLeft, lowerRight)
    }

    /**
     * Returns the top-left corner of the specific tile coordinate
     *
     * @param tx        The tile x coordinate
     * @param ty        The tile y coordinate
     * @param zoomLevel The tile zoom level
     * @return The EPSG:3857 coordinate of the top-left corner
     */
    fun tileTopLeft(tx: Int, ty: Int, zoomLevel: Int): Coordinate {
        val px = tx * tileSize
        val py = ty * tileSize
        return pixelsToMeters(px.toDouble(), py.toDouble(), zoomLevel)
    }

    /**
     * Returns the top-left corner of the top-left tile
     *
     * @return the EPSG:3857 coordinate for the top-left corner.
     */
    fun topLeft(): Coordinate {
        return topLeft
    }

    companion object {

        private val PIXEL_SIZE = 0.00028 // 28mm

        val std256 = TileMath(256)
    }

}
/**
 * Create a new GoogleMapsTileMath with default tileSize = 256px;
 */
