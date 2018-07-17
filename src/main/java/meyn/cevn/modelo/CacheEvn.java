package meyn.cevn.modelo;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public class CacheEvn<TipoChave, TipoValor> extends Cache<TipoChave, TipoValor> {

	public static <TipoChave, TipoValor> CacheEvn<TipoChave, TipoValor> getCache(Usuario usu,
			Class<? extends CacheEvn<TipoChave, TipoValor>> tipo) throws ErroModelo {
		return (CacheEvn<TipoChave, TipoValor>) getCache(ContextoEvn.getContexto(usu), tipo);
	}

	private int contadorAtualizacao = 0;

	protected int getContadorAtualizacao() {
		return contadorAtualizacao;
	}

	protected void setContadorAtualizacao(int contadorAtualizacao) {
		this.contadorAtualizacao = contadorAtualizacao;
	}

	public Usuario getUsuario() {
		return ((ContextoEvn) getContexto()).getUsuario();
	}

	@Override
	public boolean isAtualizado() throws ErroModelo {
		if (getUsuario().getContadorAtualizacao() > getContadorAtualizacao()) {
			setAtualizado(false);
		}
		return super.isAtualizado();
	}

	@Override
	public void setAtualizado(boolean atualizado) throws ErroModelo {
		if (atualizado) {
			setContadorAtualizacao(getUsuario().getContadorAtualizacao());
		}
		super.setAtualizado(atualizado);
	}
}
