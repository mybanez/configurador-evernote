package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evernote.edam.type.Tag;

import meyn.util.Cache;
import meyn.util.ErroExecucao;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public abstract class CacheNotas<TipoNota extends Nota> extends CacheOTEvn<TipoNota> {
	
	private class CachePorGrupo extends Cache<String, Grupo<TipoNota>> {
		void put(TipoNota nota) {
			try {
				Grupo<TipoNota> grupo, subgrupo;
				List<String> lsIds = nota.getMetadado().getTagGuids();
				if (lsIds != null) {
					CacheTags cacheTag = CacheTags.getCache(getUsuario());
					for (String id : lsIds) {
						subgrupo = null;
						Tag mtd = cacheTag.get(id);
						do {
							String idGrupo = mtd.getGuid();
							String nomeGrupo = mtd.getName();
							if (grupoHomonimoDeItemPermitido || !nomeGrupo.equals(nota.getNome())) {
								grupo = getOrDefault(idGrupo, new Grupo<TipoNota>(nomeGrupo));
								if (grupo.isVazio()) {
									put(idGrupo, grupo);
									put(nomeGrupo, grupo);
								}
								if (subgrupo != null) {
									grupo.getGruposFilho().add(subgrupo);
								} else {
									//Só inclui se item não está em subgrupo
									if (Collections.disjoint(cacheTag.consultarPorRepositorio(grupo.getNome()), lsIds)) {
										grupo.getOtsFilho().add(nota);
									}	
								}
								subgrupo = grupo;
							}
							mtd = cacheTag.get(mtd.getParentGuid());
						} while (mtd != null);
					}
				}
			} catch (ErroModelo e) {
				throw new ErroExecucao("Erro inserindo no cache: " + getClass());
			}
		}
	}

	private class CachePorRepositorio extends Cache<String, List<TipoNota>> {
		void put(TipoNota nota) {
			try {
				String idRepo = nota.getMetadado().getNotebookGuid();
				String nomeRepo = CacheNotebooks.getCache(getUsuario()).get(idRepo).getName();
				List<TipoNota> lsNotas = getOrDefault(idRepo, new ArrayList<TipoNota>());
				if (lsNotas.isEmpty()) {
					put(idRepo, lsNotas);
					put(nomeRepo, lsNotas);
				}
				lsNotas.add(nota);
			} catch (ErroModelo e) {
				throw new ErroExecucao("Erro inserindo no cache: " + getClass());
			}
		}
	}

	private boolean grupoHomonimoDeItemPermitido;
	private CachePorGrupo cacheGrupo = new CachePorGrupo();
	private CachePorRepositorio cacheRepo = new CachePorRepositorio();

	public boolean isGrupoHomonimoDeItemPermitido() {
		return grupoHomonimoDeItemPermitido;
	}

	public void setGrupoHomonimoDeItemPermitido(boolean grupoHomonimoDeItemPermitido) {
		this.grupoHomonimoDeItemPermitido = grupoHomonimoDeItemPermitido;
	}

	@Override
	public TipoNota put(String id, TipoNota nota) {
		cacheGrupo.put(nota);
		cacheRepo.put(nota);
		return super.put(id, nota);
	}

	@Override
	public void clear() {
		super.clear();
		cacheGrupo.clear();
		cacheRepo.clear();
	}

	@Override
	public Grupo<TipoNota> consultarPorGrupo(String nomeGrupo) throws ErroModelo {
		return cacheGrupo.getOrDefault(nomeGrupo, new Grupo<TipoNota>(nomeGrupo));	
	}

	@Override
	public Collection<TipoNota> consultarPorRepositorio(String nomeRepositorio) throws ErroModelo {
		return new ArrayList<TipoNota>(cacheRepo.getOrDefault(nomeRepositorio, Collections.emptyList()));
	}
}