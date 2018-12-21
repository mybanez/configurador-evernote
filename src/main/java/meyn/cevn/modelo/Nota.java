package meyn.cevn.modelo;

import java.util.Date;

import com.evernote.edam.notestore.NoteMetadata;

public interface Nota extends EntidadeEvn<NoteMetadata> {
	long getDataCriacao();

	void setDataCriacao(long dtCriacao);

	long getDataAlteracao();

	void setDataAlteracao(long dtAlteracao);

	String getDataCriacaoFmt();

	void setDataCriacaoFmt(String dtCriacao);

	String getDataAlteracaoFmt();

	void setDataAlteracaoFmt(String dtAlteracao);

	boolean isLembrete();

	void setLembrete(boolean isLembrete);

	Date getDataLembrete();

	void setDataLembrete(Date data);

	String getURL();

	void setURL(String url);

	String getConteudo();

	void setConteudo(String content);
}
