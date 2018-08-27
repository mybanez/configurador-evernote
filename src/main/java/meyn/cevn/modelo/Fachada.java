package meyn.cevn.modelo;

import java.util.Collection;

import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.acao.CadastroAcao;
import meyn.cevn.modelo.interesse.CadastroInteresse;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.log.CadastroLog;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.CadastroReferencia;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.CadastroSumario;
import meyn.cevn.modelo.sumario.Sumario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.FachadaModeloImpl;

public class Fachada extends FachadaModeloImpl {

	@FunctionalInterface
	public static interface GeradorSumarios {
		void executar(Usuario usu) throws ErroModelo;
	}

	protected CadastroInteresse getCadastroInteresse() throws ErroModelo {
		return (CadastroInteresse) FachadaModeloImpl.<Usuario, Interesse>getCadastro(ChavesModelo.INTERESSE);
	}

	protected CadastroProjeto getCadastroProjeto() throws ErroModelo {
		return (CadastroProjeto) FachadaModeloImpl.<Usuario, Projeto>getCadastro(ChavesModelo.PROJETO);
	}

	protected CadastroAcao getCadastroAcao() throws ErroModelo {
		return (CadastroAcao) FachadaModeloImpl.<Usuario, Acao>getCadastro(ChavesModelo.ACAO);
	}

	protected CadastroReferencia getCadastroReferencia() throws ErroModelo {
		return (CadastroReferencia) FachadaModeloImpl.<Usuario, Referencia>getCadastro(ChavesModelo.REFERENCIA);
	}

	protected CadastroSumario getCadastroSumario() throws ErroModelo {
		return (CadastroSumario) Fachada.<Usuario, Sumario>getCadastro(ChavesModelo.SUMARIO);
	}

	protected CadastroLog getCadastroLog() throws ErroModelo {
		return (CadastroLog) Fachada.<Usuario, Nota>getCadastro(ChavesModelo.LOG);
	}
	
	@SuppressWarnings("rawtypes")
	public String getNomeRepositorio(String modelo) throws ErroModelo {
		return ((CadastroEvn) getCadastro(modelo)).getNomeRepositorio();
	}
	
	@SuppressWarnings("rawtypes")
	public void invalidarCaches(Usuario usu, String modelo) throws ErroModelo {
		((CadastroEvn) getCadastro(modelo)).invalidarCaches(usu);
	}

	//// SUMÁRIOS ////

	public void gerarSumarioInteresses(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadastroSumario();
		CadastroInteresse cadIntr = getCadastroInteresse();
		Collection<Interesse> clIntrs = cadIntr.consultarTodos(usu);
		cadSum.gerarSumarioInteresses(usu, clIntrs);
		cadSum.gerarSumarioInteressesDetalhado(usu, clIntrs);
		Grupo<Interesse> raiz = cadIntr.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioInteresses(usu, raiz);
		cadSum.gerarSumarioInteressesDetalhado(usu, raiz);
	}

	public void gerarSumarioInteresse(Usuario usu, String id) throws ErroModelo {
		getCadastroSumario().gerarSumarioInteresse(usu, getCadastroInteresse().consultarPorChavePrimaria(usu, id));
	}

	public void gerarSumarioProjetos(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadastroSumario();
		CadastroProjeto cadProj = getCadastroProjeto();
		Collection<Projeto> clProjs = cadProj.consultarTodos(usu);
		cadSum.gerarSumarioProjetos(usu, clProjs);
		cadSum.gerarSumarioProjetosDetalhado(usu, clProjs);
		Grupo<Projeto> raiz = cadProj.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioProjetos(usu, raiz);
		cadSum.gerarSumarioProjetosDetalhado(usu, raiz);
	}

	public void gerarSumarioProjeto(Usuario usu, String id) throws ErroModelo {
		getCadastroSumario().gerarSumarioProjeto(usu, getCadastroProjeto().consultarPorChavePrimaria(usu, id));
	}

	public void gerarSumarioAcoes(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadastroSumario();
		CadastroAcao cadAcao = getCadastroAcao();
		Collection<Acao> clAcoes = cadAcao.consultarTodos(usu);
		cadSum.gerarSumarioAcoes(usu, clAcoes);
		cadSum.gerarSumarioAcoesDetalhado(usu, clAcoes);
		cadSum.gerarSumarioAcoesCalendario(usu, clAcoes);
		Grupo<Acao> raiz = cadAcao.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioAcoes(usu, raiz);
		cadSum.gerarSumarioAcoesDetalhado(usu, raiz);
	}

	public void gerarSumarioAcoesCalendario(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarSumarioAcoesCalendario(usu, getCadastroAcao().consultarTodos(usu));
	}

	public void gerarSumarioReferencias(Usuario usu) throws ErroModelo {
		CadastroSumario cadSum = getCadastroSumario();
		CadastroReferencia cadRef = getCadastroReferencia();
		Collection<Referencia> clRefs = cadRef.consultarTodos(usu);
		cadSum.gerarSumarioReferencias(usu, clRefs);
		cadSum.gerarSumarioReferenciasDetalhado(usu, clRefs);
		Grupo<Referencia> raiz = cadRef.consultarTodosPorGrupos(usu);
		cadSum.gerarSumarioReferencias(usu, raiz);
		cadSum.gerarSumarioReferenciasDetalhado(usu, raiz);
	}

	public void excluirSumariosInvalidos(Usuario usu) throws ErroModelo {
		getCadastroSumario().excluirSumariosInvalidos(usu);
	}

	//// VALIDAÇÕES COMPLETAS ////

	public void gerarValidacaoProjetos(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoProjetos(usu, getCadastroProjeto().validarTodos(usu));
	}

	public void gerarValidacaoProjeto(Usuario usu, String id) throws ErroModelo {
		getCadastroSumario().gerarValidacaoProjeto(usu, getCadastroProjeto().validarPorChavePrimaria(usu, id));
	}

	public void gerarValidacaoAcoes(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoAcoes(usu, getCadastroAcao().validarTodos(usu));
	}

	public void gerarValidacaoReferencias(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoReferencias(usu, getCadastroReferencia().validarTodos(usu));
	}

	//// VALIDAÇÕES PARCIAIS (ASSUME ENTIDADES JÁ VALIDADAS) ////

	public void validarEntidades(Usuario usu) throws ErroModelo {
		CadastroEvn.validarEntidades(usu);
	}

	public void gerarValidacaoProjetosParcial(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoProjetos(usu, getCadastroProjeto().consultarTodos(usu));
	}

	public void gerarValidacaoProjetoParcial(Usuario usu, String id) throws ErroModelo {
		getCadastroSumario().gerarValidacaoProjeto(usu, getCadastroProjeto().consultarPorChavePrimaria(usu, id));
	}

	public void gerarValidacaoAcoesParcial(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoAcoes(usu, getCadastroAcao().consultarTodos(usu));
	}

	public void gerarValidacaoReferenciasParcial(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoReferencias(usu, getCadastroReferencia().consultarTodos(usu));
	}

	//// LOGS ////

	public Nota gerarLogUsuario(Usuario usu) throws ErroModelo {
		return getCadastroLog().gerarLogUsuario(usu);
	}
	
	public void excluirLogsAntigos(Usuario usu) throws ErroModelo {
		getCadastroLog().excluirLogsAntigos(usu);
	}
}
