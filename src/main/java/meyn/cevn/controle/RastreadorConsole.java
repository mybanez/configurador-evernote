package meyn.cevn.controle;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpServletRequest;

import io.opencensus.common.Scope;

@SuppressWarnings("serial")
public class RastreadorConsole extends RastreadorEvn implements PhaseListener {

	private Scope scp = null;

	@Override
	public void beforePhase(PhaseEvent event) {
		if (event.getPhaseId() == PhaseId.RESTORE_VIEW) {
			scp = iniciar((HttpServletRequest)event.getFacesContext().getExternalContext().getRequest());
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
