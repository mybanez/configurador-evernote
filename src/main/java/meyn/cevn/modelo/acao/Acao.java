package meyn.cevn.modelo.acao;

import java.util.Collection;
import java.util.Date;

import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.Projeto;

public interface Acao extends Nota {

	boolean isFoco();

	void setFoco(boolean ehFoco);

	boolean isDelegada();

	void setDelegada(boolean ehDelegada);

	boolean isProxima();

	void setProxima(boolean ehProxima);

	boolean isComunicacao();

	void setComunicacao(boolean ehComunicacao);

	boolean isLeituraRevisao();

	void setLeituraRevisao(boolean ehLeituraRevisao);

	Date getDataLembrete();

	void setDataLembrete(Date tempo);

	Interesse getEmpregador();

	void setEmpregador(Interesse intr);

	Collection<Projeto> getProjetos();

	void setProjetos(Collection<Projeto> clProjs);
}
