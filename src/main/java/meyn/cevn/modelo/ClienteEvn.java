package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;

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

	@FunctionalInterface
	private static interface Operacao {
		void executar(NoteStoreClient cliente);
	}

	@SuppressWarnings("serial")
	private static class ClienteArmazenamento extends ConcurrentLinkedQueue<Operacao> {
		NoteStoreClient noteStoreClient;

		ClienteArmazenamento(NoteStoreClient noteStoreClient) {
			this.noteStoreClient = noteStoreClient;
			new Thread() {
				@Override
				public void run() {
					while (true) {
						while (!isEmpty()) {
							synchronized (noteStoreClient) {
								poll().executar(noteStoreClient);
							}
						}
						synchronized (ClienteArmazenamento.this) {
							try {
								ClienteArmazenamento.this.wait();
							} catch (InterruptedException e) {
							}
						}
					}
				}
			}.start();
		}

		void executar(Operacao oper) {
			add(oper);
			synchronized (this) {
				notify();
			}
		}
	}

	private static class EstoqueClientes {
		int indice = 0;
		ClienteArmazenamento[] clientes = new ClienteArmazenamento[QTD_CLIENTES];

		EstoqueClientes(ClientFactory factory) throws EDAMUserException, EDAMSystemException, TException {
			for (int i = 0; i < QTD_CLIENTES; i++) {
				clientes[i] = new ClienteArmazenamento(factory.createNoteStoreClient());
			}
		}

		ClienteArmazenamento getCliente() {
			return clientes[indice = (indice + 1) % QTD_CLIENTES];
		}

		NoteStoreClient getNoteStoreClient() {
			return getCliente().noteStoreClient;
		}

		int getTamanhoFilaServidor() {
			int tam = 0;
			for (ClienteArmazenamento cliente : clientes) {
				tam += cliente.size();
			}
			return tam;
		}
	}

	private static final String AUTH_TOKEN_SAND_BOX = "S=s1:U=93be0:E=1683a17eb9d:C=160e266bcc0:P=1cd:A=en-devtoken:V=2:H=a12125d6ec082e623c91bcc59f25de88";
	private static final String AUTH_TOKEN_PROD = "S=s202:U=187f3ba:E=17037151a4d:C=168df63eb30:P=1cd:A=en-devtoken:V=2:H=c756a3e04fc6a0aaf609600ff981779c";

	private static final String USER_STORE_CLIENT = "USER_STORE_CLIENT";

	private static final String ESTOQUE_CLIENTES = "FILA_CLIENTES";
	private static final int QTD_CLIENTES = 10;

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

	private static EstoqueClientes getEstoqueClientes(Usuario usu) {
		return (EstoqueClientes) ContextoEvn.getContexto(usu).get(ESTOQUE_CLIENTES);
	}

	private static void setEstoqueClientes(Usuario usu, EstoqueClientes filaClientes) {
		ContextoEvn.getContexto(usu).put(ESTOQUE_CLIENTES, filaClientes);
	}

	public static int getTamanhoFilaServidor(Usuario usu) {
		return getEstoqueClientes(usu).getTamanhoFilaServidor();
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
			setEstoqueClientes(usu, new EstoqueClientes(factory));
			usu.setPrefixoURL("evernote:///view/" + user.getId() + "/" + user.getShardId() + "/");
			usu.setContadorAtualizacao(0);
		} catch (TException | EDAMUserException | EDAMSystemException e) {
			throw new ErroModelo("Erro conectando ao servidor. Usuário: " + usu, e);
		}
	}

	public static Collection<String> consultarAtualizacoes(Usuario usu, Collection<String> clIdsNtbk) throws ErroModelo {
		try {
			TreeSet<String> clIdsAtu = new TreeSet<String>();
			NoteStoreClient noteStoreClient = getEstoqueClientes(usu).getNoteStoreClient();
			SyncState currentSyncState;
			SyncChunk syncChunk;
			int contadorBase = usu.getContadorAtualizacao();
			boolean primeiraAtualizacao = contadorBase == 0;
			if (primeiraAtualizacao) {
				clIdsAtu.add(TAGS);
				clIdsAtu.add(NOTEBOOKS);
				clIdsAtu.addAll(clIdsNtbk);
			}
			synchronized (noteStoreClient) {
				currentSyncState = noteStoreClient.getSyncState();
			}
			usu.setContadorAtualizacao(currentSyncState.getUpdateCount());
			if (!primeiraAtualizacao && contadorBase < usu.getContadorAtualizacao()) {
				do {
					synchronized (noteStoreClient) {
						syncChunk = noteStoreClient.getFilteredSyncChunk(contadorBase, 100, SYNC_FILTER);
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
			NoteStoreClient noteStoreClient = getEstoqueClientes(usu).getNoteStoreClient();
			synchronized (noteStoreClient) {
				lsMtds = noteStoreClient.listTags();
			}
			return lsMtds == null ? Collections.emptyList() : new ArrayList<Tag>(lsMtds);
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro consultando tags no servidor", e);
		}
	}

	public static List<Notebook> consultarNotebooks(Usuario usu) throws ErroModelo {
		try {
			List<Notebook> lsMtds;
			NoteStoreClient noteStoreClient = getEstoqueClientes(usu).getNoteStoreClient();
			synchronized (noteStoreClient) {
				lsMtds = noteStoreClient.listNotebooks();
			}
			return lsMtds == null ? Collections.emptyList() : new ArrayList<Notebook>(lsMtds);
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro consultando notebooks no servidor", e);
		}
	}

	public static List<NoteMetadata> consultarNotas(Usuario usu, NoteFilter filtro, NotesMetadataResultSpec campos) throws ErroModelo {
		try {
			List<NoteMetadata> lsMtds = new ArrayList<NoteMetadata>();
			NoteStoreClient noteStoreClient = getEstoqueClientes(usu).getNoteStoreClient();
			int desloc = 0;
			NotesMetadataList lsMtdsPag;
			do {
				synchronized (noteStoreClient) {
					lsMtdsPag = noteStoreClient.findNotesMetadata(filtro, desloc, com.evernote.edam.limits.Constants.EDAM_USER_NOTES_MAX,
					        campos);
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
			NoteStoreClient noteStoreClient = getEstoqueClientes(usu).getNoteStoreClient();
			synchronized (noteStoreClient) {
				return noteStoreClient.getNote(id, carregarConteudo, false, false, false);
			}
		} catch (TException | EDAMUserException | EDAMSystemException | EDAMNotFoundException e) {
			throw new ErroModelo("Erro consultando nota no servidor", e);
		}
	}

	public static Note incluirNota(Usuario usu, Note mtd) throws ErroModelo {
		try {
			NoteStoreClient noteStoreClient = getEstoqueClientes(usu).getNoteStoreClient();
			synchronized (noteStoreClient) {
				return noteStoreClient.createNote(mtd);
			}
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroModelo("Erro incluindo nota no servidor", e);
		}
	}

	public static void excluirNota(Usuario usu, String id) throws ErroModelo {
		getEstoqueClientes(usu).getCliente().executar((NoteStoreClient noteStoreClient) -> {
			try {
				noteStoreClient.deleteNote(id);
			} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
				LogManager.getLogger(ClienteEvn.class).error(new ErroModelo("Erro excluindo nota no servidor", e));
			}
		});
	}

	public static void atualizarNota(Usuario usu, Note mtd) throws ErroModelo {
		getEstoqueClientes(usu).getCliente().executar((NoteStoreClient noteStoreClient) -> {
			try {
				noteStoreClient.updateNote(mtd);
			} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
				LogManager.getLogger(ClienteEvn.class).error(new ErroModelo("Erro alterando nota no servidor", e));
			}
		});
	}
}
