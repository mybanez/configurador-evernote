package meyn.cevn.controle;

import java.util.ArrayList;
import java.util.Collection;

import javax.faces.bean.ManagedBean;

import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;

@SuppressWarnings("serial")
@ManagedBean(name = "console")
public class Console extends ManagedBeanImpl {

	private Collection<Interesse> clInteresses;
	private Collection<Projeto> clProjetos;
	
	public Console() throws ErroModelo {
		Usuario usu = getUsuario();
		clInteresses = new ArrayList<Interesse>(FabricaFachada.getFachada().consultarTodos(usu, ChavesModelo.INTERESSE));
		clProjetos = new ArrayList<Projeto>(FabricaFachada.getFachada().consultarTodos(usu, ChavesModelo.PROJETO));
	}

	public Collection<Interesse> getInteresses() throws ErroModelo {
		return clInteresses;
	}
	
	public Collection<Projeto> getProjetos() throws ErroModelo {
		return clProjetos; 
	}
}

