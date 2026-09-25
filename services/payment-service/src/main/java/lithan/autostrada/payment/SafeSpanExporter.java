package lithan.autostrada.payment;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.*;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/** Allowlist before transport: no URL/query, credentials, payloads or exception text. */
public final class SafeSpanExporter implements SpanExporter {
  private final SpanExporter delegate;
  private final Resource resource;
  public SafeSpanExporter(SpanExporter delegate, String service) {
    this.delegate = delegate;
    this.resource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), service));
  }
  @Override public CompletableResultCode export(Collection<SpanData> spans) {
    return delegate.export(spans.stream().map(this::sanitize).toList());
  }
  private SpanData sanitize(SpanData span) {
    var attributes = Attributes.builder();
    for (String key : List.of("http.method", "http.request.method")) {
      String value = span.getAttributes().get(AttributeKey.stringKey(key));
      if (value != null && Set.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "CONNECT").contains(value)) {
        attributes.put(key, value);
      }
    }
    for (String key : List.of("http.status_code", "http.response.status_code")) {
      Long value = span.getAttributes().get(AttributeKey.longKey(key));
      if (value != null && value >= 100 && value <= 599) attributes.put(key, value);
    }
    Attributes safe = attributes.build();
    return new DelegatingSpanData(span) {
      @Override public String getName() { return span.getKind().name(); }
      @Override public Attributes getAttributes() { return safe; }
      @Override public Resource getResource() { return resource; }
      @Override public InstrumentationScopeInfo getInstrumentationScopeInfo() { return InstrumentationScopeInfo.create("autostrada"); }
      @Override public SpanContext getSpanContext() { return cleanContext(span.getSpanContext()); }
      @Override public SpanContext getParentSpanContext() { return cleanContext(span.getParentSpanContext()); }
      @Override public List<EventData> getEvents() { return List.of(); }
      @Override public List<LinkData> getLinks() { return List.of(); }
      @Override public StatusData getStatus() { return StatusData.create(span.getStatus().getStatusCode(), ""); }
      @Override public int getTotalAttributeCount() { return safe.size(); }
      @Override public int getTotalRecordedEvents() { return 0; }
      @Override public int getTotalRecordedLinks() { return 0; }
    };
  }
  private static SpanContext cleanContext(SpanContext context) {
    return SpanContext.create(context.getTraceId(), context.getSpanId(), context.getTraceFlags(), TraceState.getDefault());
  }
  @Override public CompletableResultCode flush() { return delegate.flush(); }
  @Override public CompletableResultCode shutdown() { return delegate.shutdown(); }
}
