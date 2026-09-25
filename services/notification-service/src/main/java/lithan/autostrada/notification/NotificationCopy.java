package lithan.autostrada.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;

/** Offline maintenance utility; never registered as an HTTP endpoint. */
public final class NotificationCopy {
  public static void main(String[] args) {
    if(!"I_HAVE_STOPPED_ALL_WRITERS".equals(System.getenv("CUTOVER_WRITE_FREEZE")) || !"UTC".equals(System.getenv("CUTOVER_SOURCE_TIMEZONE")))
      throw new IllegalStateException("A verified UTC source and stopped writers are required");
    try(var source=DriverManager.getConnection(env("CUTOVER_SOURCE_URL"),env("CUTOVER_SOURCE_USER"),env("CUTOVER_SOURCE_PASSWORD"));
        var target=DriverManager.getConnection(env("CUTOVER_TARGET_URL"),env("CUTOVER_TARGET_USER"),env("CUTOVER_TARGET_PASSWORD"))) {
      if(source.getMetaData().getURL().equals(target.getMetaData().getURL()))throw new IllegalStateException("Distinct owner schemas required");
      copy(source,target);
    }catch(Exception ignored){System.err.println("Notification copy failed; preserve stopped writers and private diagnostics.");System.exit(1);}
  }
  private static String env(String key){String v=System.getenv(key);if(v==null||v.isBlank())throw new IllegalArgumentException("Missing "+key);return v;}
  public static void copy(Connection source,Connection target) throws Exception {
    try(var s=source.createStatement()) {
      s.execute("CREATE TABLE IF NOT EXISTS notification_cutover(id INTEGER PRIMARY KEY,state VARCHAR(40) NOT NULL)");
      s.executeUpdate("DELETE FROM notification_cutover WHERE id=1");s.executeUpdate("INSERT INTO notification_cutover VALUES(1,'COPYING')");
    }
    List<List<Object>> expected=snapshot(source);
    target.setAutoCommit(false);
    try {
      long count;try(var s=target.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM tb_notification")){if(!r.next())throw new SQLException("Missing target count");count=r.getLong(1);}
      if(count==0)try(var s=target.prepareStatement("INSERT INTO tb_notification(id_notification,id_user,id_car,notification_type,message,created_at,read_at,auction_snapshot,dedupe_key) VALUES (?,?,?,?,?,?,?,?,?)")) {
        for(var row:expected){for(int i=0;i<row.size();i++)s.setObject(i+1,row.get(i));s.executeUpdate();}
      }
      verify(target,expected);
      if(!expected.equals(snapshot(source)))throw new SQLException("Source changed during notification copy");
      target.commit();verify(target,expected);
    }catch(Exception error){target.rollback();throw error;}
    if(target.getMetaData().getDatabaseProductName().equals("H2"))try(var s=target.createStatement()) {
      int next=expected.isEmpty()?1:((Number)expected.get(expected.size()-1).get(0)).intValue()+1;
      s.execute("ALTER TABLE tb_notification ALTER COLUMN id_notification RESTART WITH "+next);
    }
    if(!expected.equals(snapshot(source)))throw new SQLException("Source changed after notification copy");
    // Seed business dedupe in the publisher too: the first scan must not recreate imported inbox entries.
    for(var row:expected)try(var s=source.prepareStatement("INSERT INTO tb_notification_outbox(event_id,dedupe_key,payload,created_at,published_at,next_attempt_at) SELECT ?,?,'{}',?,?,? WHERE NOT EXISTS (SELECT 1 FROM tb_notification_outbox WHERE dedupe_key=?)")) {
      s.setString(1,UUID.randomUUID().toString());s.setObject(2,row.get(8));s.setObject(3,row.get(5));s.setObject(4,row.get(5));s.setObject(5,row.get(5));s.setObject(6,row.get(8));s.executeUpdate();
    }
    try(var s=source.createStatement()) {
      s.executeUpdate("UPDATE notification_cutover SET state='PARITY_VERIFIED' WHERE id=1");
    }
    System.out.println("Notification copy parity verified: IDs, recipient/auction references, messages, timestamps, read state, snapshots and dedupe keys.");
  }
  private static List<List<Object>> snapshot(Connection source) throws Exception {
    var json=new ObjectMapper();List<List<Object>> expected=new ArrayList<>();
    try(var s=source.createStatement();var rows=s.executeQuery("SELECT n.*,c.make,c.model,c.production_year,c.price,c.status,c.auction_end_time FROM tb_auction_notification n JOIN tb_car c ON c.id_car=n.id_car ORDER BY n.id_notification")) {
      while(rows.next()) {
        var a=new LinkedHashMap<String,Object>();int car=rows.getInt("id_car"),user=rows.getInt("id_user");
        a.put("id",car);a.put("make",rows.getString("make"));a.put("model",rows.getString("model"));a.put("year",rows.getString("production_year"));
        a.put("price",rows.getInt("price"));a.put("status",rows.getString("status"));a.put("statusLabel",rows.getString("status"));
        var end=rows.getTimestamp("auction_end_time");a.put("auctionEndTime",end==null?null:end.toLocalDateTime().toString());
        a.put("auctionEndTimeEpochMillis",end==null?0:end.toInstant().toEpochMilli());a.put("sellerDisplayName",null);
        a.put("imageUrl","/api/public/auctions/"+car+"/notification-image");a.put("imageUrls",List.of(a.get("imageUrl")));
        expected.add(Arrays.asList(rows.getInt("id_notification"),user,car,rows.getString("notification_type"),rows.getString("message"),
            rows.getTimestamp("created_at"),rows.getTimestamp("read_at"),json.writeValueAsString(a),user+":"+car+":ENDING_SOON"));
      }
    }
    try(var s=source.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM tb_auction_notification")){if(!r.next())throw new SQLException("Missing source count");if(r.getLong(1)!=expected.size())throw new SQLException("Orphan notification detected");}
    return expected;
  }
  private static void verify(Connection db,List<List<Object>> expected) throws Exception {
    List<List<Object>> actual=new ArrayList<>();
    try(var s=db.createStatement();var r=s.executeQuery("SELECT id_notification,id_user,id_car,notification_type,message,created_at,read_at,auction_snapshot,dedupe_key FROM tb_notification ORDER BY id_notification")) {
      while(r.next())actual.add(Arrays.asList(r.getInt(1),r.getInt(2),r.getInt(3),r.getString(4),r.getString(5),r.getTimestamp(6),r.getTimestamp(7),r.getString(8),r.getString(9)));
    }
    if(!expected.equals(actual))throw new SQLException("Notification copy parity mismatch");
  }
}
