package meyn.cevn.modelo.usuario;

import com.evernote.auth.EvernoteAuth;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.userstore.Constants;
import com.evernote.thrift.TException;

import meyn.cevn.modelo.ClienteEvn;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.CadastroImpl;
import meyn.util.modelo.cadastro.ErroCadastro;

public class CadastroUsuario extends CadastroImpl<Usuario, Usuario> {
	public CadastroUsuario() throws ErroCadastro {
		super();
	}

	@Override
	public Usuario consultarPorChavePrimaria(Usuario usu, Usuario chave, Class<?> molde) throws ErroCadastro {
		try {
			// Set up the UserStore client and check that we can speak to the server
			EvernoteAuth evernoteAuth = new EvernoteAuth(chave.getEvernoteService(), chave.getToken());
			ClientFactory factory = new ClientFactory(evernoteAuth);
			UserStoreClient userStore = factory.createUserStoreClient();
			if (!userStore.checkVersion("CEVN", Constants.EDAM_VERSION_MAJOR, Constants.EDAM_VERSION_MINOR)) {
				throw new ErroCadastro("Versão de protocolo do cliente Evernote incompatível");
			}
			ClienteEvn.setUserStore(chave, userStore);
			// Set up the NoteStore client
			NoteStoreClient noteStore = factory.createNoteStoreClient();
			ClienteEvn.setNoteStore(chave, noteStore);
			chave.setPrefixoURL("evernote:///view/" + userStore.getUser().getId() + "/" + userStore.getUser().getShardId() + "/");
			return chave;
		} catch (TException | EDAMUserException | EDAMSystemException e) {
			throw new ErroCadastro("Erro obtendo usuário Evernote: " + chave, e);
		}
	}

	public void atualizarContadorAtualizacao(Usuario usu) throws ErroModelo {
		try {
			usu.setContadorAtualizacao(ClienteEvn.getContadorAtualizacao(usu));
		} catch (EDAMUserException | EDAMSystemException | TException e) {
			throw new ErroModelo("Erro atualizando estado do usuário.", e);
		}
	}
}
