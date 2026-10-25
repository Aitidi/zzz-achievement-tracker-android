package com.aitidi.zzztracker.data.repo

import org.junit.Assert.*
import org.junit.Test

class ImportValidationTest {
    private val ids = setOf("a", "b", "c")

    private fun rejects(text: String) {
        assertThrows(InvalidImportException::class.java) { parseImport(text, ids, currentCompleted = 2) }
    }

    @Test
    fun emptyOrBlankFileIsRejected() {
        rejects("")
        rejects("  \n\t")
    }

    @Test
    fun nonBackupJsonIsRejected() {
        rejects("not json")
        rejects("[1, 2, 3]")
        rejects("""{"foo": "bar"}""")
        rejects("""{"items": [{"name": "x"}]}""")
    }

    @Test
    fun backupWithoutEntriesIsRejected() {
        rejects("""{"items": []}""")
    }

    @Test
    fun backupSharingNoIdWithCatalogIsRejected() {
        rejects("""{"items": [{"id": "x", "progress": true}, {"id": "y", "progress": false}]}""")
    }

    @Test
    fun validBackupReportsWhatWillBeOverwritten() {
        val pending = parseImport(
            """{"items": [{"id": "a", "progress": true}, {"id": "b", "progress": false}, {"id": "gone", "progress": true}]}""",
            ids,
            currentCompleted = 2,
        )
        assertEquals(mapOf("a" to true, "b" to false, "gone" to true), pending.progressById)
        assertEquals(3, pending.source)
        assertEquals(2, pending.matched)
        assertEquals(1, pending.completed)
        assertEquals(2, pending.currentCompleted)
    }
}
