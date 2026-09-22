package lithan.autostrada.identity.migration;

import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Offline, explicit maintenance command. Never registered in the HTTP application. */
public final class IdentityCopy {
  private static final List<String> TABLES=List.of("tb_user","tb_user_profile","tb_role","tb_profile_picture","tb_password_reset_token");
  public static void main(String[] args) throws Exception {
    if(!"I_HAVE_STOPPED_ALL_WRITERS".equals(System.getenv("CUTOVER_WRITE_FREEZE")))
      throw new IllegalStateException("Stop all writers and acknowledge CUTOVER_WRITE_FREEZE first");
    if(!"UTC".equals(System.getenv("CUTOVER_SOURCE_TIMEZONE")))
      throw new IllegalStateException("Verify the source timestamp zone; this tool requires UTC without conversion");
    try(var source=DriverManager.getConnection(env("CUTOVER_SOURCE_URL"),env("CUTOVER_SOURCE_USER"),env("CUTOVER_SOURCE_PASSWORD"));
        var target=DriverManager.getConnection(env("CUTOVER_TARGET_URL"),env("CUTOVER_TARGET_USER"),env("CUTOVER_TARGET_PASSWORD"))) {
      if(Objects.equals(source.getMetaData().getURL(),target.getMetaData().getURL())) throw new IllegalStateException("Distinct schemas required");
      copy(source,target);
    } catch(Exception error) {
      // Driver exceptions may include connection credentials or row contents.
      System.err.println("Identity copy failed; keep writers stopped and inspect private database diagnostics.");
      System.exit(1);
    }
  }
  static String env(String name) {String v=System.getenv(name);if(v==null || v.isBlank())throw new IllegalArgumentException("Missing "+name);return v;}
  public static Map<String,String> copy(Connection source,Connection target) throws Exception {
    var before=snapshot(source);
    target.setAutoCommit(false);
    try {
      boolean empty=true;
      for(String table:TABLES) try(var s=target.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)) {r.next();empty &= r.getLong(1)==0;}
      if(empty) for(String table:TABLES) {
        try(var s=source.createStatement();var rows=s.executeQuery("SELECT * FROM "+table+" ORDER BY 1")) {
          var meta=rows.getMetaData();int count=meta.getColumnCount();var names=new ArrayList<String>();
          for(int i=1;i<=count;i++)names.add(meta.getColumnName(i));
          String sql="INSERT INTO "+table+" ("+String.join(",",names)+") VALUES ("+String.join(",",Collections.nCopies(count,"?"))+")";
          try(var insert=target.prepareStatement(sql)) {
            while(rows.next()) {for(int i=1;i<=count;i++)insert.setObject(i,rows.getObject(i));insert.executeUpdate();}
          }
        }
      }
      if(!before.equals(snapshot(source)) || !before.equals(snapshot(target))) throw new SQLException("Copy parity mismatch");
      target.commit();
      if(target.getMetaData().getDatabaseProductName().equals("H2")) for(String table:TABLES)
        try(var s=target.createStatement();var r=s.executeQuery("SELECT * FROM "+table+" WHERE 1=0")) {
          String id=r.getMetaData().getColumnName(1);long next;
          try(var q=target.createStatement();var max=q.executeQuery("SELECT COALESCE(MAX("+id+"),0)+1 FROM "+table)) {max.next();next=max.getLong(1);}
          try(var q=target.createStatement()){q.execute("ALTER TABLE "+table+" ALTER COLUMN "+id+" RESTART WITH "+next);}
        }
    } catch(Exception error) {target.rollback();throw error;}
    // Compare again after commit before allowing V19. Partial/idempotent reruns must have exact parity.
    if(!before.equals(snapshot(source)) || !before.equals(snapshot(target)))throw new SQLException("Post-copy parity mismatch");
    try(var s=source.createStatement()) {
      s.execute("CREATE TABLE IF NOT EXISTS identity_cutover (id INT PRIMARY KEY,state VARCHAR(40) NOT NULL,manifest VARCHAR(4096) NOT NULL)");
      s.executeUpdate("DELETE FROM identity_cutover WHERE id=1");
    }
    try(var s=source.prepareStatement("INSERT INTO identity_cutover(id,state,manifest) VALUES(1,'PARITY_VERIFIED',?)")) {
      s.setString(1,before.toString());s.executeUpdate();
    }
    System.out.println("Identity copy parity verified for five tables; accounts, profiles, roles, images and reset state preserved.");
    return before;
  }
  public static Map<String,String> snapshot(Connection connection) throws Exception {
    var result=new LinkedHashMap<String,String>();
    for(String table:TABLES)try(var s=connection.createStatement();var rows=s.executeQuery("SELECT * FROM "+table+" ORDER BY 1")) {
      var digest=MessageDigest.getInstance("SHA-256");long count=0;
      var names=new ArrayList<String>();for(int i=1;i<=rows.getMetaData().getColumnCount();i++)names.add(rows.getMetaData().getColumnName(i));
      Collections.sort(names);
      while(rows.next()) {count++;for(String name:names) {
        Object value=rows.getObject(name);byte[] bytes=value==null?new byte[0]:String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        digest.update((byte)(value==null?0:1));digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());digest.update(bytes);
      }}
      result.put(table,count+":"+HexFormat.of().formatHex(digest.digest()));
    }
    return result;
  }
}
