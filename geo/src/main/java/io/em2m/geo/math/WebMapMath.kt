package io.em2m.geo.math

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.geom.util.GeometryTransformer
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

class WebMapMath {

    companion object {
        // 20037508.342789244
        private const val originShift = 2.0 * Math.PI * 6378137.0 / 2.0

        /**
         * Converts given coordinate in WGS84 Datum to XY in Spherical Mercator
         * EPSG:3857
         *
         * @param coord The coordinate to convert
         * @return The coordinate transformed to EPSG:3857
         */
        fun lngLatToMeters(coord: Coordinate): Coordinate {
            val mx = coord.x * originShift / 180.0
            var my = ln(tan((90 + coord.y) * Math.PI / 360.0)) / (Math.PI / 180.0)
            my *= originShift / 180.0
            return Coordinate(mx, my)
        }

        /**
         * Converts geometry from lat/lon (EPSG:4326)) to Spherical Mercator
         * (EPSG:3857)
         *
         * @param geometry the geometry to convert
         * @return the geometry transformed to EPSG:3857
         */
        fun lngLatToMeters(geometry: Geometry): Geometry {
            val transformer = object : GeometryTransformer() {
                override fun transformCoordinates(coords: CoordinateSequence, parent: Geometry?): CoordinateSequence {
                    val newCoords = arrayOfNulls<Coordinate>(coords.size())
                    for (i in 0 until coords.size()) {
                        val coord = coords.getCoordinate(i)
                        newCoords[i] = lngLatToMeters(coord)
                    }
                    return CoordinateArraySequence(newCoords)
                }
            }
            return transformer.transform(geometry)
        }

        /**
         * Transforms given envelope in Spherical Mercator to EPSG:3857
         *
         * @param env The envelope to transform
         * @return The envelope transformed to EPSG:3857
         */
        fun lngLatToMeters(env: Envelope): Envelope {
            val min = lngLatToMeters(env.minX, env.minY)
            val max = lngLatToMeters(env.maxX, env.maxY)
            return Envelope(min.x, max.x, min.y, max.y)
        }

        /**
         * Transforms given envelope in  Spherical Mercator to EPSG:4326
         *
         * @param env The envelope to transform
         * @return The envelope transformed to EPSG:4326
         */
        fun metersToLngLat(env: Envelope): Envelope {
            val min = metersToLngLat(env.minX, env.minY)
            val max = metersToLngLat(env.maxX, env.maxY)
            return Envelope(min.x, max.x, min.y, max.y)
        }


        /**
         * Converts given coordinate in WGS84 Datum to XY in Spherical Mercator
         * EPSG:3857
         *
         * @param lng the longitude of the coordinate
         * @param lat the latitude of the coordinate
         * @return The coordinate transformed to EPSG:3857
         */
        fun lngLatToMeters(lng: Double, lat: Double): Coordinate {
            val mx = lng * originShift / 180.0
            var my = ln(tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0)

            my *= originShift / 180.0

            return Coordinate(mx, my)
        }


        /**
         * Converts XY point from Spherical Mercator (EPSG:3785) to lat/lon
         * (EPSG:4326)
         *
         * @param mx the X coordinate in meters
         * @param my the Y coordinate in meters
         * @return The coordinate transformed to EPSG:4326
         */
        fun metersToLngLat(mx: Double, my: Double): Coordinate {
            val lon = mx / originShift * 180.0
            var lat = my / originShift * 180.0

            lat = 180 / Math.PI * (2 * atan(exp(lat * Math.PI / 180.0)) - Math.PI / 2.0)

            return Coordinate(lon, lat)
        }

        /**
         * Converts coordinate from Spherical Mercator (EPSG:3785) to lat/lon
         * (EPSG:4326)
         *
         * @param coord the coordinate in meters
         * @return The coordinate transformed to EPSG:4326
         */
        fun metersToLngLat(coord: Coordinate): Coordinate {
            return metersToLngLat(coord.x, coord.y)
        }

        /**
         * Converts geometry from Spherical Mercator
         * (EPSG:3857) to lat/lon (EPSG:4326))
         *
         * @param geometry the geometry to convert
         * @return the geometry transformed to EPSG:4326
         */
        fun metersToLngLat(geometry: Geometry): Geometry {
            val transformer = object : GeometryTransformer() {
                override fun transformCoordinates(coords: CoordinateSequence, parent: Geometry?): CoordinateSequence {
                    val newCoords = arrayOfNulls<Coordinate>(coords.size())
                    for (i in 0 until coords.size()) {
                        val coord = coords.getCoordinate(i)
                        newCoords[i] = metersToLngLat(coord)
                    }
                    return CoordinateArraySequence(newCoords)
                }
            }
            return transformer.transform(geometry)
        }

        fun Coordinate.toLngLat(): Coordinate = metersToLngLat(this)
        fun Coordinate.toMeters(): Coordinate = lngLatToMeters(this)
        fun Envelope.toLngLat(): Envelope = metersToLngLat(this)
        fun Envelope.toMeters(): Envelope = lngLatToMeters(this)
        fun Geometry.toLngLat(): Geometry = metersToLngLat(this)
        fun Geometry.toMeters(): Geometry = lngLatToMeters(this)
    }
}