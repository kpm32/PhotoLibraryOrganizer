package com.anvar.photolibraryorganizer.data.filesystem

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrashFileMoverTest {
    @Test
    fun usesFallbackWhenPrimaryMoverFails() {
        val movedPaths = mutableListOf<Path>()
        val path = Path.of("/tmp/photo.jpg")
        val mover = FallbackTrashFileMover(
            primary = TestTrashFileMover { false },
            fallback = TestTrashFileMover {
                movedPaths.add(it)
                true
            },
        )

        assertTrue(mover.moveToTrash(path))
        assertEquals(listOf(path), movedPaths)
    }

    private class TestTrashFileMover(
        private val action: (Path) -> Boolean,
    ) : TrashFileMover {
        override fun moveToTrash(path: Path): Boolean = action(path)
    }
}
