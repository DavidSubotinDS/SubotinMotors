package lithan.autostrada.payment;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Aggregate operational signals only; labels never contain actor or provider identifiers. */
@Component
class PaymentOperationalMetrics {
  PaymentOperationalMetrics(MeterRegistry meters,JdbcTemplate db,Clock clock){
    meters.gauge("autostrada.payment.reconciliation.pending",db,value(db,"SELECT COUNT(*) FROM payment_attempt WHERE status IN ('CREATING','RECONCILE_REQUIRED','CHECKOUT_CREATED','EXPIRY_REQUESTED')"));
    meters.gauge("autostrada.payment.webhook.unmatched",db,value(db,"SELECT COUNT(*) FROM payment_webhook_receipt WHERE status='UNMATCHED'"));
    meters.gauge("autostrada.payment.outbox.pending",db,value(db,"SELECT COUNT(*) FROM payment_outbox WHERE status='PENDING'"));
    meters.gauge("autostrada.payment.attempt.oldest.seconds",db,age(db,clock,"SELECT MIN(created_at) FROM payment_attempt WHERE status NOT IN ('SUCCEEDED','FAILED','EXPIRED')"));
    meters.gauge("autostrada.payment.outbox.oldest.seconds",db,age(db,clock,"SELECT MIN(created_at) FROM payment_outbox WHERE status='PENDING'"));
  }
  private static java.util.function.ToDoubleFunction<JdbcTemplate> value(JdbcTemplate ignored,String sql){return db->{try{Number n=db.queryForObject(sql,Number.class);return n==null?0:n.doubleValue();}catch(RuntimeException unavailable){return Double.NaN;}};}
  private static java.util.function.ToDoubleFunction<JdbcTemplate> age(JdbcTemplate ignored,Clock clock,String sql){return db->{try{java.sql.Timestamp value=db.queryForObject(sql,java.sql.Timestamp.class);return value==null?0:Math.max(0,clock.instant().getEpochSecond()-value.toInstant().getEpochSecond());}catch(RuntimeException unavailable){return Double.NaN;}};}
}
