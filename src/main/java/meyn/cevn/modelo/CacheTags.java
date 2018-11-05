package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.evernote.edam.type.Tag;

import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public class CacheTags extends CacheEvn<String, Tag> {

	private static final String EXP_NOME_REPOSITORIO = "^<.+>";

	public static CacheTags getCache(Usuario usu) throws ErroModelo {
		try {
			CacheTags cache = (CacheTags) getCache(usu, CacheTags.class);
			if (!cache.isAtualizado()) {
				cache.clear();
				List<Tag> lsTags = ClienteEvn.consultarTags(usu);
				for (Tag tag : lsTags) {
					cache.put(tag.getGuid(), tag);
					cache.put(tag.getName(), tag);
				}
				cache.cacheRepo = cache.new CachePorRepositorio(usu);
				cache.setAtualizado(true);
			}
			return cache;
		} catch (Exception e) {
			throw new ErroModelo("Erro carregando cache de tags", e);
		}
	}
	
	private class CachePorRepositorio extends Cache<String, List<Tag>> {
		CachePorRepositorio(Usuario usu) throws ErroModelo {
			for (Tag tag : new HashSet<Tag>(CacheTags.this.values())) {
				Tag filho = tag;
				if (!tag.getName().matches(EXP_NOME_REPOSITORIO)) {
					while (tag.isSetParentGuid()) {
						tag = CacheTags.this.get(tag.getParentGuid());
						String nomeRepo = tag.getName();
						List<Tag> lsFilhos = getOrDefault(nomeRepo, new ArrayList<Tag>());
						if (lsFilhos.isEmpty()) {
							put(nomeRepo, lsFilhos);
						}
						lsFilhos.add(filho);
					}
				}
			}
		}
	};

	private CachePorRepositorio cacheRepo;

	public List<Tag> consultarPorRepositorio(String nomeRepo) {
		return new ArrayList<Tag>(cacheRepo.getOrDefault(nomeRepo, Collections.emptyList()));
	}
}
