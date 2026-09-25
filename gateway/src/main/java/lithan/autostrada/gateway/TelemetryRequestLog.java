package lithan.autostrada.gateway;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import io.micrometer.tracing.Tracer;
import org.slf4j.LoggerFactory;

/** Curated request telemetry: never writes paths, headers, payloads or exception messages. */
@Component
@Profile("observability")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class TelemetryRequestLog implements org.springframework.web.server.WebFilter {
  private final Tracer tracer;
  TelemetryRequestLog(Tracer tracer) { this.tracer = tracer; }
  @Override public reactor.core.publisher.Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange, org.springframework.web.server.WebFilterChain chain) {
    long started = System.nanoTime();
    return chain.filter(exchange).doFinally(signal -> {
      var responseStatus = exchange.getResponse().getStatusCode();
      write(exchange.getRequest().getMethod().name(), responseStatus == null ? 200 : responseStatus.value(), started);
    });
  }
  private void write(String method, int status, long started) {
    var span = tracer.currentSpan();
    String trace = span == null ? "none" : span.context().traceId();
    String spanId = span == null ? "none" : span.context().spanId();
    String safeMethod = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS").contains(method) ? method : "OTHER";
    LoggerFactory.getLogger("autostrada.telemetry").info("method={} status={} duration_ms={} trace_id={} span_id={}",
        safeMethod, status, (System.nanoTime() - started) / 1_000_000, trace, spanId);
  }
}
