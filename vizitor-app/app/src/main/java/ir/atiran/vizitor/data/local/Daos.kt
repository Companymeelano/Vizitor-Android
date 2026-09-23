/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | لایه دسترسی به داده (Room DAOs)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE LOCALIZED ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name COLLATE LOCALIZED ASC")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE code = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("UPDATE products SET stock = :stock WHERE id = :productId")
    suspend fun updateStock(productId: Int, stock: Double)

    @Query("DELETE FROM products")
    suspend fun clear()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE LOCALIZED ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name COLLATE LOCALIZED ASC")
    suspend fun getAll(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE purchaseDropPercent >= :minDrop ORDER BY purchaseDropPercent DESC")
    fun observeNeedsFollowUp(minDrop: Int = 25): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers")
    suspend fun clear()
}

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insertHeader(invoice: InvoiceEntity): Long

    @Insert
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Update
    suspend fun updateHeader(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC")
    suspend fun getPending(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE id = :invoiceId LIMIT 1")
    suspend fun getById(invoiceId: Long): InvoiceEntity?

    /** آخرین قیمت فروش یک کالا به یک مشتری (برای پیشنهاد قیمت هوشمند). */
    @Query(
        "SELECT ii.unitPrice FROM invoice_items ii " +
        "INNER JOIN invoices i ON i.id = ii.invoiceId " +
        "WHERE i.customerId = :customerId AND ii.productId = :productId " +
        "ORDER BY i.createdAt DESC LIMIT 1"
    )
    suspend fun lastUnitPrice(customerId: Int, productId: Int): Long?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getItems(invoiceId: Long): List<InvoiceItemEntity>

    @Query("SELECT COALESCE(SUM(finalAmount), 0) FROM invoices WHERE createdAt >= :since AND status != 'FAILED'")
    fun observeSalesSince(since: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(finalAmount), 0) FROM invoices WHERE createdAt >= :since AND status != 'FAILED'")
    suspend fun salesSince(since: Long): Long

    @Query("SELECT COUNT(*) FROM invoices WHERE status = 'PENDING' OR status = 'FAILED'")
    fun observePendingCount(): Flow<Int>

    /** پرفروش‌ترین کالاها از اقلام فاکتورهای محلی (ویجت پرفروش‌ها). */
    @Query(
        "SELECT productName AS productName, SUM(quantity) AS total " +
                "FROM invoice_items GROUP BY productName ORDER BY total DESC LIMIT 3"
    )
    suspend fun topProducts(): List<TopProduct>
}

@Dao
interface SalMaliDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<SalMaliHistoryEntity>)

    @Query("SELECT * FROM sal_mali_history WHERE customerId = :customerId ORDER BY totalQty DESC LIMIT 30")
    suspend fun forCustomer(customerId: Int): List<SalMaliHistoryEntity>

    @Query("DELETE FROM sal_mali_history WHERE customerId = :customerId")
    suspend fun clearForCustomer(customerId: Int)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY productName COLLATE LOCALIZED ASC")
    fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items ORDER BY productName COLLATE LOCALIZED ASC")
    suspend fun getAll(): List<CartItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CartItemEntity)

    @Delete
    suspend fun delete(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteByProduct(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Query("SELECT COALESCE(SUM(quantity * unitPrice), 0) FROM cart_items")
    fun observeTotal(): Flow<Long>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    fun observeItemCount(): Flow<Double>
}

@Dao
interface ServerInvoiceDao {
    @Query("SELECT * FROM server_invoices ORDER BY id DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<ServerInvoiceEntity>>

    @Query("SELECT COUNT(*) FROM server_invoices")
    fun observeCount(): kotlinx.coroutines.flow.Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ServerInvoiceEntity>)

    @Query("SELECT COUNT(*) FROM server_invoices")
    suspend fun count(): Int

    @Query("DELETE FROM server_invoices")
    suspend fun clear()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timeLong ASC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int

    @Insert
    suspend fun insert(msg: ChatMessageEntity): Long

    @Insert
    suspend fun insertAll(msgs: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun delete(id: Long)
}
