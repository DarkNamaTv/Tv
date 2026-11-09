package com.darktv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class UiNavigationTest {
    @Test fun completeRows() {
        assertEquals(GridRightDestination.NextChannel, gridRightDestination(4, 12, 6))
        assertEquals(GridRightDestination.ScrollTop, gridRightDestination(5, 12, 6))
        assertEquals(GridRightDestination.NextChannel, gridRightDestination(10, 12, 6))
        assertEquals(GridRightDestination.ScrollTop, gridRightDestination(11, 12, 6))
    }
    @Test fun incompleteLastRow() {
        assertEquals(GridRightDestination.NextChannel, gridRightDestination(12, 14, 6))
        assertEquals(GridRightDestination.ScrollTop, gridRightDestination(13, 14, 6))
    }
    @Test fun singleColumnAndInvalidInputs() {
        assertEquals(GridRightDestination.ScrollTop, gridRightDestination(0, 3, 1))
        assertEquals(GridRightDestination.ScrollTop, gridRightDestination(2, 3, 1))
        assertEquals(GridRightDestination.Block, gridRightDestination(0, 0, 6))
        assertEquals(GridRightDestination.Block, gridRightDestination(-1, 2, 6))
        assertEquals(GridRightDestination.Block, gridRightDestination(2, 2, 6))
        assertEquals(GridRightDestination.Block, gridRightDestination(0, 1, 0))
    }
}
