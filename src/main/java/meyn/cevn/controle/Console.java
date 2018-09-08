package meyn.cevn.controle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import meyn.cevn.ClienteEvn;
import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.EntidadeEvn;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.Fachada;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.Usuario;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.CadastroSumario;
import meyn.cevn.modelo.sumario.Sumario;
import meyn.cevn.util.FusoHorario;
import meyn.util.Erro;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
@ManagedBean(name = "console")
@RequestScoped
public class Console extends ManagedBeanEvn {

	Fachada fc;

	public Console() throws ErroModelo {
		fc = FabricaFachada.getFachada();
		sessao = Sessao.getSessao(getUsuario());
		iniciarEntidades();
	}

	//// SESSÃO ////

	private static class Sessao {

		static Sessao getSessao(Usuario usu) throws ErroModelo {
			ContextoEvn contexto = ContextoEvn.getContexto(usu);
			String chave = Sessao.class.getName();
			synchronized (contexto) {
				if (!contexto.containsKey(chave)) {
					contexto.put(chave, new Sessao(usu));
				}
			}
			return (Sessao) contexto.get(chave);
		}

		Fachada fc;
		
		Usuario usuario;
		int contadorProcessos = 0;
		String tempoProc = "";
		Date inicioProc = null;
		Date fimProc = null;
		StringBuffer saidaPadrao = new StringBuffer();
		StringBuffer saidaErro = new StringBuffer();

		Logger logger = LogManager.getLogger(getClass());

		Sessao(Usuario usuario) throws ErroModelo {
			this.usuario = usuario;
			ClienteEvn.invalidarCaches(usuario);
			fc = FabricaFachada.getFachada();
			fc.excluirSumariosInvalidos(usuario);
			logger.debug("sessão console criada");
		}

		Integer getContadorProcessos() {
			logger.trace("contador de processos solicitado: {}", contadorProcessos);
			return contadorProcessos;
		}

		String getStatusProcessamento() {
			return saidaErro.length() == 0 ? "ok" : "erro";
		}

		void zerarTempoProcessamento() {
			synchronized (this) {
				if (contadorProcessos == 0) {
					tempoProc = "";
					inicioProc = fimProc = null;
				}
			}
		}

		String getTempoProcessamento() {
			if (inicioProc != null) {
				long ini = inicioProc.getTime();
				long fim = fimProc.getTime();
				if (fim >= ini) {
					float tempo = (float) (fim - ini);
					Float m = tempo / 60000, s = (tempo / 1000) % 60;
					tempoProc = String.format("%.1f s", s);
					if (m >= 1) {
						tempoProc = String.format("%.0f m ", m) + tempoProc;
					}
				}
			}
			return tempoProc;
		}

		String getUrlLog() {
			return usuario.getLog().getURL();
		}

		String getSaidaPadrao() {
			return saidaPadrao.toString();
		}

		String getSaidaErro() {
			return saidaErro.toString();
		}

		void limparSaida() {
			zerarTempoProcessamento();
			synchronized (saidaPadrao) {
				saidaPadrao.setLength(0);
			}
			synchronized (saidaErro) {
				saidaErro.setLength(0);
			}
			logger.trace("saída limpa");
		}
	}

	private Sessao sessao;

	public int getContadorProcessos() {
		return sessao.getContadorProcessos();
	}

	public String getStatusProcessamento() {
		return sessao.getStatusProcessamento();
	}

	public String getTempoProcessamento() {
		return sessao.getTempoProcessamento();
	}

	public String getUrlLog() {
		return sessao.getUrlLog();
	}

	public String getSaidaPadrao() {
		return sessao.getSaidaPadrao();
	}

	public String getSaidaErro() {
		return sessao.getSaidaErro();
	}

	public void limparSaida() {
		sessao.limparSaida();
	}

	//// INTERESSES ////

	private static final String OP_INTERESSES = CadastroSumario.OP_INTERESSES;
	private static final String OP_INTERESSE = CadastroSumario.OP_INTERESSE;
	private Collection<Interesse> clIntrs;
	private Collection<Interesse> clIntrsFiltrado;
	private boolean gerarSumIntrsCol = false;
	private boolean gerarSumIntrsInd = false;

	public Collection<Interesse> getInteresses() throws ErroModelo {
		return clIntrs;
	}

	public Collection<Interesse> getInteressesFiltrado() {
		return clIntrsFiltrado;
	}

	public void setInteressesFiltrado(Collection<Interesse> clIntrsFiltrado) {
		this.clIntrsFiltrado = clIntrsFiltrado;
	}

	public boolean isGerarSumInteressesCol() {
		return gerarSumIntrsCol;
	}

	public void setGerarSumInteressesCol(boolean gerarSumIntrsCol) {
		this.gerarSumIntrsCol = gerarSumIntrsCol;
	}

	public boolean isGerarSumInteressesInd() {
		return gerarSumIntrsInd;
	}

	public void setGerarSumInteressesInd(boolean gerarSumIntrsInd) {
		this.gerarSumIntrsInd = gerarSumIntrsInd;
	}

	//// PROJETOS ////

	private static final String OP_PROJETOS = CadastroSumario.OP_PROJETOS;
	private static final String OP_VAL_PROJETOS = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_PROJETOS;
	private static final String OP_VAL_PROJETOS_PAR = OP_VAL_PROJETOS + "PAR";
	private static final String OP_PROJETO = CadastroSumario.OP_PROJETO;
	private static final String OP_VAL_PROJETO = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_PROJETO;
	private static final String OP_VAL_PROJETO_PAR = OP_VAL_PROJETO + "PAR";
	private Collection<Projeto> clProjs;
	private Collection<Projeto> clProjsFiltrado;
	private boolean gerarSumProjsCol = false;
	private boolean gerarSumProjsInd = false;
	private boolean gerarValProjsCol = false;
	private boolean gerarValProjsInd = false;

	public Collection<Projeto> getProjetos() throws ErroModelo {
		return clProjs;
	}

	public Collection<Projeto> getProjetosFiltrado() throws ErroModelo {
		return clProjsFiltrado;
	}

	public void setclProjetosFiltradoFiltrado(Collection<Projeto> clProjsFiltrado) {
		this.clProjsFiltrado = clProjsFiltrado;
	}

	public boolean isGerarSumProjetosCol() {
		return gerarSumProjsCol;
	}

	public void setGerarSumProjetosCol(boolean gerarSumProjsCol) {
		this.gerarSumProjsCol = gerarSumProjsCol;
	}

	public boolean isGerarSumProjetosInd() {
		return gerarSumProjsInd;
	}

	public void setGerarSumProjetosInd(boolean gerarSumProjsInd) {
		this.gerarSumProjsInd = gerarSumProjsInd;
	}

	public boolean isGerarValProjetosCol() {
		return gerarValProjsCol;
	}

	public void setGerarValProjetosCol(boolean gerarValProjsCol) {
		this.gerarValProjsCol = gerarValProjsCol;
	}

	public boolean isGerarValProjetosInd() {
		return gerarValProjsInd;
	}

	public void setGerarValProjetosInd(boolean gerarValProjsInd) {
		this.gerarValProjsInd = gerarValProjsInd;
	}

	//// AÇÕES ////

	private static final String OP_ACOES = CadastroSumario.OP_ACOES;
	private static final String OP_ACOES_CALEND = CadastroSumario.OP_ACOES_CALENDARIO;
	private static final String OP_VAL_ACOES = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_ACOES;
	private static final String OP_VAL_ACOES_PAR = OP_VAL_ACOES + "PAR";
	private Collection<Acao> clAcoes;
	private Collection<Acao> clAcoesFiltrado;
	private boolean gerarSumAcoesCol = false;
	private boolean gerarValAcoesCol = false;

	public Collection<Acao> getAcoes() throws ErroModelo {
		return clAcoes;
	}

	public Collection<Acao> getAcoesFiltrado() throws ErroModelo {
		return clAcoesFiltrado;
	}

	public void setAcoesFiltrado(Collection<Acao> clAcoesFiltrado) {
		this.clAcoesFiltrado = clAcoesFiltrado;
	}

	public boolean isGerarSumAcoesCol() {
		return gerarSumAcoesCol;
	}

	public void setGerarSumAcoesCol(boolean gerarSumAcoesCol) {
		this.gerarSumAcoesCol = gerarSumAcoesCol;
	}

	public boolean isGerarValAcoesCol() {
		return gerarValAcoesCol;
	}

	public void setGerarValAcoesCol(boolean gerarValAcoesCol) {
		this.gerarValAcoesCol = gerarValAcoesCol;
	}

	//// REFERÊNCIAS ////

	private static final String OP_REFERENCIAS = CadastroSumario.OP_REFERENCIAS;
	private static final String OP_VAL_REFERENCIAS = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_REFERENCIAS;
	private static final String OP_VAL_REFERENCIAS_PAR = OP_VAL_REFERENCIAS + "PAR";
	private Collection<Referencia> clRefs;
	private Collection<Referencia> clRefsFiltrado;
	private boolean gerarSumRefsCol = false;
	private boolean gerarValRefsCol = false;

	public Collection<Referencia> getReferencias() throws ErroModelo {
		return clRefs;
	}

	public Collection<Referencia> getReferenciasFiltrado() throws ErroModelo {
		return clRefsFiltrado;
	}

	public void setReferenciasFiltrado(Collection<Referencia> clRefsFiltrado) {
		this.clRefsFiltrado = clRefsFiltrado;
	}

	public boolean isGerarSumReferenciasCol() {
		return gerarSumRefsCol;
	}

	public void setGerarSumReferenciasCol(boolean gerarSumRefsCol) {
		this.gerarSumRefsCol = gerarSumRefsCol;
	}

	public boolean isGerarValReferenciasCol() {
		return gerarValRefsCol;
	}

	public void setGerarValReferenciasCol(boolean gerarValRefsCol) {
		this.gerarValRefsCol = gerarValRefsCol;
	}

	//// SUMÁRIOS ////

	private Collection<Sumario> clSums;
	private Collection<Sumario> clSumsFiltrado;

	public Collection<Sumario> getSumarios() throws ErroModelo {
		return clSums;
	}

	public Collection<Sumario> getSumariosFiltrado() throws ErroModelo {
		return clSumsFiltrado;
	}

	public void setSumariosFiltrado(Collection<Sumario> clSumariosFiltrado) {
		this.clSumsFiltrado = clSumariosFiltrado;
	}

	//// CONFIGURADOR ////

	private Collection<Nota> clLogs;

	public Collection<Nota> getLogs() throws ErroModelo {
		return clLogs;
	}

	//// OPERAÇÕES ////

	private static final SimpleDateFormat FORMATO_DATA_HORA;

	static {
		FORMATO_DATA_HORA = new SimpleDateFormat("[HH:mm:ss] ");
		FORMATO_DATA_HORA.setTimeZone(FusoHorario.FORTALEZA);
	}

	@FunctionalInterface
	private static interface Operacao {
		void executar(Usuario usu) throws ErroModelo;
	}

	@FunctionalInterface
	private static interface OperacaoEntidade<TipoEnt extends EntidadeEvn<?>> {
		void executar(Usuario usu, TipoEnt ent) throws ErroModelo;
	}

	private static class InfoOperacao {
		String msgOperacao;
		String msgOperacaoMin;
		String msgEntidade;
		String msgEntidadeMin;
		String msgStatusSucesso;
		String msgStatusErro;

		InfoOperacao(String msgOperacao, String msgEntidade, String msgStatusSucesso, String msgStatusErro) {
			this.msgOperacao = msgOperacao;
			this.msgOperacaoMin = msgOperacao.toLowerCase();
			this.msgEntidade = msgEntidade;
			this.msgEntidadeMin = msgEntidade.toLowerCase();
			this.msgStatusSucesso = msgStatusSucesso;
			this.msgStatusErro = msgStatusErro;
		}
	}

	private abstract class ProcessoOperacao extends Thread {

		ProcessoOperacao(boolean requerValidacao) throws ErroModelo {
			sessao.contadorProcessos++;
			if (requerValidacao) {
				validarEntidades();
			}
		}

		@Override
		public void run() {
			ThreadContext.put("usuario", sessao.usuario.getId());
		}

		void iniciar(boolean assincrono) {
			if (assincrono) {
				start();
			} else {
				run();
			}
		}

		void iniciarOperacao(Date inicio) {
			synchronized (sessao) {
				getLogger().trace("operação iniciada (processos={})", sessao.contadorProcessos);
				if (sessao.inicioProc == null) {
					sessao.inicioProc = sessao.fimProc = inicio;
				}
			}
		}

		void finalizarOperacao(Date fim) {
			synchronized (sessao) {
				sessao.contadorProcessos--;
				if (sessao.contadorProcessos == 0) {
					sessao.fimProc = fim;
				}
				getLogger().trace("operação finalizada (processos={})", sessao.contadorProcessos);
			}
		}

		void adicionarRotuloTempo(Date tempo) {
			sessao.saidaPadrao.append(FORMATO_DATA_HORA.format(tempo));
		}

		void adicionarTempoProcessamento(Date inicio, Date fim) {
			sessao.saidaPadrao.append(" (");
			sessao.saidaPadrao.append(String.format("%.1f", (float) (fim.getTime() - inicio.getTime()) / 1000));
			sessao.saidaPadrao.append(" s)\n");
		}
	}

	private static class InfoOperacaoLote extends InfoOperacao {
		Operacao oprLote;

		InfoOperacaoLote(String msgOperacao, String msgEntidade, String msgStatusSucesso, String msgStatusErro,
				Operacao oprLote) {
			super(msgOperacao, msgEntidade, msgStatusSucesso, msgStatusErro);
			this.oprLote = oprLote;
		}
	}

	private class ProcessoOperacaoLote extends ProcessoOperacao {

		InfoOperacaoLote info;

		ProcessoOperacaoLote(boolean requerValidacao) throws ErroModelo {
			super(requerValidacao);
		}

		ProcessoOperacaoLote(InfoOperacaoLote info, boolean requerValidacao) throws ErroModelo {
			super(requerValidacao);
			this.info = info;
		}

		@Override
		public void run() {
			super.run();
			Date inicio, fim = null;
			inicio = new Date();
			iniciarOperacao(inicio);
			synchronized (sessao.saidaPadrao) {
				adicionarRotuloTempo(inicio);
				sessao.saidaPadrao.append(info.msgOperacao).append(info.msgEntidadeMin).append("...\n");
			}
			String msgStatus = info.msgStatusSucesso;
			try {
				info.oprLote.executar(sessao.usuario);
			} catch (Exception e) {
				msgStatus = info.msgStatusErro;
				synchronized (sessao.saidaErro) {
					sessao.saidaErro.append("*** Erro ").append(info.msgOperacaoMin).append(info.msgEntidadeMin);
					sessao.saidaErro.append(" ***\n\n").append(Erro.toString(e)).append("\n");
				}
				getLogger().error("Erro {}{}", info.msgOperacaoMin, info.msgEntidadeMin, e);
			}
			fim = new Date();
			synchronized (sessao.saidaPadrao) {
				adicionarRotuloTempo(fim);
				sessao.saidaPadrao.append(info.msgEntidade).append(msgStatus);
				adicionarTempoProcessamento(inicio, fim);
			}
			finalizarOperacao(fim);
		}
	}

	private static class InfoGeracaoSumarios extends InfoOperacaoLote {

		InfoGeracaoSumarios(String msgEntSumPlr, boolean ehEntSumGenMasc, boolean ehSumario,
				Operacao operacaoSums) {
			super("Gerando ", formatarMsgSumario(msgEntSumPlr, ehEntSumGenMasc, ehSumario),
					formatarMsgStatus(ehSumario), " não foram" + formatarMsgStatus(ehSumario), operacaoSums);
		}

		static String formatarMsgSumario(String msgEntSumPlr, boolean ehEntSumGenMasc, boolean ehSumario) {
			return (ehSumario ? "Sumários" : "Validações") + " coletiv" + (ehSumario ? "os" : "as") + " d"
					+ (ehEntSumGenMasc ? "os " : "as ") + msgEntSumPlr;
		}

		static String formatarMsgStatus(boolean ehSumario) {
			return " gerad" + (ehSumario ? "o" : "a") + "s com sucesso";
		}
	}

	private class ProcessoGeracaoSumarios extends ProcessoOperacaoLote {

		ProcessoGeracaoSumarios(InfoGeracaoSumarios info) throws ErroModelo {
			super(info, info.msgEntidadeMin.startsWith("val"));
		}
	}

	private static class InfoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends InfoOperacao {
		String msgSumarioInd;
		String msgSumarioIndMin;
		OperacaoEntidade<TipoEnt> oprSumario;

		InfoGeracaoSumario(String msgEntSum, boolean ehEntSumGenMasc, boolean ehSumario,
				OperacaoEntidade<TipoEnt> operacaoSum) {
			super("Gerando ", formatarMsgSumario(msgEntSum + "s", ehEntSumGenMasc, ehSumario),
					formatarMsgStatus(ehSumario), " não foi" + formatarMsgStatus(ehSumario));
			this.oprSumario = operacaoSum;
			this.msgSumarioInd = formatarMsgSumarioInd(msgEntSum, ehEntSumGenMasc, ehSumario);
			this.msgSumarioIndMin = msgSumarioInd.toLowerCase();
		}

		static String formatarMsgSumario(String msgEntSumPlr, boolean ehEntSumGenMasc, boolean ehSumario) {
			return (ehSumario ? "Sumários" : "Validações") + " individuais d" + (ehEntSumGenMasc ? "os " : "as ")
					+ msgEntSumPlr;
		}

		static String formatarMsgSumarioInd(String msgEntSum, boolean ehEntSumGenMasc, boolean ehSumario) {
			return (ehSumario ? "Sumário" : "Validação") + " d" + (ehEntSumGenMasc ? "o " : "a ") + msgEntSum + " ";
		}

		static String formatarMsgStatus(boolean ehSumario) {
			return " gerad" + (ehSumario ? "o" : "a") + " com sucesso";
		}
	}

	private class ProcessoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends ProcessoOperacao {

		InfoGeracaoSumario<TipoEnt> info;
		Collection<TipoEnt> clEnts;

		ProcessoGeracaoSumario(InfoGeracaoSumario<TipoEnt> info, Collection<TipoEnt> clEnts) throws ErroModelo {
			super(info.msgEntidadeMin.startsWith("val"));
			this.info = info;
			this.clEnts = clEnts;
		}

		@SuppressWarnings("unchecked")
		ProcessoGeracaoSumario(InfoGeracaoSumario<? extends EntidadeEvn<?>> info, List<EntidadeEvn<?>> clEnts) throws ErroModelo {
			this((InfoGeracaoSumario<TipoEnt>) info, (Collection<TipoEnt>)clEnts);
		}

		@Override
		public void run() {
			super.run();
			Date inicio, fim = null;
			inicio = new Date();
			iniciarOperacao(inicio);
			synchronized (sessao.saidaPadrao) {
				adicionarRotuloTempo(inicio);
				sessao.saidaPadrao.append(info.msgOperacao).append(info.msgEntidadeMin).append("...\n");
			}
			for (TipoEnt ent : clEnts) {
				String msgStatus = info.msgStatusSucesso;
				try {
					info.oprSumario.executar(sessao.usuario, ent);
				} catch (Exception e) {
					msgStatus = info.msgStatusErro;
					synchronized (sessao.saidaErro) {
						sessao.saidaErro.append("*** Erro ").append(info.msgOperacaoMin);
						sessao.saidaErro.append(info.msgSumarioIndMin).append(ent.getNome());
						sessao.saidaErro.append(" ***\n\n").append(Erro.toString(e)).append("\n");
					}
					getLogger().error("Erro {}{}{}", info.msgOperacaoMin, info.msgSumarioIndMin, ent.getNome(), e);
				}
				fim = new Date();
				synchronized (sessao.saidaPadrao) {
					adicionarRotuloTempo(fim);
					sessao.saidaPadrao.append(info.msgSumarioInd).append(ent.getNome()).append(msgStatus);
					adicionarTempoProcessamento(inicio, fim);
				}
				inicio = new Date();
			}
			finalizarOperacao(fim);
		}
	}

	private class ProcessoExclusaoSumarios extends ProcessoOperacaoLote {

		ProcessoExclusaoSumarios() throws ErroModelo {
			super(false);
			info = new InfoOperacaoLote("Excluindo ", "Sumários", " excluídos com sucesso!",
					" não foram excluídos com sucesso!", (Usuario usu) -> {
						fc.excluirTodos(usu, ChavesModelo.SUMARIO);
						fc.invalidarCaches(usu, ChavesModelo.SUMARIO);
						fc.invalidarCaches(usu, ChavesModelo.INTERESSE);
						fc.invalidarCaches(usu, ChavesModelo.PROJETO);
						fc.consultarTodos(usu, ChavesModelo.INTERESSE);
						fc.consultarTodos(usu, ChavesModelo.PROJETO);
					});
		}
	}

	private class ProcessoExclusaoLogs extends ProcessoOperacaoLote {

		ProcessoExclusaoLogs() throws ErroModelo {
			super(false);
			info = new InfoOperacaoLote("Excluindo ", "Logs", " excluídos com sucesso!",
					" não foram excluídos com sucesso!", (Usuario usu) -> fc.excluirLogsAntigos(usu));
		}
	}

	private class MapaOperacoes extends HashMap<String, InfoGeracaoSumarios> {
		{
			Operacao operSums;
			operSums = (Usuario usu) -> fc.gerarSumarioInteresses(usu);
			put(OP_INTERESSES, new InfoGeracaoSumarios("interesses", true, true, operSums));
			operSums = (Usuario usu) -> fc.gerarSumarioProjetos(usu);
			put(OP_PROJETOS, new InfoGeracaoSumarios("projetos", true, true, operSums));
			operSums = (Usuario usu) -> fc.gerarValidacaoProjetos(usu);
			put(OP_VAL_PROJETOS, new InfoGeracaoSumarios("projetos", true, false, operSums));
			operSums = (Usuario usu) -> fc.gerarValidacaoProjetosParcial(usu);
			put(OP_VAL_PROJETOS_PAR, new InfoGeracaoSumarios("projetos", true, false, operSums));
			operSums = (Usuario usu) -> fc.gerarSumarioAcoes(usu);
			put(OP_ACOES, new InfoGeracaoSumarios("ações", false, true, operSums));
			operSums = (Usuario usu) -> fc.gerarSumarioAcoesCalendario(usu);
			put(OP_ACOES_CALEND, new InfoGeracaoSumarios("ações", false, true, operSums));
			operSums = (Usuario usu) -> fc.gerarValidacaoAcoes(usu);
			put(OP_VAL_ACOES, new InfoGeracaoSumarios("ações", false, false, operSums));
			operSums = (Usuario usu) -> fc.gerarValidacaoAcoesParcial(usu);
			put(OP_VAL_ACOES_PAR, new InfoGeracaoSumarios("ações", false, false, operSums));
			operSums = (Usuario usu) -> fc.gerarSumarioReferencias(usu);
			put(OP_REFERENCIAS, new InfoGeracaoSumarios("referências", false, true, operSums));
			operSums = (Usuario usu) -> fc.gerarValidacaoReferencias(usu);
			put(OP_VAL_REFERENCIAS, new InfoGeracaoSumarios("referências", false, false, operSums));
			operSums = (Usuario usu) -> fc.gerarValidacaoReferenciasParcial(usu);
			put(OP_VAL_REFERENCIAS_PAR, new InfoGeracaoSumarios("referências", false, false, operSums));
		}
	};

	private class MapaOperacoesInd extends HashMap<String, InfoGeracaoSumario<? extends EntidadeEvn<?>>> {
		{
			OperacaoEntidade<Interesse> oprSumIntr;
			OperacaoEntidade<Projeto> oprSumPrj;
			oprSumIntr = (Usuario usu, Interesse intr) -> fc.gerarSumarioInteresse(usu, intr.getId());
			put(OP_INTERESSE, new InfoGeracaoSumario<Interesse>("interesse", true, true, oprSumIntr));
			oprSumPrj = (Usuario usu, Projeto prj) -> fc.gerarSumarioProjeto(usu, prj.getId());
			put(OP_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, true, oprSumPrj));
			oprSumPrj = (Usuario usu, Projeto prj) -> fc.gerarValidacaoProjeto(usu, prj.getId());
			put(OP_VAL_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, false, oprSumPrj));
			oprSumPrj = (Usuario usu, Projeto prj) -> fc.gerarValidacaoProjetoParcial(usu, prj.getId());
			put(OP_VAL_PROJETO_PAR, new InfoGeracaoSumario<Projeto>("projeto", true, false, oprSumPrj));
		}
		
		@SuppressWarnings("unchecked")
		<TipoEnt extends EntidadeEvn<?>> InfoGeracaoSumario<TipoEnt> get(String key) {
			return (InfoGeracaoSumario<TipoEnt>) super.get(key);
		}		
	};

	private MapaOperacoes mpOperacoes = new MapaOperacoes();
	private MapaOperacoesInd mpOperacoesEnt = new MapaOperacoesInd();

	//// MANUTENÇÃO ENTIDADES ////

	boolean entidadesValidadas = false;

	private void exibirErro(Exception e) {
		synchronized (sessao.saidaErro) {
			sessao.saidaErro.append("*** Erro inesperado ***\n\n");
			sessao.saidaErro.append(Erro.toString(e)).append("\n");
		}
		getLogger().error("Erro inesperado", e);
	}

	public void iniciarEntidades() throws ErroModelo {
		Usuario usu = sessao.usuario;
		// Só checa mudanças se não houver processamento ativo
		getLogger().trace("iniciando entidades (processos={})", sessao.contadorProcessos);
		if (sessao.contadorProcessos == 0) {
			ClienteEvn.invalidarCaches(usu);
		}
		clSums = fc.consultarTodos(usu, ChavesModelo.SUMARIO);
		clIntrs = fc.consultarTodos(usu, ChavesModelo.INTERESSE);
		clProjs = fc.consultarTodos(usu, ChavesModelo.PROJETO);
		clAcoes = fc.consultarTodos(usu, ChavesModelo.ACAO);
		clRefs = fc.consultarTodos(usu, ChavesModelo.REFERENCIA);
		clLogs = fc.consultarTodos(usu, ChavesModelo.LOG);
		verificarConsistenciaSumarios();
		getLogger().debug("entidades iniciadas");
	}

	public void validarEntidades() throws ErroModelo {
		if (!entidadesValidadas) {
			Usuario usu = sessao.usuario;
			fc.validarEntidades(usu);
			fc.consultarTodos(usu, ChavesModelo.INTERESSE);
			fc.consultarTodos(usu, ChavesModelo.PROJETO);
			fc.consultarTodos(usu, ChavesModelo.ACAO);
			fc.consultarTodos(usu, ChavesModelo.REFERENCIA);
			entidadesValidadas = true;
			getLogger().debug("entidades validadas");
		}
	}

	public void recarregarEntidade() throws ErroModelo {
		Usuario usu = sessao.usuario;
		String modelo = ChavesModelo.NOME_PACOTE + getParametroRequisicao("modelo").toUpperCase();
		fc.invalidarCaches(usu, modelo);
		fc.consultarTodos(usu, modelo);
		getLogger().debug("entidades recarregadas: {}", modelo);
	}

	void verificarConsistenciaSumarios() {
		Collection<Sumario> clSumsInd = new ArrayList<Sumario>(clSums);
		clSumsInd.removeIf((sum) -> {
			String nome = sum.getNome();
			return !nome.startsWith("Sumário ") && !nome.startsWith("Validação ");
		});
		// Nem todos os sumários individuais foram criados
		if (clSumsInd.size() < clProjs.size() * 2 + clIntrs.size()) {
			getLogger().error("Quantidade de sumários individuais inconsistente: {} (intrs={}, projs={})",
					clSumsInd.size(), clIntrs.size(), clProjs.size());
		}
	}

	public String getResultadoGeracaoSumariosParametro() {
		try {
			sessao.limparSaida();
			String oper = getParametroRequisicao("sumario");
			String id = getParametroRequisicao("id");
			if (id == null) {
				new ProcessoGeracaoSumarios(mpOperacoes.get(oper)).iniciar(false);
			} else {
				String[] tokens = oper.split("_");
				String modelo = ChavesModelo.NOME_PACOTE + tokens[tokens.length - 1].toUpperCase();

				InfoGeracaoSumario<? extends EntidadeEvn<?>> info = mpOperacoesEnt.get(oper);
				EntidadeEvn<?> ent = FabricaEntidade.getInstancia(new HashMap<String, Object>() {
					{
						put("id", id);
					}
				}, EntidadeEvn.class);
				ent = fc.consultarPorChavePrimaria(sessao.usuario, modelo, ent);
				new ProcessoGeracaoSumario<EntidadeEvn<?>>(info, Arrays.asList(ent)).iniciar(false);
			}
		} catch (ErroModelo e) {
			exibirErro(e);
		}
		return sessao.getStatusProcessamento();
	}

	public void gerarSumariosConsole() {
		try {
			sessao.zerarTempoProcessamento();
			Collection<ProcessoOperacao> clProcessos = new ArrayList<ProcessoOperacao>();
			if (gerarSumIntrsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_INTERESSES)));
			}
			if (gerarSumIntrsInd) {
				clProcessos.add(new ProcessoGeracaoSumario<Interesse>(mpOperacoesEnt.get(OP_INTERESSE), clIntrs));
			}
			if (gerarSumProjsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_PROJETOS)));
			}
			if (gerarSumProjsInd) {
				clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesEnt.get(OP_PROJETO), clProjs));
			}
			if (gerarValProjsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_VAL_PROJETOS_PAR)));
			}
			if (gerarValProjsInd) {
				clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesEnt.get(OP_VAL_PROJETO_PAR), clProjs));
			}
			if (gerarSumAcoesCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_ACOES)));
			}
			if (gerarValAcoesCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_VAL_ACOES_PAR)));
			}
			if (gerarSumRefsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_REFERENCIAS)));
			}
			if (gerarValRefsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_VAL_REFERENCIAS_PAR)));
			}
			for (ProcessoOperacao processo : clProcessos) {
				processo.iniciar(true);
			}
		} catch (ErroModelo e) {
			exibirErro(e);
		}
	}

	public void excluirSumarios() {
		try {
			sessao.zerarTempoProcessamento();
			new ProcessoExclusaoSumarios().iniciar(true);
		} catch (ErroModelo e) {
			exibirErro(e);
		}
	}

	public void excluirLogs() {
		try {
			sessao.zerarTempoProcessamento();
			new ProcessoExclusaoLogs().iniciar(true);
		} catch (ErroModelo e) {
			exibirErro(e);
		}
	}
}
