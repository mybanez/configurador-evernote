package meyn.cevn;

import java.util.Locale;

import meyn.cevn.modelo.Usuario;
import meyn.util.contexto.ContextoEmMemoria;
import meyn.util.contexto.ErroContextoJaDefinido;

@SuppressWarnings("serial")
public class ContextoEvn extends ContextoEmMemoria {
	
	private static final String CONTEXTO_LOCAL = "CONTEXTO_LOCAL: ";
	private static final String CONTEXTO_SESSAO = "CONTEXTO_SESSAO: ";
	
	static {
		Locale.setDefault(new Locale("pt", "BR"));
	}
	
	public static ContextoEvn getContextoLocal(String id) {
		return (ContextoEvn) buscar(CONTEXTO_LOCAL + id);
	}
	
	public static void removerContextoLocal(String id) {
		remover(CONTEXTO_LOCAL + id);
	}
	
	public static ContextoEvn getContextoLocal(Usuario usu) {
		String chave = CONTEXTO_LOCAL + usu.getId();
		if (!isDefinido(chave)) {
			try {
				definir(chave, new ContextoEvn(usu));
			} catch(ErroContextoJaDefinido e) {
				/* Erro pode acontecer por concorrência. Estratégia é não 
				 * sincronizar para ganhar performance, assumindo que este 
				 * contexto seja definido uma única vez para o usuário. 
				 */
			}			
		}
		return (ContextoEvn) buscar(chave);
	}

	public static void removerContextoLocal(Usuario usu) {
		removerContextoLocal(usu.getId());
	}
	
	public static ContextoEvn getContextoSessao(Usuario usu) {
		String chave = CONTEXTO_SESSAO + usu.getId();
		if (!isDefinido(chave)) {
			try {
				ContextoEvn contexto = usu.getContexto();
				if (contexto == null) {
					contexto = new ContextoEvn(usu);
				}
				definir(chave, contexto);
				usu.setContexto(contexto);
			} catch(ErroContextoJaDefinido e) {
				/* Erro pode acontecer por concorrência. Estratégia é não 
				 * sincronizar para ganhar performance, assumindo que este 
				 * contexto seja definido uma única vez para o usuário. 
				 */
			}			
		}
		return (ContextoEvn) buscar(chave);
	}
	
	public static ContextoEvn getContexto(String id) {
		return getContextoLocal(id);
	}

	public static ContextoEvn getContexto(Usuario usu) {
		return getContextoLocal(usu);
	}
	
	
	public static void removerContexto(String id) {
		removerContextoLocal(id);
	}

	public static void removerContexto(Usuario usu) {
		removerContextoLocal(usu);
	}
	
	private Usuario usuario;
	
	private ContextoEvn(Usuario usuario) {
		super();
		this.usuario = usuario;
	}

	public Usuario getUsuario() {
		return usuario;
	}
}
