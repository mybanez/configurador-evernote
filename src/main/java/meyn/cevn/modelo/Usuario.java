package meyn.cevn.modelo;

import com.evernote.auth.EvernoteService;

import meyn.cevn.ContextoEvn;
import meyn.util.modelo.entidade.Entidade;

public interface Usuario extends Entidade {
	String getId();

	void setId(String id);

	ContextoEvn getContexto();

	void setContexto(ContextoEvn contexto);

	EvernoteService getEvernoteService();

	void setEvernoteService(EvernoteService servico);

	String getToken();

	void setToken(String token);

	int getContadorAtualizacao();

	void setContadorAtualizacao(int contadorAtualizacao);

	String getPrefixoURL();

	void setPrefixoURL(String prefixoURL);

	Nota getLog();

	void setLog(Nota log);
}
