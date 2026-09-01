package com.zackwhye.secondbrain.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zackwhye.secondbrain.core.database.entity.ItemSyncState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pilot users' phones carry v1/v2 databases and will run AutoMigration 1→2 and 2→3 on first launch
 * of a new build. This opens a REAL v1 database from the exported schema JSON (app/schemas), writes
 * rows with the v1 column set, migrates to the current version, and asserts the rows survive with
 * their values — plus that the v3 default (`items.profile = 'general'`) is applied to legacy rows
 * both at the SQL level and through the Room entity mapping.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java, // Class-based constructor: picks up the @Database autoMigrations itself
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To3_preservesItemsAndBriefs_andAppliesProfileDefault() {
        // ---- v1: real rows, v1 column set (no `profile`, no `facts` table) ----
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO items (id, userId, sourceType, sourceDoor, sourceUri, rawText, title, projectId, syncState, capturedAt, createdAt, updatedAt) " +
                    "VALUES ('item-1', 'user-1', 'TEXT', 'SHARE', NULL, 'hello from v1', NULL, NULL, 'SYNCED', 1000, 1001, 1002)",
            )
            execSQL(
                "INSERT INTO items (id, userId, sourceType, sourceDoor, sourceUri, rawText, title, projectId, syncState, capturedAt, createdAt, updatedAt) " +
                    "VALUES ('item-2', NULL, 'URL', 'SHARE', 'https://example.com', NULL, 'A title', NULL, 'FAILED', 2000, 2001, 2002)",
            )
            // List<String> columns are stored by Converters as a delimited string; inserted raw
            // here and asserted raw below — the point is byte-for-byte survival.
            execSQL(
                "INSERT INTO briefs (id, itemId, status, summary, entities, topics, tasks, importance, failureReason, createdAt, updatedAt) " +
                    "VALUES ('brief-1', 'item-1', 'READY', 'A summary', 'e1e2', 't1', '', 3, NULL, 10, 11)",
            )
            close()
        }

        // ---- migrate 1 → 3 (both AutoMigrations) and validate the resulting schema against 3.json ----
        val db = helper.runMigrationsAndValidate(dbName, 3, true)

        db.query("SELECT id, userId, sourceType, sourceUri, rawText, title, syncState, capturedAt, createdAt, updatedAt, profile FROM items ORDER BY id").use { c ->
            assertEquals(2, c.count)

            c.moveToFirst()
            assertEquals("item-1", c.getString(0))
            assertEquals("user-1", c.getString(1))
            assertEquals("TEXT", c.getString(2))
            assertEquals(true, c.isNull(3))
            assertEquals("hello from v1", c.getString(4))
            assertEquals(true, c.isNull(5))
            assertEquals("SYNCED", c.getString(6))
            assertEquals(1000L, c.getLong(7))
            assertEquals(1001L, c.getLong(8))
            assertEquals(1002L, c.getLong(9))
            assertEquals("general", c.getString(10)) // v3 default applied to a legacy row

            c.moveToNext()
            assertEquals("item-2", c.getString(0))
            assertEquals(true, c.isNull(1))
            assertEquals("URL", c.getString(2))
            assertEquals("https://example.com", c.getString(3))
            assertEquals(true, c.isNull(4))
            assertEquals("A title", c.getString(5))
            assertEquals("FAILED", c.getString(6))
            assertEquals(2000L, c.getLong(7))
            assertEquals("general", c.getString(10))
        }

        db.query("SELECT id, itemId, status, summary, entities, topics, tasks, importance, failureReason, createdAt, updatedAt FROM briefs").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("brief-1", c.getString(0))
            assertEquals("item-1", c.getString(1))
            assertEquals("READY", c.getString(2))
            assertEquals("A summary", c.getString(3))
            assertEquals("e1e2", c.getString(4))
            assertEquals("t1", c.getString(5))
            assertEquals("", c.getString(6))
            assertEquals(3, c.getInt(7))
            assertEquals(true, c.isNull(8))
            assertEquals(10L, c.getLong(9))
            assertEquals(11L, c.getLong(10))
        }

        // v2 added `facts`: present and empty after migration, with its indices.
        db.query("SELECT count(*) FROM facts").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        // Room's declared indices only — SQLite adds its own sqlite_autoindex_* for the TEXT primary key.
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'facts' AND name NOT LIKE 'sqlite_autoindex%' ORDER BY name").use { c ->
            val names = generateSequence { if (c.moveToNext()) c.getString(0) else null }.toList()
            assertEquals(listOf("index_facts_sourceItemId", "index_facts_subject"), names)
        }
        db.close()

        // ---- open the migrated file through Room: entity mapping + DAO queries see the legacy rows ----
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName).build()
        try {
            runBlocking {
                val item1 = checkNotNull(room.itemDao().getById("item-1"))
                assertEquals("hello from v1", item1.rawText)
                assertEquals(ItemSyncState.SYNCED, item1.syncState)
                assertEquals("general", item1.profile)
                assertEquals("user-1", item1.userId)

                val failed = room.itemDao().getFailed()
                assertEquals(listOf("item-2"), failed.map { it.id }) // retry-on-app-open still finds legacy FAILED rows
                assertEquals("general", failed.single().profile)
            }
        } finally {
            room.close()
        }
    }
}
