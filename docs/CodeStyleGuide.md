# Code Style Guide

This guide documents the code style conventions used in the EazyDelivery app and provides guidelines for writing clean, maintainable code.

## Table of Contents

1. [Kotlin Style Guide](#kotlin-style-guide)
2. [XML Style Guide](#xml-style-guide)
3. [Architecture Guidelines](#architecture-guidelines)
4. [Documentation Guidelines](#documentation-guidelines)
5. [Testing Guidelines](#testing-guidelines)
6. [Performance Guidelines](#performance-guidelines)
7. [Error Handling Guidelines](#error-handling-guidelines)
8. [Resource Guidelines](#resource-guidelines)
9. [Best Practices](#best-practices)

## Kotlin Style Guide

### Naming Conventions

- **Packages**: Package names are all lowercase, with consecutive words simply concatenated together (no underscores).
  ```kotlin
  package com.eazydelivery.app.util
  ```

- **Classes**: Class names are written in UpperCamelCase.
  ```kotlin
  class DatabaseBackupManager
  ```

- **Functions**: Function names are written in lowerCamelCase.
  ```kotlin
  fun createBackup(): Uri?
  ```

- **Constants**: Constants (properties marked with `const`, or top-level or object `val` properties with no custom `get` function that hold deeply immutable data) are written in uppercase with underscores separating words.
  ```kotlin
  const val MAX_RETRY_COUNT = 3
  ```

- **Properties**: Non-constant properties are written in lowerCamelCase.
  ```kotlin
  val orderCount: Int
  var isServiceActive: Boolean
  ```

- **Parameters**: Parameter names are written in lowerCamelCase.
  ```kotlin
  fun createBackup(context: Context, fileName: String)
  ```

### Formatting

- **Indentation**: Use 4 spaces for indentation.
  ```kotlin
  fun example() {
      val x = 1
      if (x > 0) {
          println("Positive")
      }
  }
  ```

- **Line Length**: Maximum line length is 100 characters.

- **Line Breaks**: When a line is broken, the continuation lines are indented 4 spaces from the original line.
  ```kotlin
  val longExpression = foo() +
      bar() +
      baz()
  ```

- **Braces**: Braces follow the Kernighan and Ritchie style ("Egyptian brackets") for non-empty blocks and block-like constructs.
  ```kotlin
  if (condition) {
      // code
  } else {
      // code
  }
  ```

- **Spacing**: Use a single space after control flow keywords (`if`, `when`, `for`, `while`).
  ```kotlin
  if (condition) {
      // code
  }
  ```

- **Imports**: Imports should be ordered alphabetically, with no wildcards.
  ```kotlin
  import android.content.Context
  import android.os.Bundle
  import androidx.appcompat.app.AppCompatActivity
  import com.eazydelivery.app.R
  ```

### Coding Conventions

- **Use Expression Bodies**: Use expression bodies for simple functions.
  ```kotlin
  fun getMessage(error: AppError): String = errorMessageProvider.getMessage(error)
  ```

- **Use String Templates**: Use string templates instead of string concatenation.
  ```kotlin
  val message = "Hello, $name!"
  ```

- **Use Named Arguments**: Use named arguments for functions with multiple parameters.
  ```kotlin
  createBackup(context = context, fileName = "backup.db")
  ```

- **Use Type Inference**: Let the compiler infer the type when it's obvious.
  ```kotlin
  val message = "Hello, World!" // Type is inferred as String
  ```

- **Use Immutable Collections**: Use immutable collections when the collection doesn't need to be modified.
  ```kotlin
  val list = listOf(1, 2, 3)
  ```

- **Use Extension Functions**: Use extension functions to add functionality to existing classes.
  ```kotlin
  fun String.isValidEmail(): Boolean {
      return Patterns.EMAIL_ADDRESS.matcher(this).matches()
  }
  ```

- **Use Scope Functions**: Use scope functions (`let`, `run`, `with`, `apply`, `also`) appropriately.
  ```kotlin
  file?.let {
      // Code that uses the non-null file
  }
  ```

## XML Style Guide

### Naming Conventions

- **Layout Files**: Layout file names are written in lowercase with underscores separating words, and start with the type of layout.
  ```
  activity_main.xml
  fragment_home.xml
  item_order.xml
  ```

- **Resource IDs**: Resource IDs are written in lowercase with underscores separating words, and start with the type of resource.
  ```xml
  android:id="@+id/text_title"
  android:id="@+id/button_submit"
  android:id="@+id/image_logo"
  ```

- **Resource Files**: Resource file names are written in lowercase with underscores separating words.
  ```
  colors.xml
  strings.xml
  dimens.xml
  ```

### Formatting

- **Indentation**: Use 4 spaces for indentation.
  ```xml
  <LinearLayout
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:orientation="vertical">
      
      <TextView
          android:layout_width="wrap_content"
          android:layout_height="wrap_content"
          android:text="Hello, World!" />
      
  </LinearLayout>
  ```

- **Line Length**: Maximum line length is 100 characters.

- **Attributes**: Each attribute should be on its own line.
  ```xml
  <TextView
      android:id="@+id/text_title"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:text="@string/title"
      android:textSize="18sp" />
  ```

- **Closing Tags**: Self-closing tags should be used for elements with no children.
  ```xml
  <ImageView
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:src="@drawable/logo" />
  ```

### Resource Organization

- **String Resources**: String resources should be organized by feature or screen.
  ```xml
  <!-- Login Screen -->
  <string name="login_title">Login</string>
  <string name="login_username_hint">Username</string>
  <string name="login_password_hint">Password</string>
  <string name="login_button">Login</string>
  
  <!-- Home Screen -->
  <string name="home_title">Home</string>
  <string name="home_welcome">Welcome, %s!</string>
  ```

- **Color Resources**: Color resources should be named descriptively.
  ```xml
  <color name="primary">#1976D2</color>
  <color name="primary_dark">#0D47A1</color>
  <color name="accent">#FF5722</color>
  ```

- **Dimension Resources**: Dimension resources should be named descriptively.
  ```xml
  <dimen name="text_size_small">12sp</dimen>
  <dimen name="text_size_medium">16sp</dimen>
  <dimen name="text_size_large">20sp</dimen>
  <dimen name="margin_small">8dp</dimen>
  <dimen name="margin_medium">16dp</dimen>
  <dimen name="margin_large">24dp</dimen>
  ```

## Architecture Guidelines

The EazyDelivery app follows the MVVM (Model-View-ViewModel) architecture pattern.

### Components

- **Model**: Represents the data and business logic of the app.
  - **Entity**: Represents a database table.
  - **Repository**: Provides a clean API for data access.
  - **Data Source**: Provides data from a specific source (local database, network, etc.).

- **View**: Represents the UI of the app.
  - **Activity**: Hosts fragments and handles navigation.
  - **Fragment**: Displays UI and handles user interactions.
  - **Adapter**: Binds data to RecyclerView.

- **ViewModel**: Acts as a bridge between the Model and the View.
  - **ViewModel**: Holds UI-related data and handles UI logic.
  - **LiveData/Flow**: Provides observable data holders.

### Guidelines

- **Separation of Concerns**: Each component should have a single responsibility.
  - **Model**: Handles data and business logic.
  - **View**: Displays UI and handles user interactions.
  - **ViewModel**: Handles UI logic and provides data to the View.

- **Dependency Injection**: Use Hilt for dependency injection.
  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  object AppModule {
      @Provides
      @Singleton
      fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
          return Room.databaseBuilder(context, AppDatabase::class.java, "eazydelivery.db")
              .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
              .build()
      }
  }
  ```

- **Repository Pattern**: Use repositories to abstract data sources.
  ```kotlin
  class OrderRepository @Inject constructor(
      private val orderDao: OrderDao,
      private val orderApi: OrderApi
  ) {
      fun getOrders(): Flow<List<Order>> {
          return orderDao.getAllOrders().map { entities ->
              entities.map { it.toOrder() }
          }
      }
      
      suspend fun refreshOrders() {
          val orders = orderApi.getOrders()
          orderDao.insertAll(orders.map { it.toOrderEntity() })
      }
  }
  ```

- **Immutable Data**: Use immutable data classes for models.
  ```kotlin
  data class Order(
      val id: String,
      val customerId: String,
      val amount: Double,
      val status: OrderStatus
  )
  ```

- **Single Source of Truth**: The database should be the single source of truth for data.
  ```kotlin
  class OrderRepository @Inject constructor(
      private val orderDao: OrderDao,
      private val orderApi: OrderApi
  ) {
      fun getOrders(): Flow<List<Order>> {
          // Always get data from the database
          return orderDao.getAllOrders().map { entities ->
              entities.map { it.toOrder() }
          }
      }
      
      suspend fun refreshOrders() {
          // Update the database with data from the API
          val orders = orderApi.getOrders()
          orderDao.insertAll(orders.map { it.toOrderEntity() })
      }
  }
  ```

## Documentation Guidelines

### Code Documentation

- **Class Documentation**: Document all classes with a description of their purpose and responsibilities.
  ```kotlin
  /**
   * Manages database backups and restores.
   * Provides functionality for creating, restoring, and managing database backups.
   */
  class DatabaseBackupManager @Inject constructor(
      @ApplicationContext private val context: Context,
      private val securityManager: SecurityManager,
      private val errorHandler: ErrorHandler
  ) {
      // ...
  }
  ```

- **Function Documentation**: Document all public functions with a description of their purpose, parameters, and return value.
  ```kotlin
  /**
   * Creates a backup of the database.
   * 
   * @return The URI of the backup file, or null if the backup failed.
   */
  suspend fun createBackup(): Uri? {
      // ...
  }
  ```

- **Property Documentation**: Document all public properties with a description of their purpose.
  ```kotlin
  /**
   * The maximum number of retry attempts for network operations.
   */
  private val maxRetryCount = 3
  ```

- **Code Comments**: Use comments to explain complex or non-obvious code.
  ```kotlin
  // Use exponential backoff for retries
  val nextDelay = min(currentDelay * 2.0.pow(attempt).toLong(), maxDelayMs)
  ```

### File Documentation

- **File Header**: Include a file header with a description of the file's purpose.
  ```kotlin
  /**
   * Database backup and restore functionality.
   * This file contains classes and functions for backing up and restoring the database.
   */
  ```

### Documentation Files

- **README.md**: Include a README.md file with an overview of the project, setup instructions, and other relevant information.
- **Documentation Files**: Include documentation files for specific aspects of the project, such as performance optimization, error handling, and database management.

## Testing Guidelines

### Test Types

- **Unit Tests**: Test individual components in isolation.
  ```kotlin
  @Test
  fun `from should map UnknownHostException to Network NoConnection`() {
      // Given
      val exception = UnknownHostException("No internet")
      
      // When
      val appError = AppError.from(exception)
      
      // Then
      assertTrue(appError is AppError.Network.NoConnection)
      assertEquals(exception, appError.cause)
  }
  ```

- **Integration Tests**: Test component interactions.
  ```kotlin
  @Test
  fun `getPlatformStatsForPeriod should return correct stats`() = runBlocking {
      // Given
      val startDate = "2023-01-01"
      val endDate = "2023-12-31"
      
      // When
      val stats = optimizedQueries.getPlatformStatsForPeriod(startDate, endDate)
      
      // Then
      assertTrue(stats.isNotEmpty())
      stats.forEach { stat ->
          assertTrue(stat.orderCount > 0)
          assertTrue(stat.totalEarnings > 0)
      }
  }
  ```

- **UI Tests**: Test the user interface.
  ```kotlin
  @Test
  fun validatePerformanceButtonOpensValidationScreen() {
      // Launch the activity
      val scenario = ActivityScenario.launch(MainActivity::class.java)
      
      // Click the validate performance button
      onView(withId(R.id.button_validate_performance)).perform(click())
      
      // Verify that the validation screen is displayed
      onView(withId(R.id.title)).check(matches(withText(R.string.performance_validation)))
  }
  ```

### Test Naming

- **Test Names**: Test names should be descriptive and follow the pattern `should_do_something_when_something`.
  ```kotlin
  @Test
  fun `should return user-friendly message for network error`() {
      // ...
  }
  ```

### Test Structure

- **Given-When-Then**: Structure tests using the Given-When-Then pattern.
  ```kotlin
  @Test
  fun `should return user-friendly message for network error`() {
      // Given
      val error = AppError.Network.NoConnection()
      
      // When
      val message = errorMessageProvider.getMessage(error)
      
      // Then
      assertEquals("No internet connection available.", message)
  }
  ```

### Test Coverage

- **Code Coverage**: Aim for high code coverage, especially for critical components.
- **Edge Cases**: Test edge cases and error conditions.
- **Performance**: Test performance-critical code.

## Performance Guidelines

### General

- **Avoid Blocking the Main Thread**: Perform heavy operations on background threads.
  ```kotlin
  viewModelScope.launch(Dispatchers.IO) {
      // Perform heavy operation
      val result = repository.fetchData()
      
      // Update UI on the main thread
      withContext(Dispatchers.Main) {
          _data.value = result
      }
  }
  ```

- **Use Coroutines**: Use Kotlin coroutines for asynchronous operations.
  ```kotlin
  suspend fun fetchData(): Result<Data> {
      return withContext(Dispatchers.IO) {
          try {
              val data = api.fetchData()
              Result.success(data)
          } catch (e: Exception) {
              Result.failure(e)
          }
      }
  }
  ```

- **Optimize Algorithms**: Use efficient algorithms and data structures.
  ```kotlin
  // Use a more efficient algorithm for finding the maximum value
  val max = list.maxOrNull() ?: 0
  ```

- **Avoid Excessive Object Creation**: Reuse objects when possible to reduce garbage collection.
  ```kotlin
  // Reuse the same buffer for multiple operations
  val buffer = ByteBuffer.allocate(1024)
  ```

### UI Performance

- **Use RecyclerView**: Use RecyclerView for displaying lists.
  ```kotlin
  recyclerView.apply {
      layoutManager = LinearLayoutManager(context)
      adapter = OrderAdapter()
  }
  ```

- **Use ViewHolder Pattern**: Use the ViewHolder pattern for RecyclerView adapters.
  ```kotlin
  class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val titleTextView: TextView = itemView.findViewById(R.id.text_title)
      private val amountTextView: TextView = itemView.findViewById(R.id.text_amount)
      
      fun bind(order: Order) {
          titleTextView.text = order.title
          amountTextView.text = order.amount.toString()
      }
  }
  ```

- **Optimize Layouts**: Use ConstraintLayout for complex layouts.
  ```xml
  <androidx.constraintlayout.widget.ConstraintLayout
      android:layout_width="match_parent"
      android:layout_height="match_parent">
      
      <TextView
          android:id="@+id/text_title"
          android:layout_width="0dp"
          android:layout_height="wrap_content"
          app:layout_constraintStart_toStartOf="parent"
          app:layout_constraintEnd_toEndOf="parent"
          app:layout_constraintTop_toTopOf="parent" />
      
  </androidx.constraintlayout.widget.ConstraintLayout>
  ```

- **Avoid Nested Layouts**: Avoid deeply nested layouts.
  ```xml
  <!-- Avoid this -->
  <LinearLayout>
      <LinearLayout>
          <LinearLayout>
              <TextView />
          </LinearLayout>
      </LinearLayout>
  </LinearLayout>
  
  <!-- Use this instead -->
  <ConstraintLayout>
      <TextView
          app:layout_constraintStart_toStartOf="parent"
          app:layout_constraintTop_toTopOf="parent" />
  </ConstraintLayout>
  ```

### Database Performance

- **Use Indexes**: Add indexes for frequently queried columns.
  ```kotlin
  @Entity(
      tableName = "orders",
      indices = [
          Index("timestamp"),
          Index("platformName")
      ]
  )
  data class OrderEntity(
      @PrimaryKey
      val id: String,
      val timestamp: String,
      val platformName: String,
      // ...
  )
  ```

- **Optimize Queries**: Optimize queries for better performance.
  ```kotlin
  @Query("""
      SELECT 
          platformName, 
          COUNT(*) as orderCount, 
          SUM(amount) as totalEarnings
      FROM orders 
      WHERE timestamp BETWEEN :startDate AND :endDate
      GROUP BY platformName
  """)
  suspend fun getPlatformStatsForPeriod(startDate: String, endDate: String): List<PlatformStatEntity>
  ```

- **Use Transactions**: Use transactions for multiple database operations.
  ```kotlin
  @Transaction
  suspend fun updateOrderAndNotification(order: OrderEntity, notification: OrderNotificationEntity) {
      orderDao.update(order)
      orderNotificationDao.update(notification)
  }
  ```

## Error Handling Guidelines

### Error Classification

- **Use AppError**: Use the `AppError` sealed class for error classification.
  ```kotlin
  sealed class AppError(
      open val message: String,
      open val cause: Throwable? = null
  ) {
      sealed class Network(...) : AppError(...)
      sealed class Api(...) : AppError(...)
      sealed class Database(...) : AppError(...)
      sealed class Permission(...) : AppError(...)
      sealed class Feature(...) : AppError(...)
      class Unexpected(...) : AppError(...)
  }
  ```

- **Convert Exceptions**: Convert exceptions to `AppError` for better classification.
  ```kotlin
  fun handleException(tag: String, throwable: Throwable): AppError {
      val appError = if (throwable is AppError) throwable else AppError.from(throwable)
      // ...
      return appError
  }
  ```

### Error Handling

- **Handle All Errors**: Handle all possible errors, even unexpected ones.
  ```kotlin
  try {
      // Perform operation
  } catch (e: Exception) {
      errorHandler.handleException("tag", e)
  }
  ```

- **Provide User Feedback**: Show user-friendly error messages.
  ```kotlin
  try {
      // Perform operation
  } catch (e: Exception) {
      val appError = errorHandler.handleException("tag", e)
      val message = errorMessageProvider.getMessage(appError)
      showErrorMessage(message)
  }
  ```

- **Implement Recovery**: Implement recovery mechanisms for recoverable errors.
  ```kotlin
  try {
      // Perform operation
  } catch (e: Exception) {
      val appError = errorHandler.handleException("tag", e)
      val recoveryAction = errorRecoveryManager.getRecoveryAction(appError)
      
      when (recoveryAction) {
          RecoveryAction.Retry -> retry()
          RecoveryAction.RestartApp -> restartApp()
          RecoveryAction.OpenNetworkSettings -> openNetworkSettings()
          else -> showErrorMessage(errorMessageProvider.getMessage(appError))
      }
  }
  ```

## Resource Guidelines

### String Resources

- **Use String Resources**: Use string resources for all user-visible text.
  ```xml
  <string name="error_no_internet_connection">No internet connection available.</string>
  ```

- **Use String Formatting**: Use string formatting for dynamic text.
  ```xml
  <string name="welcome_message">Welcome, %s!</string>
  ```

- **Organize by Feature**: Organize string resources by feature or screen.
  ```xml
  <!-- Login Screen -->
  <string name="login_title">Login</string>
  <string name="login_username_hint">Username</string>
  <string name="login_password_hint">Password</string>
  <string name="login_button">Login</string>
  ```

### Dimension Resources

- **Use Dimension Resources**: Use dimension resources for sizes and margins.
  ```xml
  <dimen name="text_size_small">12sp</dimen>
  <dimen name="text_size_medium">16sp</dimen>
  <dimen name="text_size_large">20sp</dimen>
  <dimen name="margin_small">8dp</dimen>
  <dimen name="margin_medium">16dp</dimen>
  <dimen name="margin_large">24dp</dimen>
  ```

### Color Resources

- **Use Color Resources**: Use color resources for all colors.
  ```xml
  <color name="primary">#1976D2</color>
  <color name="primary_dark">#0D47A1</color>
  <color name="accent">#FF5722</color>
  ```

- **Use Theme Attributes**: Use theme attributes for colors that should change with the theme.
  ```xml
  <TextView
      android:textColor="?attr/colorPrimary" />
  ```

### Drawable Resources

- **Use Vector Drawables**: Use vector drawables for icons and simple graphics.
  ```xml
  <vector xmlns:android="http://schemas.android.com/apk/res/android"
      android:width="24dp"
      android:height="24dp"
      android:viewportWidth="24"
      android:viewportHeight="24">
      <path
          android:fillColor="#000000"
          android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM10,17l-5,-5 1.41,-1.41L10,14.17l7.59,-7.59L19,8l-9,9z" />
  </vector>
  ```

## Best Practices

### Code Quality

- **Keep Functions Small**: Functions should do one thing and do it well.
  ```kotlin
  // Bad
  fun processOrder(order: Order) {
      // Validate order
      if (order.amount <= 0) {
          throw IllegalArgumentException("Order amount must be positive")
      }
      
      // Save order to database
      orderDao.insert(order.toOrderEntity())
      
      // Send notification
      notificationManager.sendOrderNotification(order)
  }
  
  // Good
  fun processOrder(order: Order) {
      validateOrder(order)
      saveOrder(order)
      notifyOrder(order)
  }
  
  private fun validateOrder(order: Order) {
      if (order.amount <= 0) {
          throw IllegalArgumentException("Order amount must be positive")
      }
  }
  
  private fun saveOrder(order: Order) {
      orderDao.insert(order.toOrderEntity())
  }
  
  private fun notifyOrder(order: Order) {
      notificationManager.sendOrderNotification(order)
  }
  ```

- **Avoid Magic Numbers**: Use named constants for magic numbers.
  ```kotlin
  // Bad
  if (batteryLevel < 15) {
      // Low battery
  }
  
  // Good
  private const val LOW_BATTERY_THRESHOLD = 15
  
  if (batteryLevel < LOW_BATTERY_THRESHOLD) {
      // Low battery
  }
  ```

- **Use Kotlin Features**: Use Kotlin features like extension functions, data classes, and scope functions.
  ```kotlin
  // Extension function
  fun String.isValidEmail(): Boolean {
      return Patterns.EMAIL_ADDRESS.matcher(this).matches()
  }
  
  // Data class
  data class Order(
      val id: String,
      val amount: Double,
      val status: OrderStatus
  )
  
  // Scope function
  file?.let {
      // Code that uses the non-null file
  }
  ```

- **Follow SOLID Principles**: Follow SOLID principles for better code organization.
  - **Single Responsibility Principle**: A class should have only one reason to change.
  - **Open/Closed Principle**: Classes should be open for extension but closed for modification.
  - **Liskov Substitution Principle**: Subtypes must be substitutable for their base types.
  - **Interface Segregation Principle**: Clients should not be forced to depend on methods they do not use.
  - **Dependency Inversion Principle**: High-level modules should not depend on low-level modules. Both should depend on abstractions.

### Security

- **Secure Data Storage**: Use secure methods for storing sensitive data.
  ```kotlin
  val encryptedData = securityManager.encrypt(sensitiveData)
  sharedPreferences.edit().putString("sensitive_data", encryptedData).apply()
  ```

- **Input Validation**: Validate all user input.
  ```kotlin
  fun validateEmail(email: String): Boolean {
      return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
  }
  ```

- **Permission Handling**: Handle permissions properly.
  ```kotlin
  if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
  } else {
      // Permission already granted
      getLocation()
  }
  ```

### Accessibility

- **Content Descriptions**: Provide content descriptions for all UI elements.
  ```xml
  <ImageView
      android:contentDescription="@string/logo_description" />
  ```

- **Text Sizes**: Use scalable text sizes.
  ```xml
  <TextView
      android:textSize="@dimen/text_size_medium" />
  ```

- **Color Contrast**: Ensure sufficient color contrast.
  ```xml
  <TextView
      android:textColor="@color/text_primary"
      android:background="@color/background" />
  ```

By following these guidelines, you can write clean, maintainable code that is easy to understand and modify.
