# EazyDelivery

EazyDelivery is an Android app that helps delivery drivers manage and automate their work across multiple delivery platforms.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Documentation](#documentation)
- [Performance Optimizations](#performance-optimizations)
- [Error Handling](#error-handling)
- [Database Management](#database-management)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Overview

EazyDelivery is designed to help delivery drivers who work with multiple delivery platforms (like Swiggy, Zomato, Uber Eats, Bigbasket, etc.) by providing a unified interface for managing orders, automating order acceptance, and tracking earnings.

The app uses accessibility services and notification listeners to interact with delivery apps, analyze their screens, and automatically accept orders based on user preferences.

## Features

- **Multi-Platform Support**: Works with multiple delivery platforms including Swiggy, Zomato, Uber Eats, Bigbasket, and more.
- **Automatic Order Acceptance**: Automatically accepts orders based on user-defined criteria.
- **Earnings Tracking**: Tracks earnings across all platforms.
- **Order History**: Maintains a history of all orders.
- **Statistics and Analytics**: Provides statistics and analytics on earnings, order frequency, and more.
- **Notification Management**: Manages notifications from delivery apps.
- **User Preferences**: Allows users to customize app behavior.
- **Dark Mode**: Supports dark mode for better visibility in low-light conditions.
- **Backup and Restore**: Allows users to backup and restore their data.

## Architecture

EazyDelivery follows the MVVM (Model-View-ViewModel) architecture pattern and uses the following technologies:

- **Kotlin**: The app is written entirely in Kotlin.
- **Coroutines**: Used for asynchronous programming.
- **Flow**: Used for reactive programming.
- **Hilt**: Used for dependency injection.
- **Room**: Used for database management.
- **ViewModel**: Used for UI-related data handling.
- **LiveData**: Used for observable data holders.
- **Navigation Component**: Used for navigation between screens.
- **Material Design**: Used for UI components.
- **Firebase**: Used for analytics, crash reporting, and performance monitoring.
- **TensorFlow Lite**: Used for screen analysis and object detection.

## Getting Started

### Prerequisites

- Android Studio Arctic Fox (2020.3.1) or higher
- Android SDK 33 or higher
- Kotlin 1.7.0 or higher

### Building the App

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/eazydelivery.git
   ```

2. Open the project in Android Studio.

3. Sync the project with Gradle files.

4. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```

### Running the App

1. Connect an Android device or start an emulator.

2. Run the app:
   ```bash
   ./gradlew installDebug
   ```

### Setting Up Development Environment

1. Install Android Studio.

2. Install the Android SDK.

3. Configure the Android SDK:
   - Open Android Studio.
   - Go to Settings > Appearance & Behavior > System Settings > Android SDK.
   - Install the required SDK platforms and tools.

4. Configure the project:
   - Open the project in Android Studio.
   - Sync the project with Gradle files.
   - Build the project.

## Documentation

The project includes comprehensive documentation:

- [Performance Optimization Guide](docs/PerformanceOptimizationGuide.md): Documents the performance optimizations implemented in the app.
- [Error Handling Guide](docs/ErrorHandlingGuide.md): Documents the error handling system implemented in the app.
- [Database Management Guide](docs/DatabaseManagementGuide.md): Documents the database management system implemented in the app.
- [Database Migrations](docs/DatabaseMigrations.md): Documents the database migrations implemented in the app.

## Performance Optimizations

The app includes several performance optimizations:

- **Screen Analysis Optimization**: Optimized screen analysis for better performance.
- **Database Optimization**: Optimized database queries and added indexes for better performance.
- **Background Service Optimization**: Optimized background services for better battery life.
- **Memory Management**: Implemented efficient memory management to reduce memory usage.
- **Performance Monitoring**: Added performance monitoring to track and improve performance.

For more details, see the [Performance Optimization Guide](docs/PerformanceOptimizationGuide.md).

## Error Handling

The app includes a comprehensive error handling system:

- **Error Classification**: Classifies errors into specific categories.
- **User-Friendly Error Messages**: Provides user-friendly error messages.
- **Error Recovery**: Implements recovery mechanisms for recoverable errors.
- **Error Reporting**: Reports errors to Firebase Crashlytics for monitoring.

For more details, see the [Error Handling Guide](docs/ErrorHandlingGuide.md).

## Database Management

The app includes a robust database management system:

- **Schema Design**: Well-designed schema with appropriate relationships.
- **Migrations**: Comprehensive migration system for handling schema changes.
- **Optimization**: Optimization techniques for better performance.
- **Backup and Restore**: Functionality for backing up and restoring the database.

For more details, see the [Database Management Guide](docs/DatabaseManagementGuide.md).

## Testing

The app includes comprehensive tests:

- **Unit Tests**: Tests for individual components.
- **Integration Tests**: Tests for component interactions.
- **UI Tests**: Tests for the user interface.
- **Database Tests**: Tests for database operations and migrations.
- **Performance Tests**: Tests for performance optimizations.

### Running Tests

1. Run unit tests:
   ```bash
   ./gradlew test
   ```

2. Run instrumented tests:
   ```bash
   ./gradlew connectedAndroidTest
   ```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Guidelines

- Follow the coding style of the project.
- Write tests for your code.
- Document your code.
- Keep your changes focused.
- Write good commit messages.

### Process

1. Fork the repository.
2. Create a feature branch.
3. Make your changes.
4. Run tests.
5. Submit a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
