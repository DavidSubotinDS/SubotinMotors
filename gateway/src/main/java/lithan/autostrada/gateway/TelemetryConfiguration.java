package lithan.autostrada.gateway;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;

@Configuration(proxyBeanMethods = false)
@Profile("observability")
class TelemetryConfiguration {
  @Bean
  SpanExporter safeTraceExporter(@Value("${telemetry.trace-url:http://127.0.0.1:4318/v1/traces}") String endpoint) {
    return new SafeSpanExporter(OtlpHttpSpanExporter.builder().setEndpoint(endpoint)
        .setTimeout(Duration.ofSeconds(1)).build(), "gateway");
  }
}
