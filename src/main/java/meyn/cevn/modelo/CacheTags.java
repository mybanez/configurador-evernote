package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.TException;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public class CacheTags extends CacheEvn<String, Tag> {

	private static final String EXP_NOME_REPOSITORIO = "^<.+>";

	private class CachePorRepositorio extends Cache<String, List<String>> {
		CachePorRepositorio(Usuario usu) throws ErroModelo {
			for (Tag tag : new HashSet<Tag>(CacheTags.this.values())) {
				String idFilho = tag.getGuid();
				if (!tag.getName().matches(EXP_NOME_REPOSITORIO)) {
					while (tag.isSetParentGuid()) {
						tag = CacheTags.this.get(tag.getParentGuid());
						String nomeRepo = tag.getName();
						List<String> lsFilhos = getOrDefault(nomeRepo, new ArrayList<String>());
						if (lsFilhos.isEmpty()) {
							put(nomeRepo, lsFilhos);
						}
						lsFilhos.add(idFilho);
					}
				}
			}
		}
	};

	private CachePorRepositorio cacheRepo;

	public static CacheTags getCache(Usuario usu) throws ErroModelo {
		try {
			CacheTags cache = (CacheTags) getCache(usu, CacheTags.class);
			if (!cache.isAtualizado()) {
				cache.clear();
				NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
				List<Tag> lsTags;
				synchronized (noteStore) {
					lsTags = noteStore.listTags();
				}
				for (Tag tag : lsTags) {
					cache.put(tag.getGuid(), tag);
					cache.put(tag.getName(), tag);
				}
				cache.cacheRepo = cache.new CachePorRepositorio(usu);
				cache.setAtualizado(true);
			}
			return cache;
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro carregando cache de tags.", e);
		}
	}

	public List<String> consultarPorRepositorio(String nomeRepo) {
		return new ArrayList<String>(cacheRepo.getOrDefault(nomeRepo, Collections.emptyList()));
	}
}
