package meyn.cevn.modelo;

import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.SyncState;
import com.evernote.thrift.TException;

import meyn.cevn.modelo.usuario.Usuario;

public class ClienteEvn {
	private static String URL;

	private static final String USER_STORE_CLIENT = "USER_STORE";
	private static final String NOTE_STORE_CLIENT = "NOTE_STORE";

	public static String getURL() {
		return URL;
	}

	public static void setURL(String URL) {
		ClienteEvn.URL = URL;
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
		ContextoEvn.getContexto(usu).put(NOTE_STORE_CLIENT, nsc);
	}

	public static int getContadorAtualizacao(Usuario usu) throws EDAMUserException, EDAMSystemException, TException {
		NoteStoreClient noteStore = getNoteStore(usu);
		SyncState currentState;
		synchronized (noteStore) {
			currentState = noteStore.getSyncState();
		}
		return currentState.getUpdateCount();
	}
}
