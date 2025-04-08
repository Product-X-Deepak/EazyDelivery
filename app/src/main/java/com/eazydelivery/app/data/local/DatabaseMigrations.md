# Database Migration Documentation

This document provides detailed information about the database migrations in the EazyDelivery app.

## Database Version History

| Version | Changes | Migration Class |
|---------|---------|----------------|
| 1       | Initial database schema | N/A |
| 2       | Added performance indices | MIGRATION_1_2 |
| 3       | Added additional indices and optimized tables | MIGRATION_2_3 |
| 4       | Added new platform fields (packageName, shouldRemove) | MIGRATION_3_4 |

## Migration Details

### Version 1 (Initial Schema)

Initial database schema with the following tables:
- `platforms`: Stores delivery platform information
- `orders`: Stores order information
- `order_notifications`: Stores notification information for orders
- `feedback`: Stores user feedback

### Version 1 → 2 (Performance Indices)

Added performance indices to improve query performance:

```sql
CREATE INDEX IF NOT EXISTS index_orders_platform_name ON orders(platformName)
CREATE INDEX IF NOT EXISTS index_orders_timestamp ON orders(timestamp)
CREATE INDEX IF NOT EXISTS index_orders_delivery_status ON orders(deliveryStatus)

CREATE INDEX IF NOT EXISTS index_order_notifications_platform_name ON order_notifications(platformName)
CREATE INDEX IF NOT EXISTS index_order_notifications_timestamp ON order_notifications(timestamp)
CREATE INDEX IF NOT EXISTS index_order_notifications_priority ON order_notifications(priority)

CREATE INDEX IF NOT EXISTS index_feedback_timestamp ON feedback(timestamp)
CREATE INDEX IF NOT EXISTS index_feedback_rating ON feedback(rating)
```

### Version 2 → 3 (Additional Indices and Optimization)

Added additional indices for better query performance:

```sql
CREATE INDEX IF NOT EXISTS index_orders_amount ON orders(amount)
CREATE INDEX IF NOT EXISTS index_orders_priority ON orders(priority)

CREATE INDEX IF NOT EXISTS index_order_notifications_amount ON order_notifications(amount)

CREATE INDEX IF NOT EXISTS index_platforms_is_enabled ON platforms(isEnabled)
CREATE INDEX IF NOT EXISTS index_platforms_priority ON platforms(priority)
```

### Version 3 → 4 (New Platform Fields)

Added new columns to the platforms table:

```sql
ALTER TABLE platforms ADD COLUMN packageName TEXT NOT NULL DEFAULT ''
ALTER TABLE platforms ADD COLUMN shouldRemove INTEGER NOT NULL DEFAULT 0
```

These fields were added to support:
- Package name storage for each platform
- Marking platforms for removal (e.g., Dunzo)

## Database Optimization

The database is optimized in several ways:

1. **Indices**: Strategic indices on frequently queried columns
2. **VACUUM**: Scheduled VACUUM operations to optimize storage and query planning
3. **Battery-aware optimization**: VACUUM operations only run when the device is charging or has sufficient battery

## Testing Migrations

When adding a new migration:

1. Create a new test in `AppDatabaseMigrationTest` to verify the migration
2. Test both the migration path and the resulting schema
3. Verify that existing data is preserved correctly

## Best Practices

When adding new migrations:

1. Never modify existing migrations
2. Always increment the database version
3. Add comprehensive tests for each migration
4. Document the migration in this file
5. Use `fallbackToDestructiveMigration()` only as a last resort

## Backup and Restore

The database supports backup and restore through:

1. Android's Auto Backup feature (with sensitive data excluded)
2. Manual export/import functionality in the app settings
3. Cloud backup for non-sensitive data

See `DatabaseBackupManager` for implementation details.
