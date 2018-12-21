package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evernote.edam.type.Notebook;

import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public class CacheNotebooks extends CacheEvn<String, Notebook> {

	public static CacheNotebooks getCache(Usuario usu) throws ErroModelo {
		try {
			CacheNotebooks cache = (CacheNotebooks) getCache(usu, CacheNotebooks.class);
			if (!cache.isAtualizado()) {
				List<Notebook> lsNtbks = ClienteEvn.consultarNotebooks(usu);
				for (Notebook ntb : lsNtbks) {
					cache.put(ntb.getGuid(), ntb);
					cache.put(ntb.getName(), ntb);
					String nomePilha = ntb.getStack();
					if (nomePilha != null) {
						cache.cachePilha.putIfAbsent(nomePilha, new ArrayList<Notebook>());
						cache.cachePilha.get(nomePilha).add(ntb);
					}
				}
				cache.setAtualizado(true);
			}
			return cache;
		} catch (Exception e) {
			throw new ErroModelo("Erro carregando cache de notebooks", e);
		}
	}

	private Cache<String, List<Notebook>> cachePilha = new Cache<String, List<Notebook>>() {
	};

	public List<Notebook> consultarPorPilha(String nomePilha) {
		return new ArrayList<Notebook>(cachePilha.getOrDefault(nomePilha, Collections.emptyList()));
	}
}
