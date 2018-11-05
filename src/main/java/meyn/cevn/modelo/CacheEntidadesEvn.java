package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

@SuppressWarnings("serial")
abstract class CacheEntidadesEvn<TipoEnt extends EntidadeEvn<?>> extends CacheEvn<String, TipoEnt> {
	
	private class CachePorNome extends Cache<String, TipoEnt> {
		CachePorNome() {
			setLogger(CacheEntidadesEvn.this.getLogger());
		}
		void put(TipoEnt ent) {
			put(ent.getNome(), ent);
		}
	}

	private String chave;
	private boolean entidadeValidavel = true;
	private boolean validarEntidades = false;
	private CachePorNome cacheNome = new CachePorNome();

	@Override
	public TipoEnt put(String id, TipoEnt ent) {
		if (cacheNome.containsKey(ent.getNome())) {
			getLogger().warn("Entidade duplicada: {}", ent.getNome());
		}		
		cacheNome.put(ent);
		return super.put(id, ent);
	}

	@Override
	public void clear() {
		super.clear();
		cacheNome.clear();
	}

	@Override
	protected Logger getLogger() {
		return super.getLogger();
	}	
	
	@Override
	protected void setLogger(Logger logger) {
		super.setLogger(logger);
	}
	
	protected String getChave() {
		return chave;
	}

	protected void setChave(String chave) {
		this.chave = chave;
	}

	protected boolean isEntidadeValidavel() {
		return entidadeValidavel;
	}

	protected void setEntidadeValidavel(boolean validavel) {
		this.entidadeValidavel = validavel;
	}

	protected boolean isValidarEntidades() {
		return entidadeValidavel && validarEntidades;
	}

	protected void setValidarEntidades(boolean validarEntidades) {
		this.validarEntidades = validarEntidades;
	}
	
	protected Collection<TipoEnt> consultarTodos() throws ErroModelo {
		List<TipoEnt> lsEnts = new ArrayList<TipoEnt>(values());
		Collections.sort(lsEnts, (a,b) -> a.getNome().compareTo(b.getNome()));
		return lsEnts;
	}

	protected abstract Grupo<TipoEnt> consultarPorGrupo(String nomeGrupo) throws ErroModelo;

	protected abstract Collection<TipoEnt> consultarPorRepositorio(String nomeRepositorio) throws ErroModelo;

	protected Collection<TipoEnt> consultarPorFiltro(Predicate<TipoEnt> filtro) throws ErroModelo {
		List<TipoEnt> lsEnts = new ArrayList<TipoEnt>(values());
		lsEnts.removeIf(filtro.negate());
		Collections.sort(lsEnts, (a,b) -> a.getNome().compareTo(b.getNome()));
		return lsEnts;
	}

	protected TipoEnt consultarPorChavePrimaria(String id) throws ErroModelo {
		TipoEnt ent = get(id);
		if (ent == null) {
			throw new ErroItemNaoEncontrado(id);
		}
		return ent;
	}

	protected TipoEnt consultarPorNome(String nome) throws ErroModelo {
		TipoEnt ent = cacheNome.get(nome);
		if (ent == null) {
			throw new ErroItemNaoEncontrado(nome);
		}
		return ent;
	}
}
