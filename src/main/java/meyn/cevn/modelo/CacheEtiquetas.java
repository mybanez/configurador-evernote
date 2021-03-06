package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import com.evernote.edam.type.Tag;

import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
abstract class CacheEtiquetas<TipoEtq extends Etiqueta> extends CacheEntidadesEvn<TipoEtq> {

	private class CachePorGrupo extends Cache<String, Grupo<TipoEtq>> {
		CachePorGrupo() throws ErroModelo {
			setLogger(CacheEtiquetas.this.getLogger());
			Grupo<TipoEtq> grupo, subgrupo;
			CacheTags cacheTag = CacheTags.getCache(getUsuario());
			for (TipoEtq etq : CacheEtiquetas.this.values()) {
				// Não representa um grupo já inserido no cache
				if (!containsKey(etq.getId())) {
					subgrupo = null;
					Tag tag = etq.getMetadado();
					while (tag.isSetParentGuid()) {
						tag = cacheTag.get(tag.getParentGuid());
						String idGrupo = tag.getGuid();
						String nomeGrupo = tag.getName();
						grupo = getOrDefault(idGrupo, new Grupo<TipoEtq>(nomeGrupo, CacheEtiquetas.this.get(idGrupo)));
						if (grupo.isVazio()) {
							put(idGrupo, grupo);
							put(nomeGrupo, grupo);
						}
						if (subgrupo != null) {
							grupo.getGruposFilho().add(subgrupo);
							etq = subgrupo.getEntidade();
							// Além de entidade, representa um grupo
							if (etq != null) {
								grupo.getEntidadesFilho().remove(etq);
							}
						} else {
							grupo.getEntidadesFilho().add(etq);
						}
						subgrupo = grupo;
					}
				}
			}
		}
	}

	private class CachePorRepositorio extends Cache<String, SortedSet<TipoEtq>> {
		CachePorRepositorio() throws ErroModelo {
			setLogger(CacheEtiquetas.this.getLogger());
			CacheTags cacheTag = CacheTags.getCache(getUsuario());
			for (TipoEtq etq : CacheEtiquetas.this.values()) {
				Tag mtd = etq.getMetadado();
				while (mtd.isSetParentGuid()) {
					mtd = cacheTag.get(mtd.getParentGuid());
					String idRepo = mtd.getGuid();
					String nomeRepo = mtd.getName();
					SortedSet<TipoEtq> stFilhos = getOrDefault(idRepo, new TreeSet<TipoEtq>((a, b) -> a.getNome().compareTo(b.getNome())));
					if (stFilhos.isEmpty()) {
						put(idRepo, stFilhos);
						put(nomeRepo, stFilhos);
					}
					stFilhos.add(etq);
				}
			}
		}
	}

	private CachePorGrupo cacheGrupo;
	private CachePorRepositorio cacheRepo;

	protected void executarPosCarregamento() throws ErroModelo {
		cacheGrupo = new CachePorGrupo();
		cacheRepo = new CachePorRepositorio();
	}

	@Override
	public Grupo<TipoEtq> consultarPorGrupo(String nomeGrupo) throws ErroModelo {
		return cacheGrupo.getOrDefault(nomeGrupo, new Grupo<TipoEtq>(nomeGrupo));
	}

	@Override
	public Collection<TipoEtq> consultarPorRepositorio(String nomeRepositorio) throws ErroModelo {
		return new ArrayList<TipoEtq>(cacheRepo.getOrDefault(nomeRepositorio, Collections.emptySortedSet()));
	}
}
