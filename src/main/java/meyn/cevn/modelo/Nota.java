package meyn.cevn.modelo;

import java.util.Date;

import com.evernote.edam.notestore.NoteMetadata;

public interface Nota extends OTEvn<NoteMetadata> {
	boolean isLembrete();
	void setLembrete(boolean isLembrete);
	Date getDataLembrete();
	void setDataLembrete(Date data);
	String getURL();
	void setURL(String url);
	String getConteudo();
	void setConteudo(String content);
}
