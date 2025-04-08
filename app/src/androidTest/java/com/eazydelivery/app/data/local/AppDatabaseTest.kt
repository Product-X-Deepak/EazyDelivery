package com.eazydelivery.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eazydelivery.app.data.local.dao.OrderDao
import com.eazydelivery.app.data.local.dao.PlatformDao
import com.eazydelivery.app.data.local.entity.DeliveryStatus
import com.eazydelivery.app.data.local.entity.OrderEntity
import com.eazydelivery.app.data.local.entity.PlatformEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    
    private lateinit var orderDao: OrderDao
    private lateinit var platformDao: PlatformDao
    private lateinit var db: AppDatabase
    
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        orderDao = db.orderDao()
        platformDao = db.platformDao()
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
    
    @Test
    @Throws(Exception::class)
    fun insertAndGetPlatform() = runBlocking {
        // Given
        val platform = PlatformEntity(
            name = "Zomato",
            isEnabled = true,
            minAmount = 100
        )
        
        // When
        platformDao.insertPlatform(platform)
        val retrievedPlatform = platformDao.getPlatformByName("Zomato")
        
        // Then
        assertNotNull(retrievedPlatform)
        assertEquals(platform.name, retrievedPlatform?.name)
        assertEquals(platform.isEnabled, retrievedPlatform?.isEnabled)
        assertEquals(platform.minAmount, retrievedPlatform?.minAmount)
    }
    
    @Test
    @Throws(Exception::class)
    fun updatePlatform() = runBlocking {
        // Given
        val platform = PlatformEntity(
            name = "Zomato",
            isEnabled = true,
            minAmount = 100
        )
        platformDao.insertPlatform(platform)
        
        // When
        val updatedPlatform = platform.copy(minAmount = 150)
        platformDao.updatePlatform(updatedPlatform)
        val retrievedPlatform = platformDao.getPlatformByName("Zomato")
        
        // Then
        assertNotNull(retrievedPlatform)
        assertEquals(150, retrievedPlatform?.minAmount)
    }
    
    @Test
    @Throws(Exception::class)
    fun getEnabledPlatforms() = runBlocking {
        // Given
        val platforms = listOf(
            PlatformEntity(name = "Zomato", isEnabled = true, minAmount = 100),
            PlatformEntity(name = "Swiggy", isEnabled = false, minAmount = 100),
            PlatformEntity(name = "Blinkit", isEnabled = true, minAmount = 100)
        )
        platformDao.insertAllPlatforms(platforms)
        
        // When
        val enabledPlatforms = platformDao.getEnabledPlatforms()
        
        // Then
        assertEquals(2, enabledPlatforms.size)
        assertEquals(setOf("Zomato", "Blinkit"), enabledPlatforms.map { it.name }.toSet())
    }
    
    @Test
    @Throws(Exception::class)
    fun insertAndGetOrder() = runBlocking {
        // Given
        val platform = PlatformEntity(name = "Zomato", isEnabled = true, minAmount = 100)
        platformDao.insertPlatform(platform)
        
        val orderId = UUID.randomUUID().toString()
        val order = OrderEntity(
            id = orderId,
            platformName = "Zomato",
            amount = 150.0,
            timestamp = System.currentTimeMillis(),
            isAccepted = true,
            deliveryStatus = DeliveryStatus.ACCEPTED
        )
        
        // When
        orderDao.insertOrder(order)
        val retrievedOrder = orderDao.getOrderById(orderId)
        
        // Then
        assertNotNull(retrievedOrder)
        assertEquals(order.id, retrievedOrder?.id)
        assertEquals(order.platformName, retrievedOrder?.platformName)
        assertEquals(order.amount, retrievedOrder?.amount)
        assertEquals(order.isAccepted, retrievedOrder?.isAccepted)
        assertEquals(order.deliveryStatus, retrievedOrder?.deliveryStatus)
    }
    
    @Test
    @Throws(Exception::class)
    fun deleteOrder() = runBlocking {
        // Given
        val platform = PlatformEntity(name = "Zomato", isEnabled = true, minAmount = 100)
        platformDao.insertPlatform(platform)
        
        val orderId = UUID.randomUUID().toString()
        val order = OrderEntity(
            id = orderId,
            platformName = "Zomato",
            amount = 150.0,
            timestamp = System.currentTimeMillis(),
            isAccepted = true
        )
        orderDao.insertOrder(order)
        
        // When
        orderDao.deleteOrder(orderId)
        val retrievedOrder = orderDao.getOrderById(orderId)
        
        // Then
        assertNull(retrievedOrder)
    }
    
    @Test
    @Throws(Exception::class)
    fun updateOrderStatus() = runBlocking {
        // Given
        val platform = PlatformEntity(name = "Zomato", isEnabled = true, minAmount = 100)
        platformDao.insertPlatform(platform)
        
        val orderId = UUID.randomUUID().toString()
        val order = OrderEntity(
            id = orderId,
            platformName = "Zomato",
            amount = 150.0,
            timestamp = System.currentTimeMillis(),
            isAccepted = false
        )
        orderDao.insertOrder(order)
        
        // When
        orderDao.updateOrderAcceptanceStatus(orderId, true)
        val retrievedOrder = orderDao.getOrderById(orderId)
        
        // Then
        assertNotNull(retrievedOrder)
        assertEquals(true, retrievedOrder?.isAccepted)
    }
}
