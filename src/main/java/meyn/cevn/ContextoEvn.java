package meyn.cevn;

import java.util.Locale;

import meyn.cevn.modelo.Usuario;
import meyn.util.contexto.ContextoEmMemoria;
import meyn.util.contexto.ErroContextoJaDefinido;

@SuppressWarnings("serial")
public class ContextoEvn extends ContextoEmMemoria {

	static {
		Locale.setDefault(new Locale("pt", "BR"));
	}

	public static ContextoEvn getContexto(String id) {
		return (ContextoEvn) buscar(id);
	}

	public static ContextoEvn getContexto(Usuario usu) {
		String chave = usu.getId();
		if (!isDefinido(chave)) {
			try {
				definir(chave, new ContextoEvn(usu));
			} catch (ErroContextoJaDefinido e) {
				/*
				 * Erro pode acontecer por concorrência. Estratégia é não sincronizar para
				 * ganhar performance, assumindo que este contexto seja definido uma única vez
				 * para o usuário.
				 */
			}
		}
		return (ContextoEvn) buscar(chave);
	}

	public static void remover(Usuario usu) {
		remover(usu.getId());
	}

	private static final String USUARIO = "USUARIO";
	private static final String URL_GERACAO = "URL_GERACAO";

	private ContextoEvn(Usuario usuario) {
		put(USUARIO, usuario);
	}

	public Usuario getUsuario() {
		return (Usuario) get(USUARIO);
	}

	public String getURLGeracao() {
		return (String) get(URL_GERACAO);
	}

	public void setURLGeracao(String urlGeracao) {
		put(URL_GERACAO, urlGeracao);
	}
}
