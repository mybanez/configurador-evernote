package meyn.cevn.modelo;

import java.util.Collection;

import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.acao.CadastroAcao;
import meyn.cevn.modelo.interesse.CadastroInteresse;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.CadastroReferencia;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.CadastroSumario;
import meyn.cevn.modelo.sumario.Sumario;
import meyn.cevn.modelo.usuario.CadastroUsuario;
import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.FachadaModeloImpl;

public class Fachada extends FachadaModeloImpl {

	private CadastroUsuario getCadUsuario() throws ErroModelo {
		return (CadastroUsuario) this.<Usuario, Usuario>getCadastro(ChavesModelo.USUARIO);
	}

	private CadastroSumario getCadSumario() throws ErroModelo {
		return  (CadastroSumario) this.<Usuario, Sumario>getCadastro(ChavesModelo.SUMARIO);
	}

	private CadastroInteresse getCadInteresse() throws ErroModelo {
		return (CadastroInteresse) this.<Usuario, Interesse>getCadastro(ChavesModelo.INTERESSE);
	}

	private CadastroProjeto getCadProjeto() throws ErroModelo {
		return (CadastroProjeto) this.<Usuario, Projeto>getCadastro(ChavesModelo.PROJETO);
	}

	private CadastroAcao getCadAcao() throws ErroModelo {
		return (CadastroAcao) this.<Usuario, Acao>getCadastro(ChavesModelo.ACAO);
	}

	private CadastroReferencia getCadReferencia() throws ErroModelo {
		return (CadastroReferencia) this.<Usuario, Referencia>getCadastro(ChavesModelo.REFERENCIA);
	}

	public Usuario consultarUsuario(Usuario usu) throws ErroModelo {
		return consultarPorChavePrimaria(ChavesModelo.USUARIO, usu);
	}

	public void atualizarContadorAtualizacao(Usuario usu) throws ErroModelo {
		getCadUsuario().atualizarContadorAtualizacao(usu);
	}	

	public void gerarSumarioInteresses(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadSumario();
		CadastroInteresse cadIntr = getCadInteresse();
		Collection<Interesse> clIntrs = cadIntr.consultarTodos(usu);
		cadSum.gerarSumarioInteresses(usu, clIntrs);
		cadSum.gerarSumarioInteressesDetalhado(usu, clIntrs);
		Grupo<Interesse> raiz = cadIntr.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioInteresses(usu, raiz);
		cadSum.gerarSumarioInteressesDetalhado(usu, raiz);	
	}

	public void gerarSumarioInteresse(Usuario usu, String id) throws ErroModelo {
		getCadSumario().gerarSumarioInteresse(usu, getCadInteresse().consultarPorChavePrimaria(usu, id));
	}

	public void gerarSumarioProjetos(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadSumario();
		CadastroProjeto cadProj = getCadProjeto();
		Collection<Projeto> clProjs = cadProj.validarTodos(usu);
		cadSum.gerarSumarioProjetos(usu, clProjs);
		cadSum.gerarSumarioProjetosDetalhado(usu, clProjs);
		cadSum.gerarValidacaoProjetos(usu, clProjs);		
		Grupo<Projeto> raiz = cadProj.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioProjetos(usu, raiz);
		cadSum.gerarSumarioProjetosDetalhado(usu, raiz);
	}

	public void gerarSumarioProjeto(Usuario usu, String id) throws ErroModelo {
		getCadSumario().gerarSumarioProjeto(usu, getCadProjeto().consultarPorChavePrimaria(usu, id));
	}
	
	public void gerarValidacaoProjetos(Usuario usu) throws ErroModelo {
		getCadSumario().gerarValidacaoProjetos(usu, getCadProjeto().validarTodos(usu));
	}

	public void gerarValidacaoProjeto(Usuario usu, String id) throws ErroModelo {
		getCadSumario().gerarValidacaoProjeto(usu, getCadProjeto().validarPorChavePrimaria(usu, id));
	}

	public void gerarSumarioAcoes(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadSumario();
		CadastroAcao cadAcao = getCadAcao();
		Collection<Acao> clAcoes = cadAcao.validarTodos(usu);
		cadSum.gerarSumarioAcoes(usu, clAcoes);
		cadSum.gerarSumarioAcoesDetalhado(usu, clAcoes);
		cadSum.gerarValidacaoAcoes(usu, clAcoes);	
		Grupo<Acao> raiz = cadAcao.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioAcoes(usu, raiz);
		cadSum.gerarSumarioAcoesDetalhado(usu, raiz);	
	}
	
	public void gerarSumarioAcoesCalendario(Usuario usu) throws ErroModelo {
		getCadSumario().gerarSumarioAcoesCalendario(usu, getCadAcao().consultarTodos(usu));
	}
		
	public void gerarValidacaoAcoes(Usuario usu) throws ErroModelo {
		getCadSumario().gerarValidacaoAcoes(usu, getCadAcao().validarTodos(usu));
	}
	
	public void gerarSumarioReferencias(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadSumario();
		CadastroReferencia cadRef = getCadReferencia();
		Collection<Referencia> clRefs = cadRef.validarTodos(usu);
		cadSum.gerarSumarioReferencias(usu, clRefs);
		cadSum.gerarSumarioReferenciasDetalhado(usu, clRefs);
		cadSum.gerarValidacaoReferencias(usu, clRefs);	
		Grupo<Referencia> raiz = cadRef.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioReferencias(usu, raiz);
		cadSum.gerarSumarioReferenciasDetalhado(usu, raiz);	
	}

	public void gerarValidacaoReferencias(Usuario usu) throws ErroModelo {
		getCadSumario().gerarValidacaoReferencias(usu, getCadReferencia().validarTodos(usu));		
	}

	public void excluirSumariosInvalidos(Usuario usu) throws ErroModelo {
		getCadSumario().excluirSumariosInvalidos(usu);		
	}	
}
