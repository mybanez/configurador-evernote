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
public abstract class CacheOTEvn<TipoOT extends OTEvn<?>> extends CacheEvn<String, TipoOT> {
	
	private class CachePorNome extends Cache<String, TipoOT> {
		void put(TipoOT ot) {
			put(ot.getNome(), ot);
		}
	}

	private CachePorNome cacheNome = new CachePorNome();

	@Override
	public Logger getLogger() {
		return super.getLogger();
	}	
	
	@Override
	public TipoOT put(String id, TipoOT ot) {
		cacheNome.put(ot);
		return super.put(id, ot);
	}

	@Override
	public void clear() {
		super.clear();
		cacheNome.clear();
	}

	public Collection<TipoOT> consultarTodos() throws ErroModelo {
		List<TipoOT> lsOTs = new ArrayList<TipoOT>(values());
		Collections.sort(lsOTs, (a,b) -> a.getNome().compareTo(b.getNome()));
		return lsOTs;
	}

	public abstract Grupo<TipoOT> consultarPorGrupo(String nomeGrupo) throws ErroModelo;

	public abstract Collection<TipoOT> consultarPorRepositorio(String nomeRepositorio) throws ErroModelo;

	public Collection<TipoOT> consultarPorFiltro(Predicate<TipoOT> filtro) throws ErroModelo {
		List<TipoOT> lsOTs = new ArrayList<TipoOT>(values());
		lsOTs.removeIf(filtro.negate());
		Collections.sort(lsOTs, (a,b) -> a.getNome().compareTo(b.getNome()));
		return lsOTs;
	}

	public TipoOT consultarPorChavePrimaria(String id) throws ErroModelo {
		TipoOT ot = get(id);
		if (ot == null) {
			throw new ErroItemNaoEncontrado(id);
		}
		return ot;
	}

	public TipoOT consultarPorNome(String nome) throws ErroModelo {
		if (size() > cacheNome.size()) {
			throw new ErroModelo("Nome não é chave primária no cache: " + getClass());
		}
		TipoOT ot = cacheNome.get(nome);
		if (ot == null) {
			throw new ErroItemNaoEncontrado(nome);
		}
		return ot;
	}
}
