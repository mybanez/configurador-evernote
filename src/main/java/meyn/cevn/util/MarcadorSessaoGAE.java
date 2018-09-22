package meyn.cevn.util;

import java.util.Map;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * Necess�ria para garantir persist�ncia de atributos 
 * alterados da sess�o por parte do Google App Engine
 */
@SuppressWarnings("serial")
public class MarcadorSessaoGAE implements PhaseListener {
	Logger logger = LogManager.getLogger(getClass());
	
	@Override
	public void afterPhase(PhaseEvent event) {
		Map<String, Object> mpAtribs = event.getFacesContext().getExternalContext().getSessionMap();
		mpAtribs.put("AGORA", System.currentTimeMillis());
		logger.trace("sessao marcada");
	}

	@Override
	public void beforePhase(PhaseEvent event) {
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;

	}
}