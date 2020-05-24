package com.revolgenx.anilib.torrent.sort

abstract class BaseSorting(var columnName: String, var direction: Direction) {

    enum class Direction {
        ASC, DESC;

        companion object {
            fun fromValue(value: String): Direction {
                for (direction in Direction::class.java.enumConstants!!) {
                    if (direction.toString().equals(value, ignoreCase = true)) {
                        return direction
                    }
                }
                return ASC
            }
        }
    }

    @FunctionalInterface
    interface SortingColumnsInterface<F> {
        fun compare(item1: F, item2: F, direction: Direction): Int
    }

    override fun toString(): String {
        return "BaseSorting{" +
                "direction=" + direction +
                ", columnName='" + columnName + '\''.toString() +
                '}'.toString()
    }
}