package meyn.cevn.controle;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.ThreadContext;

import com.evernote.auth.EvernoteService;

import meyn.cevn.modelo.ClienteEvn;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.Fachada;
import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.ot.FabricaOT;

@SuppressWarnings("serial")
public abstract class ManagedBeanImpl implements Serializable {

	private static final String AUTH_TOKEN_SAND_BOX = "S=s1:U=93be0:E=1683a17eb9d:C=160e266bcc0:P=1cd:A=en-devtoken:V=2:H=a12125d6ec082e623c91bcc59f25de88";
	private static final String AUTH_TOKEN_PROD = "S=s202:U=187f3ba:E=168dde82bfd:C=1618636fc70:P=1cd:A=en-devtoken:V=2:H=cb717752d1b131b749b1d63ef31e8109";

	protected Usuario getUsuario() throws ErroModelo {
		ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession sessao = (HttpSession) contexto.getSession(true);
		Usuario usu = (Usuario) sessao.getAttribute(ChavesControle.USUARIO);
		Fachada fc = FabricaFachada.getFachada();
		if (usu == null) {
			ClienteEvn.setURL(contexto.getRequestScheme() + "://" + contexto.getRequestServerName() + ":"
					+ contexto.getRequestServerPort() + contexto.getRequestContextPath());
			usu = FabricaOT.getInstancia(Usuario.class);
			usu.setId(sessao.getId());
			//usu.setEvernoteService(EvernoteService.SANDBOX);
			//usu.setToken(AUTH_TOKEN_SAND_BOX);
			usu.setEvernoteService(EvernoteService.PRODUCTION);
			usu.setToken(AUTH_TOKEN_PROD);
			sessao.setAttribute(ChavesControle.USUARIO, fc.consultarUsuario(usu));
			ThreadContext.put("usuario", usu.getId());
			fc.atualizarContadorAtualizacao(usu);
			fc.excluirSumariosInvalidos(usu);
		} else {
			ThreadContext.put("usuario", usu.getId());
			fc.atualizarContadorAtualizacao(usu);
		}
		return usu;
	}

	protected String getParametroURL(String nome) {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		return params.get(nome);
	}
}
