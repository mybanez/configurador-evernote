package meyn.cevn.controle;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.Usuario;
import meyn.util.modelo.ErroModelo;

public class RemovedorContexto implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent evt) {
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent evt) {
		Usuario usu = (Usuario) evt.getSession().getAttribute(ChavesControle.USUARIO);
		if (usu != null) {
			try {
				FabricaFachada.getFachada().desativarServicoLog(usu);
			} catch (ErroModelo e) {
			}
			ContextoEvn.removerContexto(usu);
		}
	}
}
