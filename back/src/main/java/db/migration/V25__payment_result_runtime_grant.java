package db.migration;

import java.sql.Connection;
import org.flywaydb.core.api.migration.*;

public class V25__payment_result_runtime_grant extends BaseJavaMigration {
  @Override public Integer getChecksum(){return 250001;}
  @Override public void migrate(Context context)throws Exception{
    Connection c=context.getConnection();if(!"MySQL".equals(c.getMetaData().getDatabaseProductName()))return;
    String user=System.getenv("DB_RUNTIME_USERNAME"),schema=c.getCatalog();
    if(user==null||user.isBlank())return;
    if(!user.matches("[A-Za-z0-9_]{1,64}")||schema==null||!schema.matches("[A-Za-z0-9_]{1,64}"))throw new IllegalStateException("Invalid payment result grant target");
    try(var s=c.createStatement()){s.execute("GRANT SELECT,INSERT,UPDATE,DELETE ON `"+schema+"`.`tb_payment_result_inbox` TO '"+user+"'@'%'");}
  }
}
