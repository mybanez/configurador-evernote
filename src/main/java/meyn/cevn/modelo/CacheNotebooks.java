package meyn.cevn.modelo;

import java.util.List;

import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Notebook;
import com.evernote.thrift.TException;

import meyn.cevn.ClienteEvn;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
public class CacheNotebooks extends CacheEvn<String, Notebook> {

	public static CacheNotebooks getCache(Usuario usu) throws ErroModelo {
		try {
			CacheNotebooks cache = (CacheNotebooks) getCache(usu, CacheNotebooks.class);
			if (!cache.isAtualizado()) {
				NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
				List<Notebook> lsNtbs;
				synchronized (noteStore) {
					lsNtbs = noteStore.listNotebooks();
				}
				for (Notebook cad : lsNtbs) {
					cache.put(cad.getGuid(), cad);
					cache.put(cad.getName(), cad);
				}
				cache.setAtualizado(true);
			}
			return cache;
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro carregando cache de notebooks.", e);
		}
	}
}
