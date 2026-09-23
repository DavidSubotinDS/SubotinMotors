package db.migration;

import org.flywaydb.core.api.migration.*;
import java.sql.*;

/** Never discard legacy inbox/read state; offline copy parity is a prerequisite. */
public class V21__notification_cutover extends BaseJavaMigration {
  @Override public Integer getChecksum(){return 210001;}
  @Override public void migrate(Context context) throws Exception {
    var connection=context.getConnection();boolean mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL");
    try(var s=connection.createStatement()) {
      try(var r=s.executeQuery("SELECT state FROM notification_cutover WHERE id=1")) {
        if(!r.next() || !"PARITY_VERIFIED".equals(r.getString(1)))throw new SQLException("Notification copy parity required before V21");
      }
      boolean legacy;
      try(var tables=connection.getMetaData().getTables(connection.getCatalog(),null,"tb_auction_notification",new String[]{"TABLE"})){legacy=tables.next();}
      if(legacy) {
        boolean carFk=false;
        try(var keys=connection.getMetaData().getImportedKeys(connection.getCatalog(),null,"tb_auction_notification")) {
          while(keys.next())if("fk_notification_car".equals(keys.getString("FK_NAME")))carFk=true;
        }
        if(carFk)s.execute("ALTER TABLE tb_auction_notification DROP "+(mysql?"FOREIGN KEY ":"CONSTRAINT ")+"fk_notification_car");
        s.execute("ALTER TABLE tb_auction_notification RENAME TO archive_notification_tb_auction_notification");
      }
      if(mysql) {
        String user=System.getenv("DB_RUNTIME_USERNAME");
        if(user!=null&&!user.isBlank()) {
          if(!user.matches("[A-Za-z0-9_]{1,64}"))throw new SQLException("Invalid runtime username");
          String schema=connection.getCatalog();
          s.execute("REVOKE IF EXISTS ALL PRIVILEGES ON `"+schema+"`.`archive_notification_tb_auction_notification` FROM '"+user+"'@'%'");
          s.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON `"+schema+"`.`tb_notification_outbox` TO '"+user+"'@'%'");
        }
      }
      s.executeUpdate("UPDATE notification_cutover SET state='CUTOVER_COMPLETE' WHERE id=1");
    }
  }
}
