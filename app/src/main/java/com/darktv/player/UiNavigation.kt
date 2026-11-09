package com.darktv.player

internal enum class GridRightDestination { NextChannel, Block, ScrollTop }

/** Right exits the grid from the last column or the last item of an incomplete row. */
internal fun gridRightDestination(index: Int, count: Int, columns: Int): GridRightDestination = when {
    count <= 0 || columns <= 0 || index !in 0 until count -> GridRightDestination.Block
    index == count - 1 -> GridRightDestination.ScrollTop
    index % columns == columns - 1 -> GridRightDestination.ScrollTop
    else -> GridRightDestination.NextChannel
}
