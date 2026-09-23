package db.migration;

import java.sql.Connection;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Restores least-privilege runtime access after the V19 schema-wide grant was revoked. */
public class V23__checkout_runtime_grants extends BaseJavaMigration {
  @Override
  public Integer getChecksum() {
    return 230001;
  }

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    if (!"MySQL".equals(connection.getMetaData().getDatabaseProductName())) return;
    String runtimeUser = System.getenv("DB_RUNTIME_USERNAME");
    if (runtimeUser == null || runtimeUser.isBlank()) return;
    if (!runtimeUser.matches("[A-Za-z0-9_]{1,64}")) {
      throw new IllegalStateException("Invalid runtime database user");
    }
    String schema = connection.getCatalog();
    if (schema == null || !schema.matches("[A-Za-z0-9_]{1,64}")) {
      throw new IllegalStateException("Invalid checkout schema");
    }
    try (var statement = connection.createStatement()) {
      for (String table : new String[] {
          "tb_checkout_attempt", "tb_stock_hold", "tb_checkout_webhook_inbox" }) {
        statement.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON `" + schema + "`.`" + table
            + "` TO '" + runtimeUser + "'@'%'");
      }
    }
  }
}
