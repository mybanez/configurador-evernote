package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.notestore.SyncChunk;
import com.evernote.edam.notestore.SyncChunkFilter;
import com.evernote.edam.notestore.SyncState;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Tag;
import com.evernote.edam.type.User;
import com.evernote.edam.userstore.Constants;
import com.evernote.thrift.TException;

import meyn.cevn.ContextoEvn;
import meyn.util.modelo.ErroModelo;

public final class ClienteEvn {

	public static final String TAGS = "TAG";
	public static final String NOTEBOOKS = "NTB";

	private static final String AUTH_TOKEN_SAND_BOX = "S=s1:U=93be0:E=1683a17eb9d:C=160e266bcc0:P=1cd:A=en-devtoken:V=2:H=a12125d6ec082e623c91bcc59f25de88";
	private static final String AUTH_TOKEN_PROD = "S=s202:U=187f3ba:E=168dde82bfd:C=1618636fc70:P=1cd:A=en-devtoken:V=2:H=cb717752d1b131b749b1d63ef31e8109";

	private static final String USER_STORE_CLIENT = "USER_STORE";
	private static final String NOTE_STORE_CLIENT = "NOTE_STORE";
	private static final SyncChunkFilter SYNC_FILTER = new SyncChunkFilter();

	static {
		SYNC_FILTER.setIncludeTags(true);
		SYNC_FILTER.setIncludeNotebooks(true);
		SYNC_FILTER.setIncludeNotes(true);
		SYNC_FILTER.setIncludeNoteAttributes(true);
	}

	private static UserStoreClient getUserStore(Usuario usu) {
		return (UserStoreClient) ContextoEvn.getContexto(usu).get(USER_STORE_CLIENT);
	}

	private static void setUserStore(Usuario usu, UserStoreClient usc) {
		ContextoEvn.getContexto(usu).put(USER_STORE_CLIENT, usc);

	}

	private static NoteStoreClient getNoteStore(Usuario usu) {
		return (NoteStoreClient) ContextoEvn.getContexto(usu).get(NOTE_STORE_CLIENT);
	}

	private static void setNoteStore(Usuario usu, NoteStoreClient nsc) {
		ContextoEvn.getContextoLocal(usu).put(NOTE_STORE_CLIENT, nsc);
	}

	public static String getNomeUsuario(Usuario usu) throws ErroModelo {
		try {
			return getUserStore(usu).getUser().getUsername();
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro obtendo nome do usuário", e);
		}
	}

	public static boolean isConectado(Usuario usu) {
		return ContextoEvn.getContexto(usu).containsKey(USER_STORE_CLIENT);
	}

	public static void conectar(Usuario usu) throws ErroModelo {
		try {
			// usu.setEvernoteService(EvernoteService.SANDBOX);
			// usu.setToken(AUTH_TOKEN_SAND_BOX);
			usu.setEvernoteService(EvernoteService.PRODUCTION);
			usu.setToken(AUTH_TOKEN_PROD);
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
		} catch (TException | EDAMUserException | EDAMSystemException e) {
			throw new ErroModelo("Erro conectando ao servidor. Usuário: " + usu, e);
		}
	}

	public static Collection<String> consultarAtualizacoes(Usuario usu, Collection<String> clIdsNtbk) throws ErroModelo {
		try {
			TreeSet<String> clIdsAtu = new TreeSet<String>();
			NoteStoreClient noteStore = getNoteStore(usu);
			SyncState currentSyncState;
			SyncChunk syncChunk;
			int contadorBase = usu.getContadorAtualizacao();
			boolean primeiraAtualizacao = contadorBase == 0;
			if (primeiraAtualizacao) {
				clIdsAtu.add(TAGS);
				clIdsAtu.add(NOTEBOOKS);
				clIdsAtu.addAll(clIdsNtbk);
			}
			synchronized (noteStore) {
				currentSyncState = noteStore.getSyncState();
			}
			usu.setContadorAtualizacao(currentSyncState.getUpdateCount());
			if (!primeiraAtualizacao && contadorBase < usu.getContadorAtualizacao()) {
				do {
					synchronized (noteStore) {
						syncChunk = noteStore.getFilteredSyncChunk(contadorBase, 100, SYNC_FILTER);
					}
					if (syncChunk.isSetTags()) {
						clIdsAtu.add(TAGS);
					}
					if (syncChunk.isSetNotebooks()) {
						clIdsAtu.add(NOTEBOOKS);
					}
					if (syncChunk.isSetNotes()) {
						for (Note note : syncChunk.getNotes()) {
							String idNtbk = note.getNotebookGuid();
							if (clIdsNtbk.contains(idNtbk)) {
								clIdsAtu.add(idNtbk);
							}
						}
					}
					if (syncChunk.isSetChunkHighUSN() && syncChunk.getChunkHighUSN() < syncChunk.getUpdateCount()) {
						contadorBase = syncChunk.getChunkHighUSN();
					} else {
						break;
					}
				} while (clIdsAtu.size() < clIdsNtbk.size() + 2);
			}
			return clIdsAtu;
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro consultando atualizações no servidor", e);
		}
	}

	public static List<Tag> consultarTags(Usuario usu) throws ErroModelo {
		try {
			List<Tag> lsMtds;
			NoteStoreClient noteStore = getNoteStore(usu);
			synchronized (noteStore) {
				lsMtds = noteStore.listTags();
			}
			return lsMtds == null ? Collections.emptyList() : new ArrayList<Tag>(lsMtds);
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro consultando tags no servidor", e);
		}
	}

	public static List<Notebook> consultarNotebooks(Usuario usu) throws ErroModelo {
		try {
			List<Notebook> lsMtds;
			NoteStoreClient noteStore = getNoteStore(usu);
			synchronized (noteStore) {
				lsMtds = noteStore.listNotebooks();
			}
			return lsMtds == null ? Collections.emptyList() : new ArrayList<Notebook>(lsMtds);
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro consultando notebooks no servidor", e);
		}
	}

	public static List<NoteMetadata> consultarNotas(Usuario usu, NoteFilter filtro, NotesMetadataResultSpec campos) throws ErroModelo {
		try {
			List<NoteMetadata> lsMtds = new ArrayList<NoteMetadata>();
			NoteStoreClient noteStore = getNoteStore(usu);
			int desloc = 0;
			NotesMetadataList lsMtdsPag;
			do {
				synchronized (noteStore) {
					lsMtdsPag = noteStore.findNotesMetadata(filtro, desloc, com.evernote.edam.limits.Constants.EDAM_USER_NOTES_MAX, campos);
				}
				lsMtds.addAll(lsMtdsPag.getNotes());
				desloc += lsMtdsPag.getNotesSize();
			} while (lsMtdsPag.getTotalNotes() > desloc);
			return lsMtds;
		} catch (TException | EDAMUserException | EDAMSystemException | EDAMNotFoundException e) {
			throw new ErroModelo("Erro consultando notas no servidor", e);
		}
	}

	public static Note consultarNota(Usuario usu, String id, boolean carregarConteudo) throws ErroModelo {
		try {
			NoteStoreClient noteStore = getNoteStore(usu);
			synchronized (noteStore) {
				return noteStore.getNote(id, carregarConteudo, false, false, false);
			}
		} catch (TException | EDAMUserException | EDAMSystemException | EDAMNotFoundException e) {
			throw new ErroModelo("Erro consultando nota no servidor", e);
		}
	}

	public static Note incluirNota(Usuario usu, Note mtd) throws ErroModelo {
		try {
			NoteStoreClient noteStore = getNoteStore(usu);
			synchronized (noteStore) {
				return noteStore.createNote(mtd);
			}
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroModelo("Erro incluindo nota no servidor", e);
		}
	}

	public static void atualizarNota(Usuario usu, Note mtd) throws ErroModelo {
		try {
			NoteStoreClient noteStore = getNoteStore(usu);
			synchronized (noteStore) {
				noteStore.updateNote(mtd);
			}
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroModelo("Erro alterando nota no servidor", e);
		}
	}

	public static void excluirNota(Usuario usu, String id) throws ErroModelo {
		try {
			NoteStoreClient noteStore = getNoteStore(usu);
			synchronized (noteStore) {
				noteStore.deleteNote(id);
			}
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroModelo("Erro excluindo nota no servidor", e);
		}
	}

}
