package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collection;

import com.evernote.edam.type.Notebook;

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

	protected CadastroSumario getCadastroSumario() throws ErroModelo {
		return (CadastroSumario) Fachada.<Usuario, Sumario>getCadastro(ChavesModelo.SUMARIO);
	}

	protected CadastroInteresse getCadastroInteresse() throws ErroModelo {
		return (CadastroInteresse) Fachada.<Usuario, Interesse>getCadastro(ChavesModelo.INTERESSE);
	}

	protected CadastroProjeto getCadastroProjeto() throws ErroModelo {
		return (CadastroProjeto) Fachada.<Usuario, Projeto>getCadastro(ChavesModelo.PROJETO);
	}

	protected CadastroAcao getCadastroAcao() throws ErroModelo {
		return (CadastroAcao) Fachada.<Usuario, Acao>getCadastro(ChavesModelo.ACAO);
	}

	protected CadastroReferencia getCadastroReferencia() throws ErroModelo {
		return (CadastroReferencia) Fachada.<Usuario, Referencia>getCadastro(ChavesModelo.REFERENCIA);
	}

	protected CadastroLog getCadastroLog() throws ErroModelo {
		return (CadastroLog) Fachada.<Usuario, Nota>getCadastro(ChavesModelo.LOG);
	}

	public void conectar(Usuario usu) throws ErroModelo {
		if (!ClienteEvn.isConectado(usu)) {
			ClienteEvn.conectar(usu);
			// Para evitar duplicações, só gera log para novas conexões
			usu.setLog(getCadastroLog().gerarLogUsuario(usu));
		}
	}

	public int consultarTamanhoFilaServidor(Usuario usu) {
		return ClienteEvn.getTamanhoFilaServidor(usu);
	}

	public boolean verificarAtualizacoesServidor(Usuario usu) throws ErroModelo {
		CacheTags cacheTag = CacheTags.getCache(usu);
		CacheNotebooks cacheNtb = CacheNotebooks.getCache(usu);

		Collection<String> clRepos = new ArrayList<String>();
		clRepos.add(getCadastroSumario().getNomeRepositorio());
		clRepos.add(getCadastroProjeto().getNomeRepositorio());
		clRepos.add(getCadastroAcao().getNomeRepositorio());
		clRepos.add(getCadastroReferencia().getNomeRepositorio());
		clRepos.add(getCadastroLog().getNomeRepositorio());

		Collection<String> clIds = new ArrayList<String>();
		for (String nomeRepo : clRepos) {
			Notebook ntb = cacheNtb.get(nomeRepo);
			if (ntb != null) {
				clIds.add(ntb.getGuid());
			} else {
				for (Notebook ntbPilha : cacheNtb.consultarPorPilha(nomeRepo)) {
					clIds.add(ntbPilha.getGuid());
				}
			}
		}

		Collection<String> clIdsAtu = ClienteEvn.consultarAtualizacoes(usu, clIds);
		if (clIdsAtu.size() > 0) {
			if (clIdsAtu.contains(ClienteEvn.TAGS)) {
				clIdsAtu.remove(ClienteEvn.TAGS);
				cacheTag.desatualizar();
				desatualizarCaches(usu);
				logger.debug("atualizar Tags");
			}
			if (clIdsAtu.contains(ClienteEvn.NOTEBOOKS)) {
				clIdsAtu.remove(ClienteEvn.NOTEBOOKS);
				cacheNtb.desatualizar();
				desatualizarCaches(usu);
				logger.debug("atualizar Notebooks");
			}
			String idLog = cacheNtb.get(getCadastroLog().getNomeRepositorio()).getGuid();
			if (clIdsAtu.contains(idLog)) {
				clIdsAtu.remove(idLog);
				getCadastroLog().desatualizarCache(usu);
				logger.debug("atualizar Logs");
			}
			if (clIdsAtu.size() > 0) {
				desatualizarCaches(usu);
				logger.debug("atualizar Entidades");
			}
			return true;
		}
		return false;
	}

	public void desatualizarCaches(Usuario usu) throws ErroModelo {
		CadastroEvn.desatualizarCaches(usu);
	}

	//// SUMÁRIOS ////

	public Collection<Sumario> consultarSumariosInvalidos(Usuario usu) throws ErroModelo {
		return getCadastroSumario().consultarSumariosInvalidos(usu);
	}

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

	public Sumario gerarSumarioInteresse(Usuario usu, String id) throws ErroModelo {
		return getCadastroSumario().gerarSumarioInteresse(usu, getCadastroInteresse().consultarPorChavePrimaria(usu, id));
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

	public Sumario gerarSumarioInicialProjeto(Usuario usu, String id) throws ErroModelo {
		return getCadastroSumario().gerarSumarioInicialProjeto(usu, getCadastroProjeto().consultarPorChavePrimaria(usu, id));
	}

	public Sumario gerarSumarioProjeto(Usuario usu, String id) throws ErroModelo {
		return getCadastroSumario().gerarSumarioProjeto(usu, getCadastroProjeto().consultarPorChavePrimaria(usu, id));
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

	//// VALIDAÇÕES COMPLETAS ////

	public void gerarValidacaoProjetos(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoProjetos(usu, getCadastroProjeto().validarTodos(usu));
	}

	public Sumario gerarValidacaoProjeto(Usuario usu, String id) throws ErroModelo {
		return getCadastroSumario().gerarValidacaoProjeto(usu, getCadastroProjeto().validarPorChavePrimaria(usu, id));
	}

	public void gerarValidacaoAcoes(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoAcoes(usu, getCadastroAcao().validarTodos(usu));
	}

	public void gerarValidacaoReferencias(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoReferencias(usu, getCadastroReferencia().validarTodos(usu));
	}

	//// VALIDAÇÕES PARCIAIS (ASSUME ENTIDADES JÁ VALIDADAS) ////

	public void desatualizarCachesParaValidacao(Usuario usu) throws ErroModelo {
		CadastroEvn.desatualizarCachesParaValidacao(usu);
	}

	public void gerarValidacaoParcialProjetos(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoProjetos(usu, getCadastroProjeto().consultarTodos(usu));
	}

	public Sumario gerarValidacaoParcialProjeto(Usuario usu, String id) throws ErroModelo {
		return getCadastroSumario().gerarValidacaoProjeto(usu, getCadastroProjeto().consultarPorChavePrimaria(usu, id));
	}

	public void gerarValidacaoParcialAcoes(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoAcoes(usu, getCadastroAcao().consultarTodos(usu));
	}

	public void gerarValidacaoParcialReferencias(Usuario usu) throws ErroModelo {
		getCadastroSumario().gerarValidacaoReferencias(usu, getCadastroReferencia().consultarTodos(usu));
	}

	//// LOGS ////

	public void desativarServicoLog(Usuario usu) {
		CadastroLog.desativarServico(usu);
	}

	public Nota consultarLogPorNome(Usuario usu, String nome) throws ErroModelo {
		return getCadastroLog().consultarPorNome(usu, nome);
	}

	public void excluirLogsAntigos(Usuario usu) throws ErroModelo {
		getCadastroLog().excluirLogsAntigos(usu);
	}
}
