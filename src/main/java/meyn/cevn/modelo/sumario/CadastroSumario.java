package meyn.cevn.modelo.sumario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import meyn.cevn.modelo.CadastroEvn;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.ClienteEvn;
import meyn.cevn.modelo.Grupo;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.OTEvn;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.ot.ErroPropriedadeOTNaoDefinida;
import meyn.util.modelo.ot.FabricaOT;

public class CadastroSumario extends CadastroNota<Sumario> {

	private static final String REPOSITORIO = "1. Sumários";

	private static final String TITULO_SUMARIO = "Sumário";
	private static final String TITULO_VALIDACAO = "Validação";
	private static final String TITULO_DETALHADO = " - Detalhado";
	private static final String TITULO_GRUPOS = " - Grupos";

	private static final String ESTILO_COR = " color: rgb(65, 173, 28);";
	private static final String ESTILO_TEXTO = "font-size: 14pt;";
	private static final String ESTILO_TEXTO_PEQUENO_1 = "font-size: 12pt;";
	private static final String ESTILO_TEXTO_PEQUENO_2 = "font-size: 10pt;";
	private static final String ESTILO_URL = ESTILO_TEXTO + ESTILO_COR;
	private static final String ESTILO_URL_PEQUENO_1 = ESTILO_TEXTO_PEQUENO_1 + ESTILO_COR;
	private static final String ESTILO_URL_PEQUENO_2 = ESTILO_TEXTO_PEQUENO_2 + ESTILO_COR;
	private static final String ESTILO_URL_TOPO = ESTILO_URL + " font-weight: bold;";
	private static final String ESTILO_URL_TOPO_PEQUENO = ESTILO_URL_PEQUENO_1 + " font-weight: bold;";
	private static final String ESTILO_LINHA = "text-align: center;";

	private static final String URL_SUMARIO = ClienteEvn.getURL() + "/faces/resultado_sumario.xhtml?sumario=";
	private static final String URL_VALIDACAO = URL_SUMARIO + "validacao_";

	private static final String EXP_TITULO = "(Sumário|Validação)(\\sdo\\s(Projeto|Interesse))?\\s-\\s(\\S.*)";

	public CadastroSumario() throws ErroCadastro {
		super(REPOSITORIO);
	}

	//// SUMÁRIOS DE INTERESSES ////

	private static final String TITULO_INTERESSE = " do Interesse - ";
	private static final String TITULO_INTERESSES = " - Interesses";

	private static final String URL_INTERESSE = "interesse&amp;id=";
	private static final String URL_INTERESSES = "interesses";

	private final GeradorENML<Interesse> grDetIntrVazio = (cont, proj) -> {
	};
	private final GeradorENML<Interesse> grDetIntr = (cont, intr) -> gerarDetalhamentoInteresse(cont, intr,
			ESTILO_TEXTO_PEQUENO_1, ESTILO_URL_PEQUENO_2);

	public Sumario consultarPorInteresse(Usuario usu, Interesse intr) throws ErroCadastro {
		return consultarPorNome(usu, TITULO_SUMARIO + TITULO_INTERESSE + intr.getNome());
	}

	public Sumario gerarSumarioInteresse(Usuario usu, Interesse intr) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_SUMARIO + URL_INTERESSE + intr.getId());
		gerarDetalhamentoInteresse(cont, intr, ESTILO_TEXTO, ESTILO_URL_PEQUENO_1);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + TITULO_INTERESSE + intr.getNome(), false, cont.toString());
	}

	public Sumario gerarSumarioInteresses(Usuario usu, Collection<Interesse> clIntrs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_INTERESSES, URL_INTERESSES, clIntrs, grDetIntrVazio);
	}

	public Sumario gerarSumarioInteressesDetalhado(Usuario usu, Collection<Interesse> clProjs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_INTERESSES + TITULO_DETALHADO, URL_INTERESSES, clProjs, grDetIntr);
	}

	public Sumario gerarSumarioInteresses(Usuario usu, Grupo<Interesse> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_INTERESSES, URL_INTERESSES, usu, raiz, grDetIntrVazio);
	}

	public Sumario gerarSumarioInteressesDetalhado(Usuario usu, Grupo<Interesse> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_INTERESSES + TITULO_DETALHADO, URL_INTERESSES, usu, raiz, grDetIntr);
	}

	private void gerarDetalhamentoInteresse(StringBuffer cont, Interesse intr, String estiloTitulo,
			String estiloItemURL) {
		GeradorENML<String> grTitulo = (cnt, tit) -> gerarItem(cnt, tit, estiloTitulo);
		gerarInicioLista(cont);
		// Projetos
		GeradorENML<Projeto> grProj = (cnt, proj) -> gerarItemURL(cnt, proj, estiloItemURL);
		gerarListaItens(cont, "Projetos", grTitulo, intr.getProjetos(), grProj);
		// Ações
		GeradorENML<Acao> grAcao = (cnt, acao) -> gerarItemURL(cnt, acao, estiloItemURL);
		gerarListaItens(cont, "Ações", grTitulo, intr.getAcoes(), grAcao);
		// Referências
		GeradorENML<Referencia> grRef = (cnt, ref) -> gerarItemURL(cnt, ref, estiloItemURL);
		gerarListaItens(cont, "Referências", grTitulo, intr.getReferencias(), grRef);
		gerarFimLista(cont);
	}

	//// SUMÁRIOS DE PROJETOS ////

	private static final String TITULO_PROJETO = " do Projeto - ";
	private static final String TITULO_PROJETOS = " - Projetos";

	private static final String URL_PROJETO = "projeto&amp;id=";
	private static final String URL_PROJETOS = "projetos";

	private static final String PREFIXO_URL_PAINEL_PROJETO = "<div style=\"text-align: center;\"><a href=\"";
	private static final String SUFIXO_URL_PAINEL_PROJETO = "\" style=\""
			+ ESTILO_URL_TOPO.replace("(", "\\(").replace(")", "\\)") + "\">SUMÁRIO</a></div><hr/>";
	private static final String PREFIXO_URL_VALIDACAO_PROJETO = "<div style=\"text-align: center;\"><a href=\"";
	private static final String SUFIXO_URL_VALIDACAO_PROJETO = "\" style=\""
			+ ESTILO_URL_TOPO_PEQUENO.replace("(", "\\(").replace(")", "\\)") + "\">Sumário</a></div><hr/>";

	private static final String EXP_URL_PAINEL_PROJETO = "(?s)(.*<en-note(?:(?![/>]).)*)(/)?(>\\s*)(?:("
			+ PREFIXO_URL_PAINEL_PROJETO + ")(.*)(" + SUFIXO_URL_PAINEL_PROJETO + "))?(.*)";
	private static final String EXP_URL_VALIDACAO_PROJETO = "(?s)((?:(?!<hr/>).)*<hr/>\\s*)(?:("
			+ PREFIXO_URL_VALIDACAO_PROJETO + ")(.*)(" + SUFIXO_URL_VALIDACAO_PROJETO + "))?(.*)";

	private final GeradorENML<Projeto> grDetProjVazio = (cont, proj) -> {
	};
	private final GeradorENML<Projeto> grDetProj = (cont, proj) -> gerarDetalhamentoProjeto(cont, proj,
			ESTILO_TEXTO_PEQUENO_1, ESTILO_URL_PEQUENO_1, ESTILO_TEXTO_PEQUENO_2, ESTILO_URL_PEQUENO_2);

	public Sumario consultarPorProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		return consultarPorNome(usu, TITULO_SUMARIO + TITULO_PROJETO + proj.getNome());
	}

	public Sumario gerarSumarioInicialProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		Sumario sum = gerarSumarioProjeto(usu, proj);
		// Atualizar url no painel do projeto
		CadastroProjeto cadProj = getCadastro(ChavesModelo.PROJETO);
		cadProj.carregarConteudo(usu, proj);
		String contProj = proj.getConteudo();
		if (contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$5").equals("")) {
			String sufixoPainel = contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$2").equals("") ? "" : "</en-note>";
			proj.setConteudo(contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$1$3" + PREFIXO_URL_PAINEL_PROJETO
					+ sum.getURL() + SUFIXO_URL_PAINEL_PROJETO + "$7" + sufixoPainel));
		} else {
			proj.setConteudo(contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$1$3$4" + sum.getURL() + "$6$7"));
		}
		cadProj.alterar(usu, proj);
		// Atualizar url na validação do projeto
		Sumario sumValid = proj.getSumarioValidacao();
		carregarConteudo(usu, sumValid);
		String contValid = sumValid.getConteudo();
		if (contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$3").equals("")) {
			sumValid.setConteudo(contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO,
					"$1" + PREFIXO_URL_VALIDACAO_PROJETO + sum.getURL() + SUFIXO_URL_VALIDACAO_PROJETO + "$5"));
		} else {
			sumValid.setConteudo(contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$1$2" + sum.getURL() + "$4$5"));
		}
		alterar(usu, sumValid);
		return sum;
	}

	public Sumario gerarSumarioProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_SUMARIO + URL_PROJETO + proj.getId());
		if (proj.getSumarioValidacao() != null) {
			gerarInicioLinha(cont, ESTILO_LINHA);
			gerarTextoURL(cont, "Validação", ESTILO_URL_TOPO_PEQUENO, proj.getSumarioValidacao().getURL());
			gerarFimLinha(cont);
			gerarQuebraSecao(cont);
		}
		gerarDetalhamentoProjeto(cont, proj, ESTILO_TEXTO, ESTILO_URL, ESTILO_TEXTO_PEQUENO_1, ESTILO_URL_PEQUENO_1);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + TITULO_PROJETO + proj.getNome(), false, cont.toString());
	}

	public Sumario gerarSumarioProjetos(Usuario usu, Collection<Projeto> clProjs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_PROJETOS, URL_PROJETOS, clProjs, grDetProjVazio);
	}

	public Sumario gerarSumarioProjetosDetalhado(Usuario usu, Collection<Projeto> clProjs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_PROJETOS + TITULO_DETALHADO, URL_PROJETOS, clProjs, grDetProj);
	}

	public Sumario gerarSumarioProjetos(Usuario usu, Grupo<Projeto> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_PROJETOS, URL_PROJETOS, usu, raiz, grDetProjVazio);
	}

	public Sumario gerarSumarioProjetosDetalhado(Usuario usu, Grupo<Projeto> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_PROJETOS + TITULO_DETALHADO, URL_PROJETOS, usu, raiz, grDetProj);
	}

	public void gerarDetalhamentoProjeto(StringBuffer cont, Projeto proj, String estiloTitulo, String estiloTituloURL,
			String estiloItem, String estiloItemURL) {
		gerarInicioLista(cont);
		// Painel
		gerarItemURL(cont, "Painel", estiloTituloURL, proj.getURL());
		// Interesses
		GeradorENML<String> grTitulo = (cnt, tit) -> gerarItem(cnt, tit, estiloTitulo);
		GeradorENML<Interesse> grIntr = (cnt, intr) -> gerarItemURL(cnt, intr, estiloItemURL);
		gerarListaItens(cont, "Interesses", grTitulo, proj.getInteresses(), grIntr);
		// Ações
		GeradorENML<Acao> grAcao = (cnt, acao) -> gerarItemURL(cnt, acao, estiloItemURL);
		Collection<Acao> clAcoesCalend = proj.getAcoesCalendario();
		if (!clAcoesCalend.isEmpty()) {
			gerarItem(cont, "Ações no Calendário", estiloTitulo);
			gerarDetalhamentoCalendario(cont, estiloTitulo, estiloItem, estiloItem, estiloItemURL, clAcoesCalend);
		}
		gerarListaItens(cont, "Ações em Foco", grTitulo, proj.getAcoesEmFoco(), grAcao);
		gerarListaItens(cont, "Ações Delegadas", grTitulo, proj.getAcoesDelegadas(), grAcao);
		gerarListaItens(cont, "Ações Futuras", grTitulo, proj.getAcoesProximas(), grAcao);
		// Referências
		GeradorENML<Referencia> grRef = (cnt, ref) -> gerarItemURL(cnt, ref, estiloItemURL);
		gerarListaItens(cont, "Referências", grTitulo, proj.getReferencias(), grRef);
		gerarFimLista(cont);
	}

	public Sumario consultarValidacaoPorProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		return consultarPorNome(usu, TITULO_VALIDACAO + TITULO_PROJETO + proj.getNome());
	}

	public Sumario gerarValidacaoProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_VALIDACAO + URL_PROJETO + proj.getId());
		if (proj.getSumario() != null) {
			gerarInicioLinha(cont, ESTILO_LINHA);
			gerarTextoURL(cont, "Sumário", ESTILO_URL_TOPO_PEQUENO, proj.getSumario().getURL());
			gerarFimLinha(cont);
			gerarQuebraSecao(cont);
		}
		gerarInicioLista(cont);
		boolean temMensagem = false;
		Collection<String> clMsgs = proj.getMensagensValidacao();
		// Testa se há validação em andamento
		if (clMsgs != null) {
			// Painel
			GeradorENML<String> grTitulo = (cnt, tit) -> gerarTexto(cnt, tit, ESTILO_TEXTO);
			gerarListaItens(cont, "Painel", grTitulo, clMsgs, grMsgValid);
			temMensagem |= !clMsgs.isEmpty();
			// Ações
			GeradorENML<Acao> grTitAcao = (cnt, acao) -> gerarItemURL(cnt, acao, ESTILO_URL_PEQUENO_1);
			GeradorENML<Acao> grAcao = (cnt, acao) -> gerarListaItens(cnt, acao, grTitAcao,
					acao.getMensagensValidacao(), grMsgValidPeq);
			Collection<Acao> clAcoes = new ArrayList<Acao>(proj.getAcoesCalendario());
			clAcoes.addAll(proj.getAcoesEmFoco());
			clAcoes.addAll(proj.getAcoesDelegadas());
			clAcoes.addAll(proj.getAcoesProximas());
			clAcoes.removeIf((Nota nota) -> nota.getMensagensValidacao().isEmpty());
			gerarListaItens(cont, "Ações", grTitulo, clAcoes, grAcao);
			temMensagem |= !clAcoes.isEmpty();
			// Referências
			GeradorENML<Referencia> grTitRef = (cnt, ref) -> gerarItemURL(cnt, ref, ESTILO_URL_PEQUENO_1);
			GeradorENML<Referencia> grRef = (cnt, ref) -> gerarListaItens(cnt, ref, grTitRef,
					ref.getMensagensValidacao(), grMsgValidPeq);
			Collection<Referencia> clRefs = proj.getReferencias();
			clRefs.removeIf((Nota nota) -> nota.getMensagensValidacao().isEmpty());
			gerarListaItens(cont, "Referências", grTitulo, clRefs, grRef);
			temMensagem |= !clRefs.isEmpty();
		}
		if (!temMensagem) {
			gerarItem(cont, "Nenhum problema encontrado!", ESTILO_TEXTO);
		}
		gerarFimLista(cont);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_VALIDACAO + TITULO_PROJETO + proj.getNome(), false, cont.toString());
	}

	public Sumario gerarValidacaoProjetos(Usuario usu, Collection<Projeto> clProjs) throws ErroCadastro {
		return gerarValidacao(usu, TITULO_PROJETOS, URL_PROJETOS, clProjs);
	}

	//// SUMÁRIOS DE AÇÕES ////

	private static final String TITULO_ACOES = " - Ações";
	private static final String TITULO_CALENDARIO = " - Calendário";

	private static final String URL_ACOES = "acoes";
	private static final String URL_CALENDARIO = "_calendario";

	private static final SimpleDateFormat FORMATO_DATA_ANO = new SimpleDateFormat("yyyy");
	private static final SimpleDateFormat FORMATO_DATA_MES = new SimpleDateFormat("MMMM");
	private static final SimpleDateFormat FORMATO_DATA_DIA = new SimpleDateFormat("EEE, dd");
	private static final SimpleDateFormat FORMATO_DATA_HORA = new SimpleDateFormat("HH:mm - ");

	private final GeradorENML<Acao> grDetAcaoVazio = (cont, acao) -> {
	};
	private final GeradorENML<Acao> grDetAcao = (cont, acao) -> gerarDetalhamentoAcao(cont, acao,
			ESTILO_TEXTO_PEQUENO_1, ESTILO_URL_PEQUENO_1);

	public Sumario gerarSumarioAcoes(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		return gerarSumario(usu, TITULO_ACOES, URL_ACOES, clAcoes, grDetAcaoVazio);
	}

	public Sumario gerarSumarioAcoesDetalhado(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		return gerarSumario(usu, TITULO_ACOES + TITULO_DETALHADO, URL_ACOES, clAcoes, grDetAcao);
	}

	public Sumario gerarSumarioAcoes(Usuario usu, Grupo<Acao> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_ACOES, URL_ACOES, usu, raiz, grDetAcaoVazio);
	}

	public Sumario gerarSumarioAcoesDetalhado(Usuario usu, Grupo<Acao> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_ACOES + TITULO_DETALHADO, URL_ACOES, usu, raiz, grDetAcao);
	}

	public Sumario gerarSumarioAcoesCalendario(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_SUMARIO + URL_ACOES + URL_CALENDARIO);
		gerarDetalhamentoCalendario(cont, ESTILO_TEXTO, ESTILO_TEXTO_PEQUENO_1, ESTILO_TEXTO_PEQUENO_2,
				ESTILO_URL_PEQUENO_2, clAcoes);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + TITULO_ACOES + TITULO_CALENDARIO, true, cont.toString());
	}

	public void gerarDetalhamentoCalendario(StringBuffer cont, String estiloTitulo, String estiloItemInterm,
			String estiloItem, String estiloURLItem, Collection<Acao> clAcoes) {
		clAcoes.removeIf((acao) -> acao.getDataLembrete() == null);
		List<Acao> lsAcoes = new ArrayList<Acao>(clAcoes);
		Collections.sort(lsAcoes, (a, b) -> a.getDataLembrete().compareTo(b.getDataLembrete()));
		gerarInicioLista(cont);
		int ano, anoAnt = -1, mes, mesAnt = -1, dia, diaAnt = -1;
		Calendar cal = Calendar.getInstance();
		for (Acao acao : lsAcoes) {
			Date dt = acao.getDataLembrete();
			cal.setTime(dt);
			ano = cal.get(Calendar.YEAR);
			mes = cal.get(Calendar.MONTH);
			dia = cal.get(Calendar.DAY_OF_MONTH);
			if (ano != anoAnt) {
				if (anoAnt != -1) {
					gerarFimLista(cont);
					gerarFimLista(cont);
					gerarFimLista(cont);
				}
				gerarItem(cont, FORMATO_DATA_ANO.format(dt), estiloTitulo);
				gerarInicioLista(cont);
				mesAnt = -1;
			}
			if (mes != mesAnt) {
				if (ano == anoAnt) {
					gerarFimLista(cont);
					gerarFimLista(cont);
				}
				gerarItem(cont, FORMATO_DATA_MES.format(dt), estiloItemInterm);
				gerarInicioLista(cont);
				diaAnt = -1;
			}
			if (dia != diaAnt) {
				if (mes == mesAnt) {
					gerarFimLista(cont);
				}
				gerarItem(cont, FORMATO_DATA_DIA.format(dt), estiloItemInterm);
				gerarInicioLista(cont);
			}
			gerarInicioItem(cont);
			gerarTexto(cont, FORMATO_DATA_HORA.format(dt), estiloItem);
			gerarTextoURL(cont, acao.getNome(), estiloURLItem, acao.getURL());
			gerarFimItem(cont);
			anoAnt = ano;
			mesAnt = mes;
			diaAnt = dia;
		}
		gerarFimLista(cont);
		gerarFimLista(cont);
		gerarFimLista(cont);
		gerarFimLista(cont);
	}

	private void gerarDetalhamentoAcao(StringBuffer cont, Acao acao, String estiloTitulo, String estiloItemURL) {
		gerarInicioLista(cont);
		// Interesses
		GeradorENML<String> grTitulo = (cnt, tit) -> gerarItem(cnt, tit, estiloTitulo);
		GeradorENML<Interesse> grIntr = (cnt, intr) -> gerarItemURL(cnt, intr, estiloItemURL);
		Interesse empr = acao.getEmpregador();
		gerarListaItens(cont, "Interesses", grTitulo, empr != null ? Arrays.asList(empr) : Collections.emptyList(), grIntr);
		// Projetos
		GeradorENML<Projeto> grProj = (cnt, proj) -> gerarItemURL(cnt, proj, estiloItemURL);
		gerarListaItens(cont, "Projetos", grTitulo, acao.getProjetos(), grProj);
		gerarFimLista(cont);
	}

	public Sumario gerarValidacaoAcoes(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		return gerarValidacao(usu, TITULO_ACOES, URL_ACOES, clAcoes);
	}

	//// SUMÁRIOS DE REFERÊNCIAS ////

	private static final String TITULO_REFERENCIAS = " - Referências";
	private static final String URL_REFERENCIAS = "referencias";

	private final GeradorENML<Referencia> grDetRefVazio = (cont, ref) -> {
	};
	private final GeradorENML<Referencia> grDetRef = (cont, acao) -> gerarDetalhamentoReferencia(cont, acao,
			ESTILO_TEXTO_PEQUENO_1, ESTILO_URL_PEQUENO_1);

	public Sumario gerarSumarioReferencias(Usuario usu, Collection<Referencia> clRefs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_REFERENCIAS, URL_REFERENCIAS, clRefs, grDetRefVazio);
	}

	public Sumario gerarSumarioReferenciasDetalhado(Usuario usu, Collection<Referencia> clRefs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_REFERENCIAS + TITULO_DETALHADO, URL_REFERENCIAS, clRefs, grDetRef);
	}

	public Sumario gerarSumarioReferencias(Usuario usu, Grupo<Referencia> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_REFERENCIAS, URL_REFERENCIAS, usu, raiz, grDetRefVazio);
	}

	public Sumario gerarSumarioReferenciasDetalhado(Usuario usu, Grupo<Referencia> raiz) throws ErroCadastro {
		return gerarSumario(TITULO_REFERENCIAS + TITULO_DETALHADO, URL_REFERENCIAS, usu, raiz, grDetRef);
	}

	private void gerarDetalhamentoReferencia(StringBuffer cont, Referencia ref, String estiloTitulo,
			String estiloItemURL) {
		gerarInicioLista(cont);
		// Interesses
		GeradorENML<String> grTitulo = (cnt, tit) -> gerarItem(cnt, tit, estiloTitulo);
		GeradorENML<Interesse> grIntr = (cnt, intr) -> gerarItemURL(cnt, intr, estiloItemURL);
		gerarListaItens(cont, "Interesses", grTitulo, ref.getInteresses(), grIntr);
		// Projetos
		GeradorENML<Projeto> grProj = (cnt, proj) -> gerarItemURL(cnt, proj, estiloItemURL);
		gerarListaItens(cont, "Projetos", grTitulo, ref.getProjetos(), grProj);
		gerarFimLista(cont);
	}

	public Sumario gerarValidacaoReferencias(Usuario usu, Collection<Referencia> clRefs) throws ErroCadastro {
		return gerarValidacao(usu, TITULO_REFERENCIAS, URL_REFERENCIAS, clRefs);
	}

	//// SUMÁRIOS GENÉRICOS ////

	public <TipoOT extends OTEvn<?>> Sumario gerarSumario(Usuario usu, String titulo, String urlAtualizar,
			Collection<TipoOT> clOTs, GeradorENML<TipoOT> geradorDetalhamentoOT) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_SUMARIO + urlAtualizar);
		try {
			gerarUrlsSumario(usu, cont, titulo);
		} catch (ErroItemNaoEncontrado e) {
		}
		gerarInicioLista(cont);
		for (TipoOT ot : clOTs) {
			gerarItemURL(cont, ot, ESTILO_URL);
			geradorDetalhamentoOT.executar(cont, ot);
		}
		gerarFimLista(cont);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + titulo, !titulo.contains(TITULO_DETALHADO), cont.toString());
	}

	public <TipoOT extends OTEvn<?>> Sumario gerarSumario(String titulo, String urlAtualizar, Usuario usu,
			Grupo<TipoOT> raiz, GeradorENML<TipoOT> geradorDetalhamentoOT) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_SUMARIO + urlAtualizar);
		try {
			gerarUrlsSumario(usu, cont, titulo);
		} catch (ErroItemNaoEncontrado e) {
		}
		gerarInicioLista(cont);
		gerarItem(cont, raiz.getNome(), ESTILO_TEXTO);
		gerarDetalhamentoGrupo(cont, raiz, geradorDetalhamentoOT);
		gerarFimLista(cont);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + titulo + TITULO_GRUPOS, false, cont.toString());
	}

	private <TipoOT extends OTEvn<?>> void gerarDetalhamentoGrupo(StringBuffer cont, Grupo<TipoOT> raiz,
			GeradorENML<TipoOT> geradorDetalhamentoOT) {
		gerarInicioLista(cont);
		TipoOT otGrp;
		for (Grupo<TipoOT> grupo : raiz.getGruposFilho()) {
			gerarInicioItem(cont);
			otGrp = grupo.getOT();
			if (otGrp != null) {
				gerarItemURL(cont, otGrp, ESTILO_URL);
			} else {
				gerarItem(cont, grupo.getNome(), ESTILO_TEXTO);
			}
			gerarFimItem(cont);
			gerarDetalhamentoGrupo(cont, grupo, geradorDetalhamentoOT);
		}
		for (TipoOT ot : raiz.getOtsFilho()) {
			gerarItemURL(cont, ot, ESTILO_URL_PEQUENO_1);
			geradorDetalhamentoOT.executar(cont, ot);
		}
		gerarFimLista(cont);
		otGrp = raiz.getOT();
		if (otGrp != null) {
			geradorDetalhamentoOT.executar(cont, otGrp);
		}
	}

	private void gerarUrlsSumario(Usuario usu, StringBuffer cont, String tituloBase) throws ErroCadastro {
		tituloBase = tituloBase.split(" - ")[1];

		String titulo = TITULO_SUMARIO + " - " + tituloBase;
		String urlTitulo = consultarPorNome(usu, titulo).getURL();
		String urlTituloDet = consultarPorNome(usu, titulo + TITULO_DETALHADO).getURL();
		String urlGrupos = consultarPorNome(usu, titulo + TITULO_GRUPOS).getURL();
		String urlGruposDet = consultarPorNome(usu, titulo + TITULO_DETALHADO + TITULO_GRUPOS).getURL();
		String urlValid = null;

		gerarInicioLinha(cont, ESTILO_LINHA);
		gerarTextoURL(cont, "Título", ESTILO_URL_TOPO_PEQUENO, urlTitulo);
		cont.append(" - ");
		gerarTextoURL(cont, "Título (detalhado)", ESTILO_URL_TOPO_PEQUENO, urlTituloDet);
		cont.append(" - ");
		gerarTextoURL(cont, "Grupo", ESTILO_URL_TOPO_PEQUENO, urlGrupos);
		cont.append(" - ");
		gerarTextoURL(cont, "Grupo (detalhado)", ESTILO_URL_TOPO_PEQUENO, urlGruposDet);
		try {
			urlValid = consultarPorNome(usu, TITULO_VALIDACAO + " - " + tituloBase).getURL();
		} catch (ErroItemNaoEncontrado e) {
		}
		if (urlValid != null) {
			cont.append(" - ");
			gerarTextoURL(cont, "Validação", ESTILO_URL_TOPO_PEQUENO, urlValid);
		}
		gerarFimLinha(cont);
		gerarQuebraSecao(cont);
	}

	//// SUMÁRIO DE VALIDAÇÃO GENÉRICO ////

	private final GeradorENML<Nota> grTitNota = (cnt, nota) -> gerarItemURL(cnt, nota, ESTILO_URL);
	private final GeradorENML<String> grMsgValid = (cnt, msg) -> gerarItemValid(cnt, msg, ESTILO_TEXTO_PEQUENO_1);
	private final GeradorENML<String> grMsgValidPeq = (cnt, msg) -> gerarItemValid(cnt, msg, ESTILO_TEXTO_PEQUENO_2);

	private Sumario gerarValidacao(Usuario usu, String titulo, String urlAtualizar, Collection<? extends Nota> clNotas)
			throws ErroCadastro {
		clNotas.removeIf((Nota nota) -> nota.getMensagensValidacao().isEmpty());
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, URL_VALIDACAO + urlAtualizar);
		try {
			gerarUrlsSumario(usu, cont, titulo);
		} catch (ErroItemNaoEncontrado e) {
		}
		gerarInicioLista(cont);
		if (!clNotas.isEmpty()) {
			for (Nota nota : clNotas) {
				gerarListaItens(cont, nota, grTitNota, nota.getMensagensValidacao(), grMsgValid);
			}
		} else {
			gerarItem(cont, "Nenhum problema encontrado!", ESTILO_TEXTO);
		}
		gerarFimLista(cont);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_VALIDACAO + titulo, false, cont.toString());
	}

	//// GERAÇÃO DE ENML ////

	private static final SimpleDateFormat FORMATO_DATA_ATUALIZADO = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

	protected void gerarCabecalho(StringBuffer cont, String urlAtualizar) {
		gerarCabecalho(cont);
		gerarInicioLinha(cont, ESTILO_LINHA);
		gerarTextoURL(cont, "ATUALIZAR", ESTILO_URL_TOPO, urlAtualizar);
		gerarFimLinha(cont);
		gerarInicioLinha(cont, ESTILO_LINHA);
		gerarTexto(cont, "(Atualizado em: " + FORMATO_DATA_ATUALIZADO.format(new Date()) + ")", ESTILO_TEXTO_PEQUENO_1);
		gerarFimLinha(cont);
		gerarQuebraSecao(cont);
	}

	@Override
	protected void gerarRodape(StringBuffer cont) {
		gerarQuebraSecao(cont);
		gerarQuebraLinha(cont);
		super.gerarRodape(cont);
	}

	private <TipoOT extends OTEvn<?>> void gerarItemURL(StringBuffer cont, TipoOT item, String estiloTexto) {
		String url;
		try {
			Sumario sum = (Sumario) item.get("sumario");
			url = sum != null ? sum.getURL() : ClienteEvn.getURL();
		} catch (ErroPropriedadeOTNaoDefinida e) {
			url = ((Nota) item).getURL();
		}
		gerarItemURL(cont, item.getNome(), estiloTexto, url);
	}

	private void gerarItemValid(StringBuffer cont, String texto, String estiloTexto) {
		gerarInicioItem(cont);
		gerarTextoMultilinha(cont, texto, estiloTexto);
		gerarFimItem(cont);
	}

	//// MANUTENÇÃO DAS ENTIDADES ////

	private Sumario gerarSumario(Usuario usu, String nome, boolean ehLembrete, String conteudo) throws ErroCadastro {
		Sumario sum;
		try {
			sum = consultarPorNome(usu, nome);
			sum.setConteudo(conteudo);
			alterar(usu, sum);
		} catch (ErroItemNaoEncontrado e) {
			sum = FabricaOT.getInstancia(Sumario.class);
			sum.setNome(nome);
			sum.setLembrete(ehLembrete);
			sum.setConteudo(conteudo);
			incluir(usu, sum);
		}
		return sum;
	}

	public void excluirSumariosInvalidos(Usuario usu) throws ErroCadastro {
		Collection<Sumario> clSums = consultarTodos(usu);
		for (Sumario sum : clSums) {
			String modelo = sum.getNome().replaceFirst(EXP_TITULO, "$3");
			String chave = sum.getNome().replaceFirst(EXP_TITULO, "$4");
			// Testa se é sumário de item e se o nome é válido
			if (!modelo.equals("") && !modelo.equals(sum.getNome())) {
				CadastroEvn<?, ?> cad = getCadastro(ChavesModelo.NOME_PACOTE + modelo.toUpperCase());
				try {
					cad.consultarPorNome(usu, chave);
				} catch (ErroItemNaoEncontrado e) {
					excluir(usu, sum);
				}
			}
		}
	}
}
