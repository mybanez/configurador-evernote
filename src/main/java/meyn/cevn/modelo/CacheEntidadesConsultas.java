package meyn.cevn.modelo;

import java.util.Collection;

import org.apache.logging.log4j.Logger;

import com.evernote.thrift.TBase;

import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
abstract class CacheEntidadesConsultas extends CacheEvn<String, CacheEntidadesEvn<?>> {

	@FunctionalInterface
	protected static interface IniciadorPropriedadesEnt<TipoMtd extends TBase<?>, TipoEnt extends EntidadeEvn<?>> {
		void executar(Usuario usu, TipoMtd mtd, TipoEnt ent) throws ErroModelo;
	}

	@FunctionalInterface
	protected static interface ValidadorPropriedadesEnt<TipoEnt extends EntidadeEvn<?>> {
		void executar(Usuario usu, TipoEnt ent) throws ErroModelo;
	}

	protected static abstract class InfoConsulta<TipoMtd extends TBase<?>, TipoEnt extends EntidadeEvn<?>> {

		private Class<?> tipoEntidade;
		private boolean entidadeValidavel;
		private IniciadorPropriedadesEnt<TipoMtd, TipoEnt> iniciadorPropsEnt;
		private IniciadorPropriedadesEnt<TipoMtd, TipoEnt> iniciadorPropsRelEnt;
		private ValidadorPropriedadesEnt<TipoEnt> validadorPropsEnt;
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

		IniciadorPropriedadesEnt<TipoMtd, TipoEnt> getIniciadorPropsEnt() {
			return iniciadorPropsEnt;
		}

		void setIniciadorPropsEnt(IniciadorPropriedadesEnt<TipoMtd, TipoEnt> iniciadorPropsEnt) {
			this.iniciadorPropsEnt = iniciadorPropsEnt;
		}

		IniciadorPropriedadesEnt<TipoMtd, TipoEnt> getIniciadorPropsRelEnt() {
			return iniciadorPropsRelEnt;
		}

		void setIniciadorPropsRelEnt(IniciadorPropriedadesEnt<TipoMtd, TipoEnt> iniciadorPropsRelEnt) {
			this.iniciadorPropsRelEnt = iniciadorPropsRelEnt;
		}

		ValidadorPropriedadesEnt<TipoEnt> getValidadorPropsEnt() {
			return validadorPropsEnt;
		}

		void setValidadorPropsEnt(ValidadorPropriedadesEnt<TipoEnt> validadorPropsEnt) {
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
				get(chave).setAtualizado(false);
			}
		}
	}

	protected void validarEntidades() throws ErroModelo {
		for (CacheEntidadesEvn<?> cache : values()) {
			cache.setValidarEntidades(true);
		}
	}
}
