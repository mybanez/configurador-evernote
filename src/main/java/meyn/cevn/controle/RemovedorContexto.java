package meyn.cevn.controle;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import meyn.cevn.modelo.ContextoEvn;
import meyn.cevn.modelo.log.CadastroLog;
import meyn.cevn.modelo.usuario.Usuario;

public class RemovedorContexto implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent evt) {}

	@Override
	public void sessionDestroyed(HttpSessionEvent evt) {
		Usuario usu = (Usuario) evt.getSession().getAttribute(ChavesControle.USUARIO);
		CadastroLog.desativarServico(usu);
		ContextoEvn.removerContexto(usu);
	}
}
