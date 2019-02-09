package meyn.cevn.controle;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.appengine.api.ThreadManager;

import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.Usuario;
import meyn.util.modelo.ErroModelo;

@WebListener
public class RemovedorContexto implements HttpSessionListener {
	
	private Logger logger = LogManager.getLogger();

	@Override
	public void sessionCreated(HttpSessionEvent evt) {
		HttpSession sessao = evt.getSession();
		String id = sessao.getId();
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(sessao.getMaxInactiveInterval()*1000);
				} catch (InterruptedException e) {
				}
				Usuario usu = ContextoEvn.getContexto(id).getUsuario();
				if (usu != null) {
					try {
						FabricaFachada.getFachada().desativarServicoLog(usu);
					} catch (ErroModelo e) {
					}
					ContextoEvn.remover(usu);
					logger.info("sessão encerrada: {}", id);
				}
			}
		}.start();
	}
	public void sessionDestroyed(HttpSessionEvent evt) {
	}
}
