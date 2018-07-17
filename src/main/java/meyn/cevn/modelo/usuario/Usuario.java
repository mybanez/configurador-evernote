package meyn.cevn.modelo.usuario;

import com.evernote.auth.EvernoteService;

import meyn.util.contexto.Contexto;
import meyn.util.modelo.ot.OT;

public interface Usuario extends OT {
	String getId();
	void setId(String id);
	Contexto getContexto();
	void setContexto(Contexto ctx);
	
	EvernoteService getEvernoteService();
	void setEvernoteService(EvernoteService srv);
	String getToken();
	void setToken(String tkn);
	int getContadorAtualizacao();
	void setContadorAtualizacao(int contadorAtualizacao);
	String getPrefixoURL();
	void setPrefixoURL(String prefixoURL);
}
