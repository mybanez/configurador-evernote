package meyn.cevn.controle;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.opencensus.common.Scope;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.SpanId;
import io.opencensus.trace.TraceId;
import io.opencensus.trace.TraceOptions;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracestate;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;

@SuppressWarnings("serial")
public class Rastreador implements PhaseListener {

	private static final String PROJETO = "GOOGLE_CLOUD_PROJECT";
	private static final Logger LOGGER = LogManager.getLogger(Rastreador.class);

	private static Tracer tracer;

	static {
		try {
			tracer = Tracing.getTracer();
			String idProjeto = System.getenv(PROJETO);
			StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().setProjectId(idProjeto).build());
			LOGGER.info("rastreando aplicativo com Stackdriver: {}", idProjeto);
		} catch (Throwable t) {
			tracer = null;
			LOGGER.error("Erro na criação e registro do exportador Stackdriver", t);
		}
	}

	private static class Contexto {
		/**
		 * The custom header name.
		 */
		static final String TRACE_HEADER = "X-Cloud-Trace-Context";

		static final long TRACE_OPTIONS_NONE = 0;
		static final long TRACE_ENABLED = 1;

		TraceId traceId;
		SpanId spanId;
		/**
		 * The trace span options, which is a bitmasked long representing the state of
		 * various tracing features (such as whether or not incoming/outgoing traces are
		 * enabled).
		 */
		TraceOptions options;

		/**
		 * Creates a new trace context from a trace HTTP header string. The raw trace
		 * header value takes a format like:
		 * traceid/spanid;o=options;key1=value1;key2=value2; Where traceid is a string,
		 * spanid is a number, options is a number, and options and the following
		 * key-value pairs are all optional.
		 * 
		 * @return the new Contexto, or null if we could not adequately parse it.
		 */
		public static Contexto getContexto() {
			String header = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get(TRACE_HEADER);
			if (header == null) {
				return null;
			}

			String traceId = null;
			BigInteger spanId = null;
			long options = 0;
			Map<String, String> data = new HashMap<>();

			String[] slashParts = header.split("/");
			if (slashParts.length > 0) {
				traceId = slashParts[0];
				if (slashParts.length > 1) {
					String[] semiParts = slashParts[1].split(";");
					if (semiParts.length > 0) {
						try {
							spanId = new BigInteger(semiParts[0]);
						} catch (NumberFormatException nfe) {
						}
					}
					if (semiParts.length > 1) {
						for (int i = 1; i < semiParts.length; i++) {
							String[] kvParts = semiParts[i].split("=");
							if (kvParts.length != 2) {
								continue;
							}

							if (i == 1 && "o".equals(kvParts[0])) {
								// If there is an o= (options) string, it is supposed to come first.
								try {
									options = Long.parseLong(kvParts[1]);
								} catch (NumberFormatException nfe) {
								}
							} else if (!"o".equals(kvParts[0])) {
								// Just stick it in a map.
								data.put(kvParts[0], kvParts[1]);
							}
						}
					}
				}
			}
			return new Contexto(traceId, spanId.toString(16), options, data);
		}

		/**
		 * Creates a new trace context with the given identifiers and options and the
		 * given data map.
		 */
		public Contexto(String traceId, String spanId, long options, Map<String, String> data) {
			this.traceId = TraceId.fromLowerBase16(traceId);
			this.spanId = SpanId.fromLowerBase16(StringUtils.repeat("0", 16 - spanId.length()) + spanId);
			this.options = TraceOptions.fromByte((byte) options);
		}

		public TraceId getTraceId() {
			return traceId;
		}

		public SpanId getSpanId() {
			return spanId;
		}

		public TraceOptions getOptions() {
			return options;
		}
	}

	public static Scope iniciarEscopo(String nome) {
		return tracer != null ? tracer.spanBuilder(nome).setSampler(Samplers.alwaysSample()).startScopedSpan() : null;
	}

	public static Scope iniciarEscopo(String nome, SpanContext contexto) {
		return tracer != null ? tracer.spanBuilderWithRemoteParent(nome, contexto).setSampler(Samplers.alwaysSample()).startScopedSpan()
		        : null;
	}

	private Scope scp = null;

	@Override
	public void beforePhase(PhaseEvent event) {
		if (event.getPhaseId() == PhaseId.RESTORE_VIEW) {
			try {
				Contexto ctxTrace = Contexto.getContexto();
				LOGGER.debug("contexto rastreamento: {}, {}, {}", ctxTrace.getTraceId(), ctxTrace.getSpanId(), ctxTrace.getOptions());
				SpanContext ctxSpan = SpanContext.create(ctxTrace.getTraceId(), ctxTrace.getSpanId(), ctxTrace.getOptions(),
				        Tracestate.builder().build());
				scp = iniciarEscopo("processarResposta", ctxSpan);
			} catch (Throwable t) {
				LOGGER.error("Erro rastreando execução", t);
			}
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		if (scp != null && event.getPhaseId() == PhaseId.RENDER_RESPONSE) {
			scp.close();
		}
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

}
