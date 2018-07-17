package meyn.cevn.modelo;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.contexto.Contexto;

@SuppressWarnings("serial")
public class ContextoEvn extends Contexto {
	
	private Usuario usuario;
	
	private ContextoEvn(Usuario usuario) {
		super();
		this.usuario = usuario;
	}

	public Usuario getUsuario() {
		return usuario;
	}
	
	public static ContextoEvn getContexto(String id) {
		return (ContextoEvn) buscarContextoGlobal(id);
	}
	
	public static ContextoEvn getContexto(Usuario usu) {
		if (!contextoGlobalDefinido(usu.getId())) {
			definirContextoGlobal(usu.getId(), new ContextoEvn(usu));
		}
		return (ContextoEvn) buscarContextoGlobal(usu.getId());
	}

	public static void removerContexto(String id) {
		removerContextoGlobal(id);
	}

	public static void removerContexto(Usuario usu) {
		removerContextoGlobal(usu.getId());
	}
}
