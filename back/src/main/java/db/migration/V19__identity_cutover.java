package db.migration;

import org.flywaydb.core.api.migration.*;
import java.sql.*;
import java.util.*;

/** Runs only after the maintenance copy tool has verified parity under a write freeze. */
public class V19__identity_cutover extends BaseJavaMigration {
  @Override public Integer getChecksum() { return 190001; }
  @Override public void migrate(Context context) throws Exception {
    var connection=context.getConnection();
    try(var statement=connection.createStatement()) {
      try(var rows=statement.executeQuery("SELECT state FROM identity_cutover WHERE id=1")) {
        if(!rows.next() || !"PARITY_VERIFIED".equals(rows.getString(1))) throw new SQLException("Identity copy parity is required before V19");
      }
      // Exact allowlist. Column values, nullability, uniqueness and indexes are unchanged.
      String[][] references={
        {"tb_car","fk_car_user"},{"tb_car_bid","fk_bid_user"},{"tb_test_drive","fk_test_drive_user"},
        {"tb_payment_account","fk_payment_account_user"},{"tb_payment_order","fk_payment_order_buyer"},
        {"tb_payment_order","fk_payment_order_seller"},{"tb_cart_item","fk_cart_item_user"},
        {"tb_store_order","fk_store_order_user"},{"tb_auction_follow","fk_auction_follow_user"},
        {"tb_auction_notification","fk_notification_user"},{"tb_listing_comment","fk_listing_comment_user"},
        {"tb_car_listing","fk_car_listing_seller"},{"tb_listing_test_ride","fk_listing_test_ride_user"},
        {"tb_listing_deposit","fk_listing_deposit_buyer"}};
      boolean mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL");
      for(var ref:references) {
        if(!mysql || constraintExists(connection, ref[0], ref[1]))
          statement.execute("ALTER TABLE "+ref[0]+(mysql ? " DROP FOREIGN KEY " : " DROP CONSTRAINT ")+ref[1]);
      }
      // Immutable offline archives retain original data and their local FK graph.
      for(String table:List.of("tb_user","tb_user_profile","tb_role","tb_profile_picture","tb_password_reset_token")) {
        if(tableExists(connection, table) && !tableExists(connection, "archive_identity_"+table))
          statement.execute("ALTER TABLE "+table+" RENAME TO archive_identity_"+table);
      }
      if(mysql) revokeRuntimeArchiveAccess(connection);
      statement.executeUpdate("UPDATE identity_cutover SET state='CUTOVER_COMPLETE' WHERE id=1");
    }
  }
  private static boolean tableExists(Connection connection,String table) throws SQLException {
    try(var rows=connection.getMetaData().getTables(connection.getCatalog(),null,table,new String[]{"TABLE"})) { return rows.next(); }
  }
  private static boolean constraintExists(Connection connection,String table,String constraint) throws SQLException {
    try(var rows=connection.getMetaData().getImportedKeys(connection.getCatalog(),null,table)) {
      while(rows.next()) if(constraint.equals(rows.getString("FK_NAME"))) return true;
      return false;
    }
  }
  private static void revokeRuntimeArchiveAccess(Connection connection) throws SQLException {
    String user=System.getenv("DB_RUNTIME_USERNAME");
    if(user==null || user.isBlank()) return;
    if(!user.matches("[A-Za-z0-9_]{1,64}")) throw new SQLException("Invalid runtime database user");
    String catalog=connection.getCatalog();
    try(var revoke=connection.createStatement(); var query=connection.createStatement(); var grant=connection.createStatement()) {
      revoke.execute("REVOKE IF EXISTS ALL PRIVILEGES ON `"+catalog+"`.* FROM '"+user+"'@'%'");
      revoke.execute("REVOKE IF EXISTS GRANT OPTION ON `"+catalog+"`.* FROM '"+user+"'@'%'");
      try(var tables=query.executeQuery("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='"+catalog+"' AND TABLE_TYPE='BASE TABLE'")) {
        while(tables.next()) {
          String table=tables.getString(1);
          if(table.startsWith("archive_identity_") || table.equals("identity_cutover")) continue;
          grant.execute("GRANT ALL PRIVILEGES ON `"+catalog+"`.`"+table+"` TO '"+user+"'@'%'");
        }
      }
    }
  }
}
