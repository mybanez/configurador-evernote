package meyn.cevn.modelo.projeto;

import java.util.Collection;

import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.Sumario;

public interface Projeto extends Nota {

	Etiqueta getEtiqueta();

	void setEtiqueta(Etiqueta etq);

	Collection<Interesse> getInteresses();

	void setInteresses(Collection<Interesse> clInteresses);

	Collection<Acao> getAcoesCalendario();

	void setAcoesCalendario(Collection<Acao> clAcoes);

	Collection<Acao> getAcoesEmFoco();

	void setAcoesEmFoco(Collection<Acao> clAcoes);

	Collection<Acao> getAcoesDelegadas();

	void setAcoesDelegadas(Collection<Acao> clAcoes);

	Collection<Acao> getAcoesProximas();

	void setAcoesProximas(Collection<Acao> clAcoes);

	Collection<Referencia> getReferencias();

	void setReferencias(Collection<Referencia> clReferencias);

	Sumario getSumario();

	void setSumario(Sumario sum);

	Sumario getSumarioValidacao();

	void setSumarioValidacao(Sumario sum);
}
