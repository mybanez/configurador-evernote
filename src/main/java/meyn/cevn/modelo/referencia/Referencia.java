package meyn.cevn.modelo.referencia;

import java.util.Collection;

import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.Projeto;

public interface Referencia extends Nota {

	Collection<Etiqueta> getFormatos();
	void setFormatos(Collection<Etiqueta> clFormatos);
	Collection<Etiqueta> getTemas();
	void setTemas(Collection<Etiqueta> clTemas);
	
	Collection<Interesse> getInteresses();
	void setInteresses(Collection<Interesse> clIntrs);
	Collection<Projeto> getProjetos();
	void setProjetos(Collection<Projeto> clProjs);
}
