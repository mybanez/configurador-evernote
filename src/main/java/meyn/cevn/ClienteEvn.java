package meyn.cevn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.evernote.auth.EvernoteAuth;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.notestore.SyncChunkFilter;
import com.evernote.edam.notestore.SyncState;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.User;
import com.evernote.edam.userstore.Constants;
import com.evernote.thrift.TException;

import meyn.cevn.modelo.CacheNotebooks;
import meyn.cevn.modelo.CacheTags;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.Fachada;
import meyn.cevn.modelo.Usuario;
import meyn.util.modelo.ErroModelo;

public final class ClienteEvn {

	private static final String USER_STORE_CLIENT = "USER_STORE";
	private static final String NOTE_STORE_CLIENT = "NOTE_STORE";
	private static final SyncChunkFilter SYNC_FILTER = new SyncChunkFilter();
	private static final Logger LOGGER = LogManager.getLogger(ClienteEvn.class);

	static {
		SYNC_FILTER.setIncludeNotes(true);
		SYNC_FILTER.setIncludeNoteAttributes(true);
		SYNC_FILTER.setIncludeTags(true);
		SYNC_FILTER.setIncludeNotebooks(true);
	}

	private static String URL;

	public static String getURL() {
		return URL;
	}

	public static void setURL(String URL) {
		ClienteEvn.URL = URL;
	}

	public static boolean isIniciado(Usuario usu) {
		return ContextoEvn.getContexto(usu).containsKey(USER_STORE_CLIENT);
	}

	public static UserStoreClient getUserStore(Usuario usu) {
		return (UserStoreClient) ContextoEvn.getContexto(usu).get(USER_STORE_CLIENT);
	}

	public static void setUserStore(Usuario usu, UserStoreClient usc) {
		ContextoEvn.getContexto(usu).put(USER_STORE_CLIENT, usc);

	}

	public static NoteStoreClient getNoteStore(Usuario usu) {
		return (NoteStoreClient) ContextoEvn.getContexto(usu).get(NOTE_STORE_CLIENT);
	}

	public static void setNoteStore(Usuario usu, NoteStoreClient nsc) {
		ContextoEvn.getContextoLocal(usu).put(NOTE_STORE_CLIENT, nsc);
	}

	public static void conectar(Usuario usu) throws ErroModelo {
		try {
			// Set up the UserStore client and check that we can speak to the server
			EvernoteAuth evernoteAuth = new EvernoteAuth(usu.getEvernoteService(), usu.getToken());
			ClientFactory factory = new ClientFactory(evernoteAuth);
			UserStoreClient userStore = factory.createUserStoreClient();
			if (!userStore.checkVersion("CEVN", Constants.EDAM_VERSION_MAJOR, Constants.EDAM_VERSION_MINOR)) {
				throw new ErroModelo("Versão de protocolo do cliente Evernote incompatível");
			}
			User user = userStore.getUser();
			setUserStore(usu, userStore);
			NoteStoreClient noteStore = factory.createNoteStoreClient();
			setNoteStore(usu, noteStore);
			usu.setPrefixoURL("evernote:///view/" + user.getId() + "/" + user.getShardId() + "/");
			usu.setContadorAtualizacao(0);
			usu.setLog(FabricaFachada.getFachada().gerarLogUsuario(usu));
		} catch (TException | EDAMUserException | EDAMSystemException e) {
			throw new ErroModelo("Erro obtendo usuário Evernote: " + usu, e);
		}
	}

	public static void invalidarCaches(Usuario usu) throws ErroModelo {
		try {
			NoteStoreClient noteStore = getNoteStore(usu);
			SyncState currentSyncState;
			SyncChunk syncChunk;
			int contadorBase = usu.getContadorAtualizacao();
			boolean primeiraAtualizacao = contadorBase == 0;
			boolean atualizarTags = primeiraAtualizacao;
			boolean atualizarNotebooks = primeiraAtualizacao;
			boolean atualizarLogs = primeiraAtualizacao;
			boolean atualizarSumarios = primeiraAtualizacao;
			boolean atualizarEntidades = primeiraAtualizacao;
			Fachada fc = FabricaFachada.getFachada();
			synchronized (noteStore) {
				currentSyncState = noteStore.getSyncState();
			}
			usu.setContadorAtualizacao(currentSyncState.getUpdateCount());
			if (!primeiraAtualizacao && contadorBase < usu.getContadorAtualizacao()) {
				CacheNotebooks cacheNtb = CacheNotebooks.getCache(usu);
				String idRepoLogs = cacheNtb.get(fc.getNomeRepositorio(ChavesModelo.LOG)).getGuid();
				String idRepoSums = cacheNtb.get(fc.getNomeRepositorio(ChavesModelo.SUMARIO)).getGuid();
				do {
					synchronized (noteStore) {
						syncChunk = noteStore.getFilteredSyncChunk(contadorBase, 100, SYNC_FILTER);
					}
					if (syncChunk.isSetNotebooks()) {
						atualizarNotebooks = true;
					}
					if (syncChunk.isSetTags()) {
						atualizarTags = true;
						atualizarEntidades = true;
					}
					if (syncChunk.isSetNotes()) {
						for (Note note : syncChunk.getNotes()) {
							String idNtb = note.getNotebookGuid();
							if (idNtb.equals(idRepoLogs)) {
								atualizarLogs = true;
							} else if (idNtb.equals(idRepoSums)) {
								atualizarSumarios = true;
							} else {
								atualizarEntidades = true;
							}
						}
					}
					if (syncChunk.isSetChunkHighUSN() && syncChunk.getChunkHighUSN() < syncChunk.getUpdateCount()) {
						contadorBase = syncChunk.getChunkHighUSN();
					} else {
						break;
					}
				} while (!atualizarTags || !atualizarNotebooks || !atualizarLogs || !atualizarSumarios
						|| !atualizarEntidades);
			}
			if (atualizarNotebooks) {
				CacheNotebooks.getCache(usu).setAtualizado(false);
				LOGGER.debug("atualizar Notebooks");
			}
			if (atualizarTags) {
				CacheTags.getCache(usu).setAtualizado(false);
				LOGGER.debug("atualizar Tags");
			}
			if (atualizarLogs) {
				fc.invalidarCaches(usu, ChavesModelo.LOG);
				LOGGER.debug("atualizar Logs");
			}
			if (atualizarSumarios) {
				fc.invalidarCaches(usu, ChavesModelo.SUMARIO);
				LOGGER.debug("atualizar Sumarios");
			}
			if (atualizarEntidades) {
				fc.invalidarCaches(usu, ChavesModelo.INTERESSE);
				fc.invalidarCaches(usu, ChavesModelo.PROJETO);
				fc.invalidarCaches(usu, ChavesModelo.ACAO);
				fc.invalidarCaches(usu, ChavesModelo.REFERENCIA);
				LOGGER.debug("atualizar Entidades");
			}

		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro obtendo status do usuário", e);
		}
	}
}
