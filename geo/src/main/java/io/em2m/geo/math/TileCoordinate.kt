package io.em2m.geo.math

import org.locationtech.jts.geom.Envelope

data class TileCoordinate(val x: Int, val y: Int, val z: Int) {

    fun parent(): TileCoordinate {
        return TileCoordinate(x / 2, y / 2, z - 1)
    }

    fun child(xOff: Int, yOff: Int): TileCoordinate {
        return TileCoordinate(x * 2 + xOff, y * 2 + yOff, z + 1)
    }

    fun children(): Array<TileCoordinate> {

        // UL, UR, LL, LR
        return arrayOf(child(0, 0), child(1, 0), child(0, 1), child(1, 1))
    }

    fun isLineal(other: TileCoordinate?): Boolean {
        return if (other != null) isAncestor(other) || other.isAncestor(this) || this == other else false
    }

    fun isAncestor(other: TileCoordinate?): Boolean {
        if (other == null || other.z >= this.z) {
            return false
        }
        val dz = this.z - other.z
        val ax = this.x shr dz
        val ay = this.y shr dz
        return ax == other.x && ay == other.y
    }

    fun bboxLngLat(): Envelope {
        return TileMath.std256.tileBboxLngLat(x, y, z)
    }

    fun bboxMeters(): Envelope {
        return TileMath.std256.tileBbox(x, y, z)
    }

    fun toCode(): String {
        //
        val zs = z..0
        var currentX = x
        var currentY = y
        var currentZ = z
        val path = ArrayList<Int>()
        while (currentZ >= 0) {
            val xOff = currentX - ((currentX / 2) * 2)
            val yOff = currentY - ((currentY / 2) * 2)
            currentX /= 2
            currentY /= 2
            --currentZ
            path.add(xOff * 2 + yOff)
        }
        if (path.size.mod(2) == 1) {
            path.removeLast()
        }
        return path.reversed().chunked(2).joinToString("") { pairs ->
            val high = pairs[0]
            val low = pairs[1]
            (high * 4 + low).toString(16)
        }.uppercase()
    }

    fun toCodeSet(): List<String> {
        val code = toCode()
        return (1..code.length).map { i ->
            //i.toString(16).uppercase() + "-" + code.take(i)
            code.take(i)
        }
    }

    val isValid: Boolean
        get() {
            val max = (1 shl z) - 1
            return !(x < 0 || y < 0 || z < 0 || x > max || y > max)
        }

    companion object {

        fun fromCode(code: String): TileCoordinate {

            var x = 0;
            var y = 0
            var z = 0

            code.toCharArray().forEach { letter ->
                z += 2
                val n = letter.toString().toLong(16)
                val first = n / 4
                val second = n - (first * 4)
                when (first) {
                    0L -> {
                        x = x * 2 + 0
                        y = y * 2 + 0
                    }
                    1L -> {
                        x = x * 2 + 0
                        y = y * 2 + 1
                    }
                    2L -> {
                        x = x * 2 + 1
                        y = y * 2 + 0
                    }
                    3L -> {
                        x = x * 2 + 1
                        y = y * 2 + 1
                    }
                }
                when (second) {
                    0L -> {
                        x = x * 2 + 0
                        y = y * 2 + 0
                    }
                    1L -> {
                        x = x * 2 + 0
                        y = y * 2 + 1
                    }
                    2L -> {
                        x = x * 2 + 1
                        y = y * 2 + 0
                    }
                    3L -> {
                        x = x * 2 + 1
                        y = y * 2 + 1
                    }

                }

            }
            return TileCoordinate(x, y, z)
        }
    }

}
