package lithan.autostrada.notification;

import static org.assertj.core.api.Assertions.*;
import java.sql.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CopyTests {
  @Test void preservesIdsReadStateAndDedupeAndRejectsDivergedRerun() throws Exception {
    String sourceUrl="jdbc:h2:mem:source_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    String targetUrl="jdbc:h2:mem:target_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    org.flywaydb.core.Flyway.configure().dataSource(targetUrl,"sa","").load().migrate();
    try(var source=DriverManager.getConnection(sourceUrl,"sa","");var target=DriverManager.getConnection(targetUrl,"sa","");var s=source.createStatement()) {
      s.execute("CREATE TABLE tb_car(id_car INT PRIMARY KEY,make VARCHAR(100),model VARCHAR(100),production_year VARCHAR(4),price INT,status VARCHAR(20),auction_end_time TIMESTAMP(6))");
      s.execute("CREATE TABLE tb_auction_notification(id_notification INT PRIMARY KEY,id_user INT,id_car INT,notification_type VARCHAR(50),message VARCHAR(500),created_at TIMESTAMP(6),read_at TIMESTAMP(6))");
      s.execute("CREATE TABLE tb_notification_outbox(event_id VARCHAR(36) PRIMARY KEY,dedupe_key VARCHAR(160) UNIQUE,payload TEXT,created_at TIMESTAMP(6),published_at TIMESTAMP(6),next_attempt_at TIMESTAMP(6))");
      s.execute("INSERT INTO tb_car VALUES(11,'Škoda','Test','2024',100,'ACTIVE','2030-01-02 12:00:00')");
      s.execute("INSERT INTO tb_auction_notification VALUES(37,7,11,'AUCTION_ENDING_SOON','Škoda is ending soon.','2030-01-01 12:00:00.123456','2030-01-01 12:01:00.123456')");
      NotificationCopy.copy(source,target);NotificationCopy.copy(source,target);
      try(var q=target.createStatement();var r=q.executeQuery("SELECT id_notification,id_user,id_car,message,read_at FROM tb_notification")) {
        assertThat(r.next()).isTrue();assertThat(r.getInt(1)).isEqualTo(37);assertThat(r.getInt(2)).isEqualTo(7);assertThat(r.getInt(3)).isEqualTo(11);
        assertThat(r.getString(4)).isEqualTo("Škoda is ending soon.");assertThat(r.getTimestamp(5)).isEqualTo(Timestamp.valueOf("2030-01-01 12:01:00.123456"));assertThat(r.next()).isFalse();
      }
      try(var r=s.executeQuery("SELECT COUNT(*) FROM tb_notification_outbox")){r.next();assertThat(r.getInt(1)).isEqualTo(1);}
      s.executeUpdate("UPDATE tb_auction_notification SET read_at=NULL");
      assertThatThrownBy(()->NotificationCopy.copy(source,target)).isInstanceOf(SQLException.class);
      try(var r=s.executeQuery("SELECT state FROM notification_cutover")){r.next();assertThat(r.getString(1)).isEqualTo("COPYING");}
    }
  }
}
