package com.example.solarShop.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 =
    object : Migration(3, 4) {

        override fun migrate(db: SupportSQLiteDatabase) {

            // =========================================================
            // clients
            // =========================================================

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN uid TEXT NOT NULL DEFAULT ''"
            )

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
            )

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN deletedAt INTEGER"
            )

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
            )

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN createdByUserId INTEGER"
            )

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN updatedByUserId INTEGER"
            )

            db.execSQL(
                "ALTER TABLE clients ADD COLUMN shopUid TEXT"
            )

            db.execSQL(
                """
                UPDATE clients
                SET uid = lower(hex(randomblob(16)))
                WHERE uid = ''
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE clients
                SET updatedAt = createdAt
                WHERE updatedAt = 0
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_clients_uid
                ON clients(uid)
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_clients_updatedAt ON clients(updatedAt)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_clients_deletedAt ON clients(deletedAt)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_clients_isSynced ON clients(isSynced)"
            )

            // =========================================================
            // orders
            // =========================================================

            db.execSQL(
                "ALTER TABLE orders ADD COLUMN uid TEXT NOT NULL DEFAULT ''"
            )

            db.execSQL(
                "ALTER TABLE orders ADD COLUMN deletedAt INTEGER"
            )

            db.execSQL(
                "ALTER TABLE orders ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0"
            )

            db.execSQL(
                "ALTER TABLE orders ADD COLUMN createdByUserId INTEGER"
            )

            db.execSQL(
                "ALTER TABLE orders ADD COLUMN updatedByUserId INTEGER"
            )

            db.execSQL(
                "ALTER TABLE orders ADD COLUMN shopUid TEXT"
            )

            db.execSQL(
                """
                UPDATE orders
                SET uid = lower(hex(randomblob(16)))
                WHERE uid = ''
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_orders_uid
                ON orders(uid)
                """.trimIndent()
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_orders_updatedAt ON orders(updatedAt)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_orders_deletedAt ON orders(deletedAt)"
            )

            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_orders_isSynced ON orders(isSynced)"
            )

            // =========================================================
            // invoice_documents
            // =========================================================

            db.execSQL(
                """
                ALTER TABLE invoice_documents
                ADD COLUMN uid TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_documents
                ADD COLUMN deletedAt INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_documents
                ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_documents
                ADD COLUMN createdByUserId INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_documents
                ADD COLUMN updatedByUserId INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_documents
                ADD COLUMN shopUid TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE invoice_documents
                SET uid = lower(hex(randomblob(16)))
                WHERE uid = ''
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_invoice_documents_uid
                ON invoice_documents(uid)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_invoice_documents_updatedAt
                ON invoice_documents(updatedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_invoice_documents_deletedAt
                ON invoice_documents(deletedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_invoice_documents_isSynced
                ON invoice_documents(isSynced)
                """.trimIndent()
            )

            // =========================================================
            // invoice_items
            // =========================================================

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN uid TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN deletedAt INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN createdByUserId INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN updatedByUserId INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE invoice_items
                ADD COLUMN shopUid TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE invoice_items
                SET uid = lower(hex(randomblob(16)))
                WHERE uid = ''
                """.trimIndent()
            )

            // تاریخ ردیف‌های قدیمی از سند مادر گرفته شود.
            db.execSQL(
                """
                UPDATE invoice_items
                SET createdAt = COALESCE(
                    (
                        SELECT invoice_documents.createdAt
                        FROM invoice_documents
                        WHERE invoice_documents.id = invoice_items.invoiceId
                    ),
                    CAST(strftime('%s', 'now') AS INTEGER) * 1000
                )
                WHERE createdAt = 0
                """.trimIndent()
            )

            db.execSQL(
                """
                UPDATE invoice_items
                SET updatedAt = COALESCE(
                    (
                        SELECT invoice_documents.updatedAt
                        FROM invoice_documents
                        WHERE invoice_documents.id = invoice_items.invoiceId
                    ),
                    createdAt
                )
                WHERE updatedAt = 0
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS index_invoice_items_uid
                ON invoice_items(uid)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_invoice_items_updatedAt
                ON invoice_items(updatedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_invoice_items_deletedAt
                ON invoice_items(deletedAt)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_invoice_items_isSynced
                ON invoice_items(isSynced)
                """.trimIndent()
            )
        }
    }