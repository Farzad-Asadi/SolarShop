package com.example.solarShop.data.room.tables.client

import androidx.room.withTransaction
import com.example.solarShop.data.local.database.AppDatabase
import com.example.solarShop.data.room.tables.orderAll.order.OrderDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class OfflineClientRepository @Inject constructor(
    private val db: AppDatabase,
    private val clientDao: ClientDao,
    private val orderDao: OrderDao
) : ClientRepository {

    // =========================================================
    // Local CRUD
    // =========================================================

    override suspend fun insertClient(
        clientEntity: ClientEntity
    ): Long {
        val now =
            System.currentTimeMillis()

        return clientDao.insertClient(
            clientEntity.copy(
                createdAt = clientEntity.createdAt
                    .takeIf { it > 0L }
                    ?: now,

                updatedAt = now,
                deletedAt = null,
                isSynced = false
            )
        )
    }

    override suspend fun updateClient(
        clientEntity: ClientEntity
    ): Int {
        return clientDao.updateClient(
            clientEntity.copy(
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
        )
    }

    override suspend fun deleteClient(
        clientEntity: ClientEntity
    ): Int {
        val clientId =
            clientEntity.id ?: return 0

        val now =
            System.currentTimeMillis()

        return db.withTransaction {

            /*
             * رفتار قبلی حذف مشتری، سفارش‌هایش را نیز به دلیل FK حذف می‌کرد.
             * اکنون سفارش‌ها نیز tombstone می‌شوند.
             */
            orderDao.softDeleteByClientId(
                clientId = clientId,
                deletedAt = now
            )

            clientDao.softDeleteById(
                clientId = clientId,
                deletedAt = now
            )
        }
    }

    override suspend fun getClientById(
        clientId: Int
    ): ClientEntity? {
        return clientDao.getClientById(clientId)
    }

    // =========================================================
    // UI Flows
    // =========================================================

    override fun observeAllClients(): Flow<List<ClientEntity>> {
        return clientDao
            .observeAllClients()
            .distinctUntilChanged()
    }

    override fun observeClientById(
        clientId: Int
    ): Flow<ClientEntity?> {
        return clientDao
            .observeClientById(clientId)
            .distinctUntilChanged()
    }

    /*
     * مشتری‌ها از این مرحله به بعد اشتراکی هستند.
     * userKey فقط برای سازگاری مدل قدیمی باقی مانده است.
     */
    override fun observeAllClientOfUser(
        userKey: String
    ): Flow<List<ClientEntity>> {
        return clientDao
            .observeAllClients()
            .distinctUntilChanged()
    }

    override fun observeClientWithOrders(): Flow<List<ClientWithOrders>> {
        return combineClientsWithActiveOrders()
    }

    /*
     * پارامتر userKey عمداً برای سازگاری API فعلی نگه داشته شده،
     * اما همه کاربران، کل مشتری‌های فروشگاه را می‌بینند.
     */
    override fun observeClientsWithOrdersByUserId(
        userKey: String
    ): Flow<List<ClientWithOrders>> {
        return combineClientsWithActiveOrders()
    }

    private fun combineClientsWithActiveOrders():
            Flow<List<ClientWithOrders>> {

        return combine(
            clientDao.observeAllClients(),
            orderDao.observeAllOrders()
        ) { clients, orders ->

            val ordersByClientId =
                orders.groupBy { order ->
                    order.clientId
                }

            clients.map { client ->

                ClientWithOrders(
                    clientEntity = client,
                    orders = client.id
                        ?.let { clientId ->
                            ordersByClientId[clientId].orEmpty()
                        }
                        .orEmpty()
                )
            }
        }.distinctUntilChanged()
    }

    // =========================================================
    // Sync
    // =========================================================

    override suspend fun getClientByUid(
        uid: String
    ): ClientEntity? {
        return clientDao.getClientByUid(uid)
    }

    override suspend fun getClientIdByUid(
        uid: String
    ): Int? {
        return clientDao.getClientIdByUid(uid)
    }

    override suspend fun getUnsyncedClients():
            List<ClientEntity> {

        return clientDao.getUnsyncedClients()
    }

    override suspend fun markClientsSynced(
        uids: List<String>
    ) {
        if (uids.isEmpty()) return

        clientDao.markClientsSynced(uids)
    }

    override suspend fun softDeleteByUid(
        uid: String
    ): Int {
        return clientDao.softDeleteByUid(uid)
    }

    override suspend fun upsertClientByUid(
        clientEntity: ClientEntity
    ): Long {
        val existing =
            clientDao.getClientByUid(clientEntity.uid)

        if (existing == null) {
            return clientDao.insertClient(clientEntity)
        }

        /*
         * tombstone محلی نباید با نسخه زنده قدیمی سرور احیا شود.
         */
        if (
            existing.deletedAt != null &&
            clientEntity.deletedAt == null
        ) {
            return existing.id?.toLong() ?: 0L
        }

        /*
         * تغییر محلی جدیدتر نباید با داده قدیمی‌تر Pull بازنویسی شود.
         */
        if (
            !existing.isSynced &&
            existing.updatedAt > clientEntity.updatedAt
        ) {
            return existing.id?.toLong() ?: 0L
        }

        val merged =
            clientEntity.copy(
                id = existing.id,

                /*
                 * userKey محلی حفظ می‌شود تا Foreign Key نسخه فعلی نشکند.
                 */
                userKey = existing.userKey,

                /*
                 * زمان ایجاد رکورد محلی ثابت می‌ماند.
                 */
                createdAt = existing.createdAt
            )

        clientDao.updateClient(merged)

        return existing.id?.toLong() ?: 0L
    }
}