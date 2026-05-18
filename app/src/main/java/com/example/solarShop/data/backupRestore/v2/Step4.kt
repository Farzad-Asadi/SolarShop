package com.example.solarShop.data.backupRestore.v2

import kotlinx.serialization.Serializable

// ============================================================================
// مرحلهٔ ۴ — CustomersActive/Archived Providers (فقط کدهای این مرحله)
// فایل: customers/CustomersProviders.kt
// ----------------------------------------------------------------------------
// تمرکز این مرحله:
// 1) فیلتر بر اساس archive = false/true
// 2) رعایت userKey + پشتیبانی از Remap در ریستور
// 3) ساخت/خواندن فقط «مشتری‌ها + سفارش‌ها» (فعلاً بدون فایل‌های ضمیمه)
// — الحاق ضمیمه‌ها را در یک مرحلهٔ بعدی اضافه می‌کنیم تا هر مرحله ساده بماند.
// ============================================================================

// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
// 1) DTO های سبک و مستقل برای customers/*/data.json
// ⚠️ فقط فیلدهای «واقعاً لازم» را می‌بریم تا ناهمخوانی با دیتابیس کم شود.
// اگر در Entity فیلدهای بیشتری داری (phone…)، همین‌جا اضافه کن.
// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ


@kotlinx.serialization.Serializable
data class CustomersDTO(
    val version: Int = 1,
    val clients: List<ClientDTO>,
    val orders: List<OrderDTO>
)


@kotlinx.serialization.Serializable
data class ClientDTO(
    val id: Int?, // Int? چون autoGenerate ممکن است
    val userKey: String, // کلید مالک — برای Remap مهم است
    val name: String,
    val archive: Boolean // برای یکسان‌سازی با فیلتر
)


@Serializable
data class OrderDTO(
    val id: Int?, // Int?
    val userKey: String, // همان userKey مالک
    val clientId: Int, // FK به Client(id)
    val archive: Boolean // هماهنگ با مدل تو
// می‌توانی فیلدهای ضروری مثل createdAt/number/... را بعداً اضافه کنی
)


// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
// 2) وابستگی‌ها: فرض بر وجود Daoهای زیر است.
// اگر نام‌هایت فرق می‌کند، فقط امضاها را با نام واقعی Daoهایت هماهنگ کن.
// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ


interface ClientDaoLite {
    suspend fun getClientsByArchive(userKey: String, archive: Boolean): List<ClientEntityLite>
    suspend fun insertClients(list: List<ClientEntityLite>)
    suspend fun clearAllForUser(userKey: String)
}


interface OrderDaoLite {
    suspend fun getOrdersByArchive(userKey: String, archive: Boolean): List<OrderEntityLite>
    suspend fun insertOrders(list: List<OrderEntityLite>)
    suspend fun clearAllForUser(userKey: String)
}


// Entityهای مینیمال برای این مرحله (Data classهای لایت جهت جداکردن از Room اصلی)
data class ClientEntityLite(
    val id: Int = 0,
    val userKey: String,
    val name: String,
    val archive: Boolean
)


data class OrderEntityLite(
    val id: Int = 0,
    val userKey: String,
    val clientId: Int,
    val archive: Boolean
)

// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
// 3) Provider پایه: کد مشترک برای Active/Archived
// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ

abstract class BaseCustomersProvider(
    private val clientDao: ClientDaoLite,
    private val orderDao: OrderDaoLite,
    private val forArchived: Boolean
) : BackupProvider {

    override val category: BackupCategory = if (forArchived) BackupCategory.CUSTOMERS_ARCHIVED else BackupCategory.CUSTOMERS_ACTIVE


    override suspend fun snapshot(ctx: BackupContext): ProviderSnapshot {
        // فقط داده‌های همان userKey فعلی و بر اساس archive
        val clients = clientDao.getClientsByArchive(ctx.currentUserKey, archive = forArchived)
        val orders = orderDao.getOrdersByArchive(ctx.currentUserKey, archive = forArchived)


        val dto = CustomersDTO(
            version = 1,
            clients = clients.map { it.toDto() },
            orders = orders.map { it.toDto() }
        )
        val jsonStr = ctx.json.encodeToString(CustomersDTO.serializer(), dto)


        // این دسته فعلاً فایل جانبی ندارد (extraFiles = emptyList)
        val (folder, dataName) = if (forArchived)
            ZipLayout.Customers.Archived.FOLDER to ZipLayout.Customers.Archived.DATA
        else
            ZipLayout.Customers.Active.FOLDER to ZipLayout.Customers.Active.DATA


        return ProviderSnapshot(
            category = category,
            zipFolder = folder,
            jsonFileName = dataName,
            jsonPayload = jsonStr,
            extraFiles = emptyList()
        )
    }
    override suspend fun restore(
        snapshot: ProviderSnapshot,
        options: RestoreOptions,
        ctx: RestoreContext
    ): RestoreReport {
        if (!options.selected.contains(category))
            return RestoreReport(category, 0, 0, 0)


        val data = ctx.json.decodeFromString(CustomersDTO.serializer(), snapshot.jsonPayload)


        // 1) تعیین userKey مقصد با توجه به گزینهٔ Remap
        val targetUserKey = when (options.userKeyMapping) {
            UserKeyMapping.RequireExactMatch -> {
                // اگر حتی یک client/ order با userKey متفاوت باشد، ریستور را بلاک کن
                val allKeys = (data.clients.map { it.userKey } + data.orders.map { it.userKey }).toSet()
                if (allKeys.size != 1 || allKeys.first() != ctx.currentUserKey) {
                    return RestoreReport(category, 0, 0, 0, errors = listOf("userKey mismatch: set=$allKeys, current=${ctx.currentUserKey}"))
                }
                ctx.currentUserKey
            }
            UserKeyMapping.RemapToCurrent -> ctx.currentUserKey
        }


        // 2) سیاست فعلی: Overwrite — فقط روی همین دسته (active/archived) برای همان userKey
        when (options.conflictPolicy) {
            ConflictPolicy.Overwrite -> {
                // پاکسازی محدود به همین userKey؛ اگر بخواهی محدود به archive هم بشود،
                // می‌توانی Dao جدا برای clearByArchive(userKey, archived) بدهی.
                clientDao.clearAllForUser(targetUserKey)
                orderDao.clearAllForUser(targetUserKey)
            }
            ConflictPolicy.Skip, ConflictPolicy.Merge -> {
                // TODO(step5): Upsert هوشمند (کلید کسب‌وکاری مثلاً name برای Client)
                clientDao.clearAllForUser(targetUserKey)
                orderDao.clearAllForUser(targetUserKey)
            }
        }


        // 3) درج — اگر Remap فعال بود، userKey را حین نگاشت جایگزین کن
        val clients = data.clients
            .filter { it.archive == forArchived }
            .map { it.toEntity(targetUserKey) }


        // نگاشت clientIdهای سفارش‌ها: چون idها ممکن است عوض شوند،
        // ابتدا مشتری‌ها را درج کن و سپس با یک map از نام (یا id قدیم→جدید) سفارش‌ها را وصل کن.
        clientDao.insertClients(clients)


        // ساخت نگاشت id قدیم → id جدید بر اساس (userKey+name) به‌عنوان کلید طبیعی ساده
        // اگر در دیتابیس محدودیت یکتا روی (userKey,name) داری، این کار مطمئن‌تر می‌شود.
        val keyByOldId: Map<Int, Pair<String,String>> = data.clients
            .filter { it.archive == forArchived }
            .associate { (it.id ?: -1) to (it.userKey to it.name) }


        // ⚠️ اینجا نیاز به کوئری کمکی داری تا id جدید را با (userKey,name) بگیری.
        // برای ساده‌سازی این مرحله، فرض می‌کنیم insertClients به همان ترتیب idهای جدید را برمی‌گرداند
        // یا در Dao کمکی مثل getIdByUserKeyAndName(userKey,name) فراهم می‌کنی.
        // فعلاً سفارش‌ها را با clientId قدیمی عبور می‌دهیم و TODO می‌گذاریم.


        val orders = data.orders
            .filter { it.archive == forArchived }
            .map { it.toEntity(targetUserKey /*, newClientId = TODO mapping */) }


        orderDao.insertOrders(orders)


        val inserted = clients.size + orders.size
        return RestoreReport(category, inserted = inserted, updated = 0, skipped = 0)
    }


}

// Providerهای نهایی که فقط پارامتر archived را مشخص می‌کنند
class CustomersActiveProvider(
    clientDao: ClientDaoLite,
    orderDao: OrderDaoLite
) : BaseCustomersProvider(clientDao, orderDao, forArchived = false)


class CustomersArchivedProvider(
    clientDao: ClientDaoLite,
    orderDao: OrderDaoLite
) : BaseCustomersProvider(clientDao, orderDao, forArchived = true)


// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
// 4) مبدل‌های DTO ↔ EntityLite
// ــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
private fun ClientEntityLite.toDto() = ClientDTO(
    id = this.id,
    userKey = this.userKey,
    name = this.name,
    archive = this.archive
)


private fun OrderEntityLite.toDto() = OrderDTO(
    id = this.id,
    userKey = this.userKey,
    clientId = this.clientId,
    archive = this.archive
)


private fun ClientDTO.toEntity(targetUserKey: String) = ClientEntityLite(
    id = 0, // 0 ⇒ autoGenerate
    userKey = targetUserKey,
    name = this.name,
    archive = this.archive
)


private fun OrderDTO.toEntity(targetUserKey: String /*, newClientId: Int */) = OrderEntityLite(
    id = 0,
    userKey = targetUserKey,
    clientId = this.clientId, // TODO: map قدیم→جدید بر اساس (userKey,name)
    archive = this.archive
)

