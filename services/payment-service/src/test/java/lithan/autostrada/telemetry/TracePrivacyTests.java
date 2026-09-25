package lithan.autostrada.telemetry;

import lithan.autostrada.payment.SafeSpanExporter;
import java.util.List;
import org.junit.jupiter.api.Test;
import io.opentelemetry.api.common.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.*;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TracePrivacyTests {
  @Test void credentialsUrlsExceptionsBaggageAndCustomLabelsNeverReachTransport() {
    var raw = mock(SpanData.class);
    var context = SpanContext.create("12345678901234567890123456789012", "1234567890123456", TraceFlags.getSampled(), TraceState.builder().put("private", "secret-sentinel").build());
    when(raw.getSpanContext()).thenReturn(context);
    when(raw.getParentSpanContext()).thenReturn(context);
    when(raw.getKind()).thenReturn(SpanKind.SERVER);
    when(raw.getName()).thenReturn("/reset?token=secret-sentinel");
    when(raw.getAttributes()).thenReturn(Attributes.builder().put("http.method", "GET").put("http.status_code", 500L)
        .put("http.url", "http://identity/reset?token=secret-sentinel").put("authorization", "secret-sentinel")
        .put("user.id", "secret-sentinel").build());
    when(raw.getResource()).thenReturn(Resource.create(Attributes.of(AttributeKey.stringKey("password"), "secret-sentinel")));
    when(raw.getEvents()).thenReturn(List.of(EventData.create(1, "secret-sentinel", Attributes.empty())));
    when(raw.getStatus()).thenReturn(StatusData.create(StatusCode.ERROR, "secret-sentinel"));
    var sink = mock(SpanExporter.class);
    when(sink.export(anyCollection())).thenAnswer(invocation -> {
      java.util.Collection<SpanData> spans = invocation.getArgument(0);
      var safe = spans.iterator().next();
      assertThat(safe.getName()).isEqualTo("SERVER");
      assertThat(safe.getAttributes().size()).isEqualTo(2);
      assertThat(safe.getAttributes().get(AttributeKey.longKey("http.status_code"))).isEqualTo(500);
      assertThat(safe.getEvents()).isEmpty();
      assertThat(safe.getLinks()).isEmpty();
      assertThat(safe.getStatus().getStatusCode()).isEqualTo(StatusCode.ERROR);
      assertThat(safe.getStatus().getDescription()).isEmpty();
      assertThat(safe.getSpanContext().getTraceId()).isEqualTo(context.getTraceId());
      assertThat(safe.getParentSpanContext().getSpanId()).isEqualTo(context.getSpanId());
      assertThat(safe.toString()).doesNotContain("secret-sentinel");
      return CompletableResultCode.ofSuccess();
    });
    assertThat(new SafeSpanExporter(sink, "fixture").export(List.of(raw)).isSuccess()).isTrue();
    verify(sink).export(anyCollection());
  }
}
