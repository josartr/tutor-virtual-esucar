package cl.esucar.tutorvirtual.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal const val DATABASE_NAME = "tutor_virtual.db"
internal const val DATABASE_VERSION = 6

internal class TutorDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                rank TEXT NOT NULL,
                first_name TEXT NOT NULL DEFAULT '',
                first_last_name TEXT NOT NULL DEFAULT '',
                second_last_name TEXT NOT NULL DEFAULT '',
                full_name TEXT NOT NULL,
                email TEXT NOT NULL,
                research_topic TEXT NOT NULL,
                tutor_name TEXT NOT NULL,
                sso_provider TEXT NOT NULL,
                institution_type TEXT NOT NULL DEFAULT 'ESUCAR',
                user_role TEXT NOT NULL DEFAULT 'ESTUDIANTE',
                password_hash TEXT NOT NULL DEFAULT '',
                password_salt TEXT NOT NULL DEFAULT '',
                is_logged_in INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                first_name TEXT NOT NULL DEFAULT '',
                first_last_name TEXT NOT NULL DEFAULT '',
                second_last_name TEXT NOT NULL DEFAULT '',
                full_name TEXT NOT NULL,
                research_topic TEXT NOT NULL,
                tutor_name TEXT NOT NULL,
                rank TEXT NOT NULL,
                institution_type TEXT NOT NULL,
                user_role TEXT NOT NULL DEFAULT 'ESTUDIANTE',
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                sso_provider TEXT NOT NULL
            )
            """.trimIndent()
        )
        createAuthTables(db)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deliverables (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                due_date TEXT NOT NULL,
                status TEXT NOT NULL,
                stage_id TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS checklist_items (
                stage_id TEXT NOT NULL,
                item_index INTEGER NOT NULL,
                PRIMARY KEY (stage_id, item_index)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS stage_notes (
                stage_id TEXT PRIMARY KEY NOT NULL,
                note TEXT NOT NULL
            )
            """.trimIndent()
        )
        createProjectTopicsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            rebuildAuthenticationSchema(db)
        } else {
            if (oldVersion < 5) {
                runCatching { db.execSQL("ALTER TABLE users ADD COLUMN first_name TEXT NOT NULL DEFAULT ''") }
                runCatching { db.execSQL("ALTER TABLE users ADD COLUMN first_last_name TEXT NOT NULL DEFAULT ''") }
                runCatching { db.execSQL("ALTER TABLE users ADD COLUMN second_last_name TEXT NOT NULL DEFAULT ''") }
                runCatching { db.execSQL("ALTER TABLE user_profile ADD COLUMN first_name TEXT NOT NULL DEFAULT ''") }
                runCatching { db.execSQL("ALTER TABLE user_profile ADD COLUMN first_last_name TEXT NOT NULL DEFAULT ''") }
                runCatching { db.execSQL("ALTER TABLE user_profile ADD COLUMN second_last_name TEXT NOT NULL DEFAULT ''") }
            }
            if (oldVersion < 6) {
                runCatching { db.execSQL("ALTER TABLE users ADD COLUMN user_role TEXT NOT NULL DEFAULT 'ESTUDIANTE'") }
                runCatching { db.execSQL("ALTER TABLE user_profile ADD COLUMN user_role TEXT NOT NULL DEFAULT 'ESTUDIANTE'") }
            }
        }
        createProjectTopicsTable(db)
    }

    private fun createProjectTopicsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS project_topics (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createAuthTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS current_session (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                user_id INTEGER NOT NULL REFERENCES users(id)
            )
            """.trimIndent()
        )
    }

    private fun createUsersTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                first_name TEXT NOT NULL DEFAULT '',
                first_last_name TEXT NOT NULL DEFAULT '',
                second_last_name TEXT NOT NULL DEFAULT '',
                full_name TEXT NOT NULL,
                research_topic TEXT NOT NULL,
                tutor_name TEXT NOT NULL,
                rank TEXT NOT NULL DEFAULT 'Alumno ESUCAR',
                institution_type TEXT NOT NULL DEFAULT 'ESUCAR',
                user_role TEXT NOT NULL DEFAULT 'ESTUDIANTE',
                password_hash TEXT NOT NULL DEFAULT '',
                password_salt TEXT NOT NULL DEFAULT '',
                sso_provider TEXT NOT NULL DEFAULT 'Correo Institucional'
            )
            """.trimIndent()
        )
        createAuthTables(db)
    }

    private fun rebuildAuthenticationSchema(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS current_session")
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS user_profile")
        db.execSQL(
            """
            CREATE TABLE user_profile (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                rank TEXT NOT NULL DEFAULT 'Alumno ESUCAR',
                first_name TEXT NOT NULL DEFAULT '',
                first_last_name TEXT NOT NULL DEFAULT '',
                second_last_name TEXT NOT NULL DEFAULT '',
                full_name TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                research_topic TEXT NOT NULL DEFAULT '',
                tutor_name TEXT NOT NULL DEFAULT '',
                sso_provider TEXT NOT NULL DEFAULT 'Correo Institucional',
                institution_type TEXT NOT NULL DEFAULT 'ESUCAR',
                user_role TEXT NOT NULL DEFAULT 'ESTUDIANTE',
                password_hash TEXT NOT NULL DEFAULT '',
                password_salt TEXT NOT NULL DEFAULT '',
                is_logged_in INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        createUsersTable(db)
    }
}
