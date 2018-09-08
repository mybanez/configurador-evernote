package meyn.cevn.controle;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.evernote.auth.EvernoteService;

import meyn.cevn.ClienteEvn;
import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
public abstract class ManagedBeanEvn implements Serializable {

	private static final String AUTH_TOKEN_SAND_BOX = "S=s1:U=93be0:E=1683a17eb9d:C=160e266bcc0:P=1cd:A=en-devtoken:V=2:H=a12125d6ec082e623c91bcc59f25de88";
	private static final String AUTH_TOKEN_PROD = "";

	private final Logger logger = LogManager.getLogger(getClass());

	protected Logger getLogger() {
		return logger;
	}

	protected String getParametroRequisicao(String nome) {
		Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
		return params.get(nome);
	}
	
	protected Usuario getUsuario() throws ErroModelo {
		ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession sessao = (HttpSession) contexto.getSession(true);
		//GAE: busca no memcache e, caso não encontre, no Datastore
		Usuario usu = (Usuario) sessao.getAttribute(ChavesControle.USUARIO);
		if (usu == null) {
			usu = FabricaEntidade.getInstancia(Usuario.class);
			usu.setId(sessao.getId());
			//usu.setEvernoteService(EvernoteService.SANDBOX);
			//usu.setToken(AUTH_TOKEN_SAND_BOX);
			usu.setEvernoteService(EvernoteService.PRODUCTION);
			usu.setToken(AUTH_TOKEN_PROD);
			sessao.setAttribute(ChavesControle.USUARIO, usu);
		}
		ThreadContext.put("usuario", usu.getId());
		//Força iniciação do contexto
		ContextoEvn.getContexto(usu);
		getLogger().debug("usuario recuperado");
		if (!ClienteEvn.isIniciado(usu)) {
			ClienteEvn.setURL(contexto.getRequestScheme() + "://" + contexto.getRequestServerName() + ":"
					+ contexto.getRequestServerPort() + contexto.getRequestContextPath());
			ClienteEvn.conectar(usu);
		}
		getLogger().debug("cliente conectado");
		return usu;
	}
}
