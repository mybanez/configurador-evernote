package meyn.cevn.modelo;

import com.evernote.thrift.TBase;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public abstract class CacheResultadosConsulta extends CacheEvn<String, CacheOTEvn<?>> {

	@FunctionalInterface
	public static interface IniciadorPropriedadesOT<TipoMtd extends TBase<?>, TipoOT extends OTEvn<?>> {
		void executar(Usuario usu, TipoMtd mtd, TipoOT moldeOT) throws ErroModelo;
	}

	@FunctionalInterface
	public static interface ValidadorPropriedadesOT<TipoOT extends OTEvn<?>> {
		void executar(Usuario usu, TipoOT ot) throws ErroModelo;
	}
	
	public static abstract class Info<TipoMtd extends TBase<?>, TipoOT extends OTEvn<?>> {

		private Class<?> moldeOT;
		private IniciadorPropriedadesOT<TipoMtd, TipoOT> iniciadorPropsOT;
		private IniciadorPropriedadesOT<TipoMtd, TipoOT> iniciadorPropsRelOT;
		private ValidadorPropriedadesOT<TipoOT> validadorPropsOT;

		public Class<?> getMoldeOT() {
			return moldeOT;
		}

		public void setMoldeOT(Class<?> moldeOT) {
			this.moldeOT = moldeOT;
		}

		public IniciadorPropriedadesOT<TipoMtd, TipoOT> getIniciadorPropsOT() {
			return iniciadorPropsOT;
		}

		public void setIniciadorPropsOT(IniciadorPropriedadesOT<TipoMtd, TipoOT> iniciadorPropsOT) {
			this.iniciadorPropsOT = iniciadorPropsOT;
		}

		public IniciadorPropriedadesOT<TipoMtd, TipoOT> getIniciadorPropsRelOT() {
			return iniciadorPropsRelOT;
		}

		public void setIniciadorPropsRelOT(IniciadorPropriedadesOT<TipoMtd, TipoOT> iniciadorPropsRelOT) {
			this.iniciadorPropsRelOT = iniciadorPropsRelOT;
		}
		
		public ValidadorPropriedadesOT<TipoOT> getValidadorPropsOT() {
			return validadorPropsOT;
		}

		public void setValidadorPropsOT(ValidadorPropriedadesOT<TipoOT> validadorPropsOT) {
			this.validadorPropsOT = validadorPropsOT;
		}

		public abstract String getChave();
	}

	public void ativarValidacaoOT() throws ErroModelo {
		setEmValidacao(true);
		setAtualizado(false);
	}

	public void desativarValidacaoOT() {
		setEmValidacao(false);
	}

	public static <TipoChave, TipoValor> CacheEvn<TipoChave, TipoValor> getCache(Usuario usu,
			Class<? extends CacheEvn<TipoChave, TipoValor>> tipo) throws ErroModelo {
		CacheEvn<TipoChave, TipoValor> cache = CacheEvn.getCache(usu, tipo);
		if (!cache.isAtualizado()) {
			cache.clear();
			cache.setAtualizado(true);
		}
		return cache;
	}

	private boolean emValidacao = false;

	public boolean isEmValidacao() {
		return emValidacao;
	}

	public void setEmValidacao(boolean emValidacao) {
		this.emValidacao = emValidacao;
	}

	public abstract CacheOTEvn<?> get(Usuario usu, Info<?, ?> infoCache) throws ErroModelo;
}
