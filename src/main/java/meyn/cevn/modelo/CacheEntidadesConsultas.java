package meyn.cevn.modelo;

import java.util.Collection;

import org.apache.logging.log4j.Logger;

import com.evernote.thrift.TBase;

import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
abstract class CacheEntidadesConsultas extends CacheEvn<String, CacheEntidadesEvn<?>> {

	@FunctionalInterface
	protected static interface IniciadorEntidade<TipoMtd extends TBase<?>, TipoEnt extends EntidadeEvn<?>> {
		void executar(Usuario usu, TipoMtd mtd, TipoEnt ent) throws ErroModelo;
	}

	@FunctionalInterface
	protected static interface ValidadorEntidade<TipoEnt extends EntidadeEvn<?>> {
		void executar(Usuario usu, TipoEnt ent) throws ErroModelo;
	}

	protected static abstract class InfoConsulta<TipoMtd extends TBase<?>, TipoEnt extends EntidadeEvn<?>> {

		private Class<?> tipoEntidade;
		private boolean entidadeValidavel;
		private IniciadorEntidade<TipoMtd, TipoEnt> iniciadorPropsEnt;
		private IniciadorEntidade<TipoMtd, TipoEnt> iniciadorPropsRelEnt;
		private ValidadorEntidade<TipoEnt> validadorPropsEnt;
		private Logger logger;

		Class<?> getTipoEntidade() {
			return tipoEntidade;
		}

		void setTipoEntidade(Class<?> tipoEntidade) {
			this.tipoEntidade = tipoEntidade;
		}

		boolean isEntidadeValidavel() {
			return entidadeValidavel;
		}

		void setEntidadeValidavel(boolean entidadeValidavel) {
			this.entidadeValidavel = entidadeValidavel;
		}

		IniciadorEntidade<TipoMtd, TipoEnt> getIniciadorPropsEnt() {
			return iniciadorPropsEnt;
		}

		void setIniciadorPropsEnt(IniciadorEntidade<TipoMtd, TipoEnt> iniciadorPropsEnt) {
			this.iniciadorPropsEnt = iniciadorPropsEnt;
		}

		IniciadorEntidade<TipoMtd, TipoEnt> getIniciadorPropsRelEnt() {
			return iniciadorPropsRelEnt;
		}

		void setIniciadorPropsRelEnt(IniciadorEntidade<TipoMtd, TipoEnt> iniciadorPropsRelEnt) {
			this.iniciadorPropsRelEnt = iniciadorPropsRelEnt;
		}

		ValidadorEntidade<TipoEnt> getValidadorPropsEnt() {
			return validadorPropsEnt;
		}

		void setValidadorPropsEnt(ValidadorEntidade<TipoEnt> validadorPropsEnt) {
			this.validadorPropsEnt = validadorPropsEnt;
		}

		Logger getLogger() {
			return logger;
		}

		void setLogger(Logger logger) {
			this.logger = logger;
		}
	}

	protected static <TipoChave, TipoValor> CacheEvn<TipoChave, TipoValor> getCache(Usuario usu,
			Class<? extends CacheEvn<TipoChave, TipoValor>> tipo) throws ErroModelo {
		CacheEvn<TipoChave, TipoValor> cache = CacheEvn.getCache(usu, tipo);
		if (!cache.isAtualizado()) {
			cache.clear();
			cache.setAtualizado(true);
		}
		return cache;
	}

	protected abstract CacheEntidadesEvn<?> get(Usuario usu, InfoConsulta<?, ?> infoCache) throws ErroModelo;

	protected void invalidarCaches(Collection<String> clChavesCaches) throws ErroModelo {
		for (String chave : clChavesCaches) {
			if (containsKey(chave)) {
				get(chave).invalidar();
			}
		}
	}

	protected void validarEntidades() throws ErroModelo {
		for (CacheEntidadesEvn<?> cache : values()) {
			cache.setValidarEntidades(true);
		}
	}
}
