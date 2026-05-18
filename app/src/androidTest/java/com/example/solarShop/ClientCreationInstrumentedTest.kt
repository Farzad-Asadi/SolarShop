package com.example.solarShop

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.solarShop.data.room.appDatabase.AppDatabase
import com.example.solarShop.data.room.tables.client.ClientEntity
import com.example.solarShop.data.room.tables.user.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientCreationInstrumentedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // فقط برای تست
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun createNewClient_savesAndCanBeReadBack() = runBlocking {
        // Arrange: اول User را می‌گذاریم چون Client به userKey foreign-key دارد
        val user = UserEntity(
            id = 1,
            name = "Test User",
            userKey = "user_key_1",
            mobilePhone = "09120000000",
            createdAt = 1L
        )
        db.userDao().insertUser(user)

        val newClient = ClientEntity(
            userKey = user.userKey,
            name = "مشتری تست",
            mobilePhone = "09350000000"
        )

        // Act: ساخت مشتری
        val rowId = db.clientDao().insertClient(newClient)
        assertTrue("insertClient باید موفق باشد", rowId > 0)

        val createdId = rowId.toInt()

        // Assert 1: خواندن مستقیم
        val saved = db.clientDao().getClientById(createdId)
        assertNotNull(saved)
        assertEquals(createdId, saved!!.id)
        assertEquals("مشتری تست", saved.name)
        assertEquals(user.userKey, saved.userKey)

        // Assert 2: از طریق Flow لیست مشتری‌های این کاربر
        val list = db.clientDao().observeAllClientOfUser(user.userKey).first()
        assertEquals(1, list.size)
        assertEquals("مشتری تست", list.first().name)
    }
}
