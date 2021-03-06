package meyn.cevn.modelo.sumario;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.Logger;

import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.CadastroEvn;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.EntidadeEvn;
import meyn.cevn.modelo.Grupo;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.Usuario;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.util.FusoHorario;
import meyn.util.modelo.Modelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.entidade.ErroPropriedadeEntidadeNaoDefinida;
import meyn.util.modelo.entidade.FabricaEntidade;

@Modelo(ChavesModelo.SUMARIO)
public class CadastroSumario extends CadastroNota<Sumario> {

	public static final int QTD_SUMS_COLETIVOS = 20;
	public static final String OP_VALIDACAO = "validacao_";

	private static final String REPOSITORIO = "1. Sumários";

	private static final String TITULO_SUMARIO = "Sumário";
	private static final String TITULO_VALIDACAO = "Validação";
	private static final String TITULO_DETALHADO = " - Detalhado";
	private static final String TITULO_GRUPOS = " - Grupos";

	private static final String EXP_TITULO = "(Sumário|Validação)(\\sdo\\s(Projeto|Interesse))?\\s-\\s(\\S.*)";

	private static final String URL_SUMARIO = "sumario=";
	private static final String URL_VALIDACAO = URL_SUMARIO + OP_VALIDACAO;
	private static final String URL_ID = "&amp;id=";

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

	public CadastroSumario() throws ErroCadastro {
		super(REPOSITORIO, false, false);
	}

	//// SUMÁRIOS DE INTERESSES ////

	public static final String OP_INTERESSE = "interesse";
	public static final String OP_INTERESSES = "interesses";

	private static final String TITULO_INTERESSE = " do Interesse - ";
	private static final String TITULO_INTERESSES = " - Interesses";

	private final GeradorENML<Interesse> grDetIntrVazio = (cont, proj) -> {
	};
	private final GeradorENML<Interesse> grDetIntr = (cont, intr) -> gerarDetalhamentoInteresse(cont, intr, ESTILO_TEXTO_PEQUENO_1,
			ESTILO_TEXTO_PEQUENO_2, ESTILO_URL_PEQUENO_2);

	public Sumario consultarPorInteresse(Usuario usu, Interesse intr) throws ErroCadastro {
		return consultarPorNome(usu, TITULO_SUMARIO + TITULO_INTERESSE + intr.getNome());
	}

	public Sumario gerarSumarioInteresse(Usuario usu, Interesse intr) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_SUMARIO + OP_INTERESSE + URL_ID + intr.getId());
		gerarDetalhamentoInteresse(cont, intr, ESTILO_TEXTO, ESTILO_TEXTO_PEQUENO_1,  ESTILO_URL_PEQUENO_1);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + TITULO_INTERESSE + intr.getNome(), false, cont.toString());
	}

	public Sumario gerarSumarioInteresses(Usuario usu, Collection<Interesse> clIntrs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_INTERESSES, OP_INTERESSES, clIntrs, grDetIntrVazio);
	}

	public Sumario gerarSumarioInteressesDetalhado(Usuario usu, Collection<Interesse> clProjs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_INTERESSES + TITULO_DETALHADO, OP_INTERESSES, clProjs, grDetIntr);
	}

	public Sumario gerarSumarioInteresses(Usuario usu, Grupo<Interesse> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_INTERESSES, OP_INTERESSES, raiz, grDetIntrVazio);
	}

	public Sumario gerarSumarioInteressesDetalhado(Usuario usu, Grupo<Interesse> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_INTERESSES + TITULO_DETALHADO, OP_INTERESSES, raiz, grDetIntr);
	}

	private void gerarDetalhamentoInteresse(StringBuffer cont, Interesse intr, String estiloTitulo, String estiloItem, String estiloItemURL) {
		GeradorENML<String> grTitulo = (cnt, tit) -> gerarItem(cnt, tit, estiloTitulo);
		gerarInicioLista(cont);
		// Projetos
		GeradorENML<Projeto> grProj = (cnt, proj) -> gerarItemURL(cnt, proj, estiloItemURL);
		gerarListaItens(cont, "Projetos", grTitulo, intr.getProjetos(), grProj);
		// Ações
		GeradorENML<Acao> grAcao = (cnt, acao) -> gerarItemURL(cnt, acao, estiloItemURL);
		gerarListaItens(cont, "Ações", grTitulo, intr.getAcoes(), grAcao);
		// Referências
		Grupo<Referencia> grRefs = intr.getReferenciasPorFormato();
		if (!grRefs.isVazio()) {
			gerarItem(cont, "Referências", estiloTitulo);
			gerarDetalhamentoGrupo(cont, grRefs, estiloItem, estiloItemURL, grDetRefVazio);
		}
		gerarFimLista(cont);
	}

	//// SUMÁRIOS DE PROJETOS ////

	public static final String OP_PROJETO = "projeto";
	public static final String OP_PROJETOS = "projetos";

	private static final String TITULO_PROJETO = " do Projeto - ";
	private static final String TITULO_PROJETOS = " - Projetos";

	private static final String PREFIXO_URL_PAINEL_PROJETO = "<div style=\"text-align: center;\"><a href=\"";
	private static final String SUFIXO_URL_PAINEL_PROJETO = "\" style=\"" + ESTILO_URL_TOPO.replace("(", "\\(").replace(")", "\\)")
	        + "\">SUMÁRIO</a></div><hr/>";
	private static final String PREFIXO_URL_VALIDACAO_PROJETO = "<div style=\"text-align: center;\"><a href=\"";
	private static final String SUFIXO_URL_VALIDACAO_PROJETO = "\" style=\""
	        + ESTILO_URL_TOPO_PEQUENO.replace("(", "\\(").replace(")", "\\)") + "\">Sumário</a></div><hr/>";

	private static final String EXP_URL_PAINEL_PROJETO = "(?s)(.*<en-note(?:(?![/>]).)*)(/)?(>)(((?:(?!" + PREFIXO_URL_PAINEL_PROJETO
	        + ").)*)(" + PREFIXO_URL_PAINEL_PROJETO + ")((?:(?!" + SUFIXO_URL_PAINEL_PROJETO + ").)*)(" + SUFIXO_URL_PAINEL_PROJETO
	        + "))?(.*)";
	private static final String EXP_URL_VALIDACAO_PROJETO = "(?s)((?:(?!<hr/>).)*<hr/>)(((?:(?!" + PREFIXO_URL_VALIDACAO_PROJETO
	        + ").)*)(" + PREFIXO_URL_VALIDACAO_PROJETO + ")((?:(?!" + SUFIXO_URL_VALIDACAO_PROJETO + ").)*)("
	        + SUFIXO_URL_VALIDACAO_PROJETO + "))?(.*)";

	private final GeradorENML<Projeto> grDetProjVazio = (cont, proj) -> {
	};
	private final GeradorENML<Projeto> grDetProj = (cont, proj) -> gerarDetalhamentoProjeto(cont, proj, ESTILO_TEXTO_PEQUENO_1,
	        ESTILO_URL_PEQUENO_1, ESTILO_TEXTO_PEQUENO_2, ESTILO_URL_PEQUENO_2);

	public Sumario consultarPorProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		return consultarPorNome(usu, TITULO_SUMARIO + TITULO_PROJETO + proj.getNome());
	}

	public Sumario gerarSumarioInicialProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		Logger logger = getLogger();
		Sumario sum = gerarSumarioProjeto(usu, proj);
		// Atualizar url no painel do projeto
		CadastroProjeto cadProj = getCadastro(ChavesModelo.PROJETO);
		cadProj.carregarConteudo(usu, proj);
		String contProj = proj.getConteudo();
		logger.trace("Conteúdo do painel do projeto: {}", contProj);
		logger.trace("Grupo $1: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$1"));
		logger.trace("Grupo $2: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$2"));
		logger.trace("Grupo $3: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$3"));
		logger.trace("Grupo $4: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$4"));
		logger.trace("Grupo $5: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$5"));
		logger.trace("Grupo $6: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$6"));
		logger.trace("Grupo $7: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$7"));
		logger.trace("Grupo $8: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$8"));
		logger.trace("Grupo $9: {}", contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$9"));
		if (contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$4").equals("")) {
			String sufixoPainel = contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$2").equals("") ? "" : "</en-note>";
			proj.setConteudo(contProj.replaceFirst(EXP_URL_PAINEL_PROJETO,
			        "$1$3" + PREFIXO_URL_PAINEL_PROJETO + sum.getURL() + SUFIXO_URL_PAINEL_PROJETO + "$9" + sufixoPainel));
			logger.trace("URL do sumário criada no painel: {}", proj.getNome());
		} else {
			proj.setConteudo(contProj.replaceFirst(EXP_URL_PAINEL_PROJETO, "$1$3$5$6" + sum.getURL() + "$8$9"));
			logger.trace("URL do sumário alterada no painel: {}", proj.getNome());
		}
		cadProj.alterar(usu, proj);
		// Atualizar url na validação do projeto
		Sumario sumValid = proj.getSumarioValidacao();
		carregarConteudo(usu, sumValid);
		String contValid = sumValid.getConteudo();
		logger.trace("Conteúdo da validação do projeto: {}", contValid);
		logger.trace("Grupo $1: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$1"));
		logger.trace("Grupo $2: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$2"));
		logger.trace("Grupo $3: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$3"));
		logger.trace("Grupo $4: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$4"));
		logger.trace("Grupo $5: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$5"));
		logger.trace("Grupo $6: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$6"));
		logger.trace("Grupo $7: {}", contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$7"));
		if (contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$2").equals("")) {
			sumValid.setConteudo(contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO,
			        "$1" + PREFIXO_URL_VALIDACAO_PROJETO + sum.getURL() + SUFIXO_URL_VALIDACAO_PROJETO + "$7"));
			logger.trace("URL do sumário criada na validação: {}", proj.getNome());
		} else {
			sumValid.setConteudo(contValid.replaceFirst(EXP_URL_VALIDACAO_PROJETO, "$1$3$4" + sum.getURL() + "$6$7"));
			logger.trace("URL do sumário alterada na validação: {}", proj.getNome());
		}
		alterar(usu, sumValid);
		return sum;
	}

	public Sumario gerarSumarioProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_SUMARIO + OP_PROJETO + URL_ID + proj.getId());
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
		return gerarSumario(usu, TITULO_PROJETOS, OP_PROJETOS, clProjs, grDetProjVazio);
	}

	public Sumario gerarSumarioProjetosDetalhado(Usuario usu, Collection<Projeto> clProjs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_PROJETOS + TITULO_DETALHADO, OP_PROJETOS, clProjs, grDetProj);
	}

	public Sumario gerarSumarioProjetos(Usuario usu, Grupo<Projeto> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_PROJETOS, OP_PROJETOS, raiz, grDetProjVazio);
	}

	public Sumario gerarSumarioProjetosDetalhado(Usuario usu, Grupo<Projeto> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_PROJETOS + TITULO_DETALHADO, OP_PROJETOS, raiz, grDetProj);
	}

	public void gerarDetalhamentoProjeto(StringBuffer cont, Projeto proj, String estiloTitulo, String estiloTituloURL, String estiloItem,
	        String estiloItemURL) {
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
		Grupo<Referencia> grRefs = proj.getReferenciasPorFormato();
		if (!grRefs.isVazio()) {
			gerarItem(cont, "Referências", estiloTitulo);
			gerarDetalhamentoGrupo(cont, grRefs, estiloItem, estiloItemURL, grDetRefVazio);
		}
		gerarFimLista(cont);
	}

	public Sumario consultarValidacaoPorProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		return consultarPorNome(usu, TITULO_VALIDACAO + TITULO_PROJETO + proj.getNome());
	}

	public Sumario gerarValidacaoProjeto(Usuario usu, Projeto proj) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_VALIDACAO + OP_PROJETO + URL_ID + proj.getId());
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
			GeradorENML<Acao> grAcao = (cnt, acao) -> gerarListaItens(cnt, acao, grTitAcao, acao.getMensagensValidacao(), grMsgValidPeq);
			Collection<Acao> clAcoes = new ArrayList<Acao>(proj.getAcoesCalendario());
			clAcoes.addAll(proj.getAcoesEmFoco());
			clAcoes.addAll(proj.getAcoesDelegadas());
			clAcoes.addAll(proj.getAcoesProximas());
			clAcoes.removeIf((Nota nota) -> nota.getMensagensValidacao().isEmpty());
			gerarListaItens(cont, "Ações", grTitulo, clAcoes, grAcao);
			temMensagem |= !clAcoes.isEmpty();
			// Referências
			GeradorENML<Referencia> grTitRef = (cnt, ref) -> gerarItemURL(cnt, ref, ESTILO_URL_PEQUENO_1);
			GeradorENML<Referencia> grRef = (cnt, ref) -> gerarListaItens(cnt, ref, grTitRef, ref.getMensagensValidacao(), grMsgValidPeq);
			Collection<Referencia> clRefs = new ArrayList<Referencia>(proj.getReferencias());
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
		return gerarValidacao(usu, TITULO_PROJETOS, OP_PROJETOS, clProjs);
	}

	//// SUMÁRIOS DE AÇÕES ////

	public static final String OP_ACOES = "acoes";
	public static final String OP_ACOES_CALENDARIO = "acoes_calendario";

	private static final String TITULO_ACOES = " - Ações";
	private static final String TITULO_CALENDARIO = " - Calendário";

	private static final SimpleDateFormat FORMATO_DATA_ANO;
	private static final SimpleDateFormat FORMATO_DATA_MES;
	private static final SimpleDateFormat FORMATO_DATA_DIA;
	private static final SimpleDateFormat FORMATO_DATA_HORA;

	static {
		FORMATO_DATA_ANO = new SimpleDateFormat("yyyy");
		FORMATO_DATA_ANO.setTimeZone(FusoHorario.FORTALEZA);
		FORMATO_DATA_MES = new SimpleDateFormat("MMMM");
		FORMATO_DATA_MES.setTimeZone(FusoHorario.FORTALEZA);
		FORMATO_DATA_DIA = new SimpleDateFormat("EEE, dd");
		FORMATO_DATA_DIA.setTimeZone(FusoHorario.FORTALEZA);
		FORMATO_DATA_HORA = new SimpleDateFormat("HH:mm - ");
		FORMATO_DATA_HORA.setTimeZone(FusoHorario.FORTALEZA);
	}

	private final GeradorENML<Acao> grDetAcaoVazio = (cont, acao) -> {
	};
	private final GeradorENML<Acao> grDetAcao = (cont, acao) -> gerarDetalhamentoAcao(cont, acao, ESTILO_TEXTO_PEQUENO_1,
	        ESTILO_URL_PEQUENO_1);

	public Sumario gerarSumarioAcoes(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		return gerarSumario(usu, TITULO_ACOES, OP_ACOES, clAcoes, grDetAcaoVazio);
	}

	public Sumario gerarSumarioAcoesDetalhado(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		return gerarSumario(usu, TITULO_ACOES + TITULO_DETALHADO, OP_ACOES, clAcoes, grDetAcao);
	}

	public Sumario gerarSumarioAcoes(Usuario usu, Grupo<Acao> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_ACOES, OP_ACOES, raiz, grDetAcaoVazio);
	}

	public Sumario gerarSumarioAcoesDetalhado(Usuario usu, Grupo<Acao> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_ACOES + TITULO_DETALHADO, OP_ACOES, raiz, grDetAcao);
	}

	public Sumario gerarSumarioAcoesCalendario(Usuario usu, Collection<Acao> clAcoes) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_SUMARIO + OP_ACOES_CALENDARIO);
		gerarDetalhamentoCalendario(cont, ESTILO_TEXTO, ESTILO_TEXTO_PEQUENO_1, ESTILO_TEXTO_PEQUENO_2, ESTILO_URL_PEQUENO_2, clAcoes);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + TITULO_ACOES + TITULO_CALENDARIO, true, cont.toString());
	}

	public void gerarDetalhamentoCalendario(StringBuffer cont, String estiloTitulo, String estiloItemInterm, String estiloItem,
	        String estiloURLItem, Collection<Acao> clAcoes) {
		clAcoes.removeIf((acao) -> acao.getDataLembrete() == null);
		List<Acao> lsAcoes = new ArrayList<Acao>(clAcoes);
		Collections.sort(lsAcoes, (a, b) -> a.getDataLembrete().compareTo(b.getDataLembrete()));
		gerarInicioLista(cont);
		int ano, anoAnt = -1, mes, mesAnt = -1, dia, diaAnt = -1;
		Calendar cal = Calendar.getInstance(FusoHorario.FORTALEZA);
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
		return gerarValidacao(usu, TITULO_ACOES, OP_ACOES, clAcoes);
	}

	//// SUMÁRIOS DE REFERÊNCIAS ////

	public static final String OP_REFERENCIAS = "referencias";

	private static final String TITULO_REFERENCIAS = " - Referências";

	private final GeradorENML<Referencia> grDetRefVazio = (cont, ref) -> {
	};
	private final GeradorENML<Referencia> grDetRef = (cont, acao) -> gerarDetalhamentoReferencia(cont, acao, ESTILO_TEXTO_PEQUENO_1,
	        ESTILO_URL_PEQUENO_1);

	public Sumario gerarSumarioReferencias(Usuario usu, Collection<Referencia> clRefs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_REFERENCIAS, OP_REFERENCIAS, clRefs, grDetRefVazio);
	}

	public Sumario gerarSumarioReferenciasDetalhado(Usuario usu, Collection<Referencia> clRefs) throws ErroCadastro {
		return gerarSumario(usu, TITULO_REFERENCIAS + TITULO_DETALHADO, OP_REFERENCIAS, clRefs, grDetRef);
	}

	public Sumario gerarSumarioReferencias(Usuario usu, Grupo<Referencia> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_REFERENCIAS, OP_REFERENCIAS, raiz, grDetRefVazio);
	}

	public Sumario gerarSumarioReferenciasDetalhado(Usuario usu, Grupo<Referencia> raiz) throws ErroCadastro {
		return gerarSumario(usu, TITULO_REFERENCIAS + TITULO_DETALHADO, OP_REFERENCIAS, raiz, grDetRef);
	}

	private void gerarDetalhamentoReferencia(StringBuffer cont, Referencia ref, String estiloTitulo, String estiloItemURL) {
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
		return gerarValidacao(usu, TITULO_REFERENCIAS, OP_REFERENCIAS, clRefs);
	}

	//// SUMÁRIOS GENÉRICOS ////

	public <TipoEnt extends EntidadeEvn<?>> Sumario gerarSumario(Usuario usu, String titulo, String oper, Collection<TipoEnt> clEnts,
	        GeradorENML<TipoEnt> geradorDetalhamentoEnt) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_SUMARIO + oper);
		try {
			gerarUrlsSumario(usu, cont, titulo);
		} catch (ErroItemNaoEncontrado e) {
		}
		gerarInicioLista(cont);
		for (TipoEnt ent : clEnts) {
			gerarItemURL(cont, ent, ESTILO_URL);
			geradorDetalhamentoEnt.executar(cont, ent);
		}
		gerarFimLista(cont);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + titulo, !titulo.contains(TITULO_DETALHADO), cont.toString());
	}

	public <TipoEnt extends EntidadeEvn<?>> Sumario gerarSumario(Usuario usu, String titulo, String oper, Grupo<TipoEnt> raiz,
	        GeradorENML<TipoEnt> geradorDetalhamentoEnt) throws ErroCadastro {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_SUMARIO + oper);
		try {
			gerarUrlsSumario(usu, cont, titulo);
		} catch (ErroItemNaoEncontrado e) {
		}
		gerarInicioLista(cont);
		gerarItem(cont, raiz.getNome(), ESTILO_TEXTO);
		gerarDetalhamentoGrupo(cont, raiz, ESTILO_TEXTO, ESTILO_URL, geradorDetalhamentoEnt);
		gerarFimLista(cont);
		gerarRodape(cont);
		return gerarSumario(usu, TITULO_SUMARIO + titulo + TITULO_GRUPOS, false, cont.toString());
	}

	private <TipoEnt extends EntidadeEvn<?>> void gerarDetalhamentoGrupo(StringBuffer cont, Grupo<TipoEnt> raiz, String estiloItem,
	        String estiloItemURL, GeradorENML<TipoEnt> geradorDetalhamentoEnt) {
		gerarInicioLista(cont);
		TipoEnt entGrp;
		for (Grupo<TipoEnt> grupo : raiz.getGruposFilho()) {
			gerarInicioItem(cont);
			entGrp = grupo.getEntidade();
			if (entGrp != null) {
				gerarItemURL(cont, entGrp, estiloItemURL);
			} else {
				gerarItem(cont, grupo.getNome(), estiloItem);
			}
			gerarFimItem(cont);
			gerarDetalhamentoGrupo(cont, grupo, estiloItem, estiloItemURL, geradorDetalhamentoEnt);
		}
		for (TipoEnt ent : raiz.getEntidadesFilho()) {
			gerarItemURL(cont, ent, estiloItemURL);
			geradorDetalhamentoEnt.executar(cont, ent);
		}
		gerarFimLista(cont);
		entGrp = raiz.getEntidade();
		if (entGrp != null) {
			geradorDetalhamentoEnt.executar(cont, entGrp);
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
	private final GeradorENML<String> grMsgValid = (cnt, msg) -> gerarItemValidacao(cnt, msg, ESTILO_TEXTO_PEQUENO_1);
	private final GeradorENML<String> grMsgValidPeq = (cnt, msg) -> gerarItemValidacao(cnt, msg, ESTILO_TEXTO_PEQUENO_2);

	private Sumario gerarValidacao(Usuario usu, String titulo, String oper, Collection<? extends Nota> clNotas) throws ErroCadastro {
		clNotas.removeIf((Nota nota) -> nota.getMensagensValidacao().isEmpty());
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont, ContextoEvn.getContexto(usu).getURLGeracao() + URL_VALIDACAO + oper);
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

	private static final SimpleDateFormat FORMATO_DATA_ATUALIZADO;

	static {
		FORMATO_DATA_ATUALIZADO = new SimpleDateFormat("dd/MM/yyyy - HH:mm");
		FORMATO_DATA_ATUALIZADO.setTimeZone(FusoHorario.FORTALEZA);
	}

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

	private <TipoEnt extends EntidadeEvn<?>> void gerarItemURL(StringBuffer cont, TipoEnt ent, String estiloTexto) {
		String url;
		try {
			Sumario sum = (Sumario) ent.get("sumario");
			url = sum != null ? sum.getURL() : "http://evernote.com";
		} catch (ErroPropriedadeEntidadeNaoDefinida e) {
			url = ((Nota) ent).getURL();
		}
		gerarItemURL(cont, ent.getNome(), estiloTexto, url);
	}

	private void gerarItemValidacao(StringBuffer cont, String texto, String estiloTexto) {
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
			sum = FabricaEntidade.getInstancia(Sumario.class);
			sum.setNome(nome);
			sum.setLembrete(ehLembrete);
			sum.setConteudo(conteudo);
			incluir(usu, sum);
		}
		return sum;
	}

	public Collection<Sumario> consultarSumariosInvalidos(Usuario usu) throws ErroCadastro {
		Collection<Sumario> clSums = new ArrayList<Sumario>(consultarTodos(usu));
		clSums.removeIf((Sumario sum) -> {
			try {
				String nome = sum.getNome();
				if (!consultarPorNome(usu, nome).getId().equals(sum.getId())) {
					return false; // Duplicação
				}
				String modelo = nome.replaceFirst(EXP_TITULO, "$3");
				String chave = nome.replaceFirst(EXP_TITULO, "$4");
				if (!modelo.equals(nome)) {
					if (nome.startsWith("Sumário do") || nome.startsWith("Validação do")) {
						if (!modelo.equals("") && !modelo.equals(nome)) {
							CadastroEvn<?, ?> cad = getCadastro(ChavesModelo.PACOTE + "." + modelo.toUpperCase());
							try {
								cad.consultarPorNome(usu, chave);
								return true; // Sumário individual válido
							} catch (ErroItemNaoEncontrado e) {
							}
						}
					} else {
						return true; // Sumário coletivo válido
					}
				}
				return false; // Sumário inválido ou sumário de entidade excluída
			} catch (ErroCadastro e) {
				getLogger().error("Erro consultando sumários inválidos", e);
				return true;
			}
		});
		return clSums;
	}
}
