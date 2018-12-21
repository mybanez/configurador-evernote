package meyn.cevn.modelo.interesse;

import java.util.Collection;

import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.Sumario;

public interface Interesse extends Etiqueta {

	Collection<Projeto> getProjetos();

	void setProjetos(Collection<Projeto> clProjs);

	Collection<Acao> getAcoes();

	void setAcoes(Collection<Acao> clAcoes);

	Collection<Referencia> getReferencias();

	void setReferencias(Collection<Referencia> clReferencias);

	Sumario getSumario();

	void setSumario(Sumario sum);
}
