package meyn.cevn.controle;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.evernote.edam.type.Notebook;

import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.CacheNotebooks;
import meyn.cevn.modelo.CacheTags;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.ClienteEvn;
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
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
@ManagedBean(name = "console")
@RequestScoped
public class Console extends ManagedBeanEvn {

	private Sessao sessao;
	private Fachada fachada;

	public Console() throws ErroModelo {
		sessao = Sessao.getSessao(getUsuario());
		fachada = FabricaFachada.getFachada();
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

		boolean nova = true;

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
			logger.debug("sessão console criada");
		}

		@SuppressWarnings("unused")
		public boolean isNova() {
			if (nova) {
				nova = false;
				return true;
			}
			return false;
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
	private static final String OP_VAL_PAR_PROJETOS = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_PROJETOS;
	private static final String OP_INI_PROJETO = "inicio_" + CadastroSumario.OP_PROJETO;
	private static final String OP_PROJETO = CadastroSumario.OP_PROJETO;
	private static final String OP_VAL_PROJETO = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_PROJETO;
	private static final String OP_VAL_PAR_PROJETO = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_PROJETO;
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
	private static final String OP_VAL_PAR_ACOES = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_ACOES;
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
	private static final String OP_VAL_PAR_REFERENCIAS = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_REFERENCIAS;
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

		boolean validacaoRequerida;

		ProcessoOperacao(boolean validacaoRequerida) throws ErroModelo {
			sessao.contadorProcessos++;
			this.validacaoRequerida = validacaoRequerida;
			if (validacaoRequerida) {
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
				logger.trace("operação iniciada (processos={})", sessao.contadorProcessos);
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
				logger.trace("operação finalizada (processos={})", sessao.contadorProcessos);
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

		InfoOperacaoLote(String msgOperacao, String msgEntidade, String msgStatusSucesso, String msgStatusErro, Operacao oprLote) {
			super(msgOperacao, msgEntidade, msgStatusSucesso, msgStatusErro);
			this.oprLote = oprLote;
		}
	}

	private class ProcessoOperacaoLote extends ProcessoOperacao {

		InfoOperacaoLote info;

		ProcessoOperacaoLote(boolean validacaoRequerida) throws ErroModelo {
			super(validacaoRequerida);
		}

		ProcessoOperacaoLote(InfoOperacaoLote info, boolean validacaoRequerida) throws ErroModelo {
			super(validacaoRequerida);
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
				logger.error("Erro {}{}", info.msgOperacaoMin, info.msgEntidadeMin, e);
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

		static String formatarMsgSumario(String msgEntSumPlr, boolean ehEntSumGenMasc, boolean ehSumario) {
			return (ehSumario ? "Sumários" : "Validações") + " coletiv" + (ehSumario ? "os" : "as") + " d"
					+ (ehEntSumGenMasc ? "os " : "as ") + msgEntSumPlr;
		}

		static String formatarMsgStatus(boolean ehSumario) {
			return " gerad" + (ehSumario ? "o" : "a") + "s com sucesso";
		}

		InfoGeracaoSumarios(String msgEntSumPlr, boolean ehEntSumGenMasc, boolean ehSumario, Operacao operacaoSums) {
			super("Gerando ", formatarMsgSumario(msgEntSumPlr, ehEntSumGenMasc, ehSumario), formatarMsgStatus(ehSumario),
					" não foram" + formatarMsgStatus(ehSumario), operacaoSums);
		}
	}

	private class ProcessoGeracaoSumarios extends ProcessoOperacaoLote {

		ProcessoGeracaoSumarios(InfoGeracaoSumarios info) throws ErroModelo {
			super(info, info.msgEntidadeMin.startsWith("val"));
		}
	}

	private static class InfoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends InfoOperacao {

		static String formatarMsgSumario(String msgEntSumPlr, boolean ehEntSumGenMasc, boolean ehSumario) {
			return (ehSumario ? "Sumários" : "Validações") + " individuais d" + (ehEntSumGenMasc ? "os " : "as ") + msgEntSumPlr;
		}

		static String formatarMsgSumarioInd(String msgEntSum, boolean ehEntSumGenMasc, boolean ehSumario) {
			return (ehSumario ? "Sumário" : "Validação") + " d" + (ehEntSumGenMasc ? "o " : "a ") + msgEntSum + " ";
		}

		static String formatarMsgStatus(boolean ehSumario) {
			return " gerad" + (ehSumario ? "o" : "a") + " com sucesso";
		}

		String msgSumarioInd;
		String msgSumarioIndMin;
		OperacaoEntidade<TipoEnt> oprSumario;

		InfoGeracaoSumario(String msgEntSum, boolean ehEntSumGenMasc, boolean ehSumario, OperacaoEntidade<TipoEnt> operacaoSum) {
			super("Gerando ", formatarMsgSumario(msgEntSum + "s", ehEntSumGenMasc, ehSumario), formatarMsgStatus(ehSumario),
					" não foi" + formatarMsgStatus(ehSumario));
			this.oprSumario = operacaoSum;
			this.msgSumarioInd = formatarMsgSumarioInd(msgEntSum, ehEntSumGenMasc, ehSumario);
			this.msgSumarioIndMin = msgSumarioInd.toLowerCase();
		}
	}

	private class ProcessoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends ProcessoOperacao {

		InfoGeracaoSumario<TipoEnt> info;
		Collection<TipoEnt> clEnts;

		ProcessoGeracaoSumario(InfoGeracaoSumario<TipoEnt> info, Collection<TipoEnt> clEnts) throws ErroModelo {
			super(info.msgEntidade.startsWith("Validação"));
			this.info = info;
			this.clEnts = clEnts;
		}

		@SuppressWarnings("unchecked")
		ProcessoGeracaoSumario(InfoGeracaoSumario<? extends EntidadeEvn<?>> info, List<EntidadeEvn<?>> clEnts) throws ErroModelo {
			this((InfoGeracaoSumario<TipoEnt>) info, (Collection<TipoEnt>) clEnts);
		}

		@Override
		public void run() {
			super.run();

			Date inicio, fim = null;
			inicio = new Date();
			iniciarOperacao(inicio);
			if (!clEnts.isEmpty()) {
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
							sessao.saidaErro.append(info.msgSumarioIndMin).append("\"").append(ent.getNome());
							sessao.saidaErro.append("\" ***\n\n").append(Erro.toString(e)).append("\n");
						}
						logger.error("Erro {}{}\"{}\"", info.msgOperacaoMin, info.msgSumarioIndMin, ent.getNome(), e);
					}
					fim = new Date();
					synchronized (sessao.saidaPadrao) {
						adicionarRotuloTempo(fim);
						sessao.saidaPadrao.append(info.msgSumarioInd).append("\"");
						sessao.saidaPadrao.append(ent.getNome()).append("\"").append(msgStatus);
						adicionarTempoProcessamento(inicio, fim);
					}
					inicio = new Date();
				}
			}
			finalizarOperacao(fim);
		}
	}

	private class ProcessoCriacaoSumario<TipoEnt extends EntidadeEvn<?>> extends ProcessoGeracaoSumario<TipoEnt> {

		ProcessoCriacaoSumario(InfoGeracaoSumario<TipoEnt> info, Collection<TipoEnt> clEnts) throws ErroModelo {
			super(info, clEnts = new ArrayList<TipoEnt>(clEnts));
			String propSum = "sumario" + (validacaoRequerida ? "Validacao" : "");
			clEnts.removeIf((ent) -> {
				return ent.get(propSum) != null;
			});
		}
	}

	private class ProcessoExclusaoSumarios extends ProcessoOperacaoLote {

		ProcessoExclusaoSumarios() throws ErroModelo {
			super(false);
			info = new InfoOperacaoLote("Excluindo ", "Sumários", " excluídos com sucesso!", " não foram excluídos com sucesso!",
					(Usuario usu) -> fachada.excluirTodos(usu, ChavesModelo.SUMARIO));
		}
	}

	private class ProcessoExclusaoLogs extends ProcessoOperacaoLote {

		ProcessoExclusaoLogs() throws ErroModelo {
			super(false);
			info = new InfoOperacaoLote("Excluindo ", "Logs", " excluídos com sucesso!", " não foram excluídos com sucesso!",
					(Usuario usu) -> fachada.excluirLogsAntigos(usu));
		}
	}

	private class MapaOperacoes extends HashMap<String, InfoGeracaoSumarios> {
		{
			Operacao operSums;
			operSums = (Usuario usu) -> fachada.gerarSumarioInteresses(usu);
			put(OP_INTERESSES, new InfoGeracaoSumarios("interesses", true, true, operSums));
			operSums = (Usuario usu) -> fachada.gerarSumarioProjetos(usu);
			put(OP_PROJETOS, new InfoGeracaoSumarios("projetos", true, true, operSums));
			operSums = (Usuario usu) -> fachada.gerarValidacaoProjetos(usu);
			put(OP_VAL_PROJETOS, new InfoGeracaoSumarios("projetos", true, false, operSums));
			operSums = (Usuario usu) -> fachada.gerarValidacaoParcialProjetos(usu);
			put(OP_VAL_PAR_PROJETOS, new InfoGeracaoSumarios("projetos", true, false, operSums));
			operSums = (Usuario usu) -> fachada.gerarSumarioAcoes(usu);
			put(OP_ACOES, new InfoGeracaoSumarios("ações", false, true, operSums));
			operSums = (Usuario usu) -> fachada.gerarSumarioAcoesCalendario(usu);
			put(OP_ACOES_CALEND, new InfoGeracaoSumarios("ações", false, true, operSums));
			operSums = (Usuario usu) -> fachada.gerarValidacaoAcoes(usu);
			put(OP_VAL_ACOES, new InfoGeracaoSumarios("ações", false, false, operSums));
			operSums = (Usuario usu) -> fachada.gerarValidacaoParcialAcoes(usu);
			put(OP_VAL_PAR_ACOES, new InfoGeracaoSumarios("ações", false, false, operSums));
			operSums = (Usuario usu) -> fachada.gerarSumarioReferencias(usu);
			put(OP_REFERENCIAS, new InfoGeracaoSumarios("referências", false, true, operSums));
			operSums = (Usuario usu) -> fachada.gerarValidacaoReferencias(usu);
			put(OP_VAL_REFERENCIAS, new InfoGeracaoSumarios("referências", false, false, operSums));
			operSums = (Usuario usu) -> fachada.gerarValidacaoParcialReferencias(usu);
			put(OP_VAL_PAR_REFERENCIAS, new InfoGeracaoSumarios("referências", false, false, operSums));
		}
	};

	private class MapaOperacoesInd extends HashMap<String, InfoGeracaoSumario<? extends EntidadeEvn<?>>> {
		{
			OperacaoEntidade<Interesse> oprSumIntr;
			OperacaoEntidade<Projeto> oprSumPrj;
			oprSumIntr = (Usuario usu, Interesse intr) -> intr.setSumario(fachada.gerarSumarioInteresse(usu, intr.getId()));
			put(OP_INTERESSE, new InfoGeracaoSumario<Interesse>("interesse", true, true, oprSumIntr));
			oprSumPrj = (Usuario usu, Projeto prj) -> prj.setSumario(fachada.gerarSumarioInicialProjeto(usu, prj.getId()));
			put(OP_INI_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, true, oprSumPrj));
			oprSumPrj = (Usuario usu, Projeto prj) -> prj.setSumario(fachada.gerarSumarioProjeto(usu, prj.getId()));
			put(OP_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, true, oprSumPrj));
			oprSumPrj = (Usuario usu, Projeto prj) -> prj.setSumarioValidacao(fachada.gerarValidacaoProjeto(usu, prj.getId()));
			put(OP_VAL_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, false, oprSumPrj));
			oprSumPrj = (Usuario usu, Projeto prj) -> prj.setSumarioValidacao(fachada.gerarValidacaoParcialProjeto(usu, prj.getId()));
			put(OP_VAL_PAR_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, false, oprSumPrj));
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
		logger.error("Erro inesperado", e);
	}

	private void iniciarEntidades() throws ErroModelo {
		logger.trace("iniciando entidades (processos={})", sessao.contadorProcessos);
		Usuario usu = sessao.usuario;
		boolean forcarConsistencia = false;
		// Só checa mudanças se não houver processamento ativo
		if (sessao.contadorProcessos == 0) {
			consultarAtualizacoes(usu);
			forcarConsistencia = Boolean.parseBoolean(getParametroRequisicao("forcarConsistencia"));
		}
		carregarEntidades(usu, forcarConsistencia);
		logger.debug("entidades iniciadas");
	}

	private void validarEntidades() throws ErroModelo {
		if (!entidadesValidadas) {
			Usuario usu = sessao.usuario;
			fachada.validarEntidades(usu);
			entidadesValidadas = true;
			carregarEntidades(usu, false);
			logger.debug("entidades validadas");
		}
	}

	private void carregarEntidades(Usuario usu, boolean forcarConsistencia) throws ErroModelo {
		logger.trace("forçar consistência: {}", forcarConsistencia);
		if (forcarConsistencia) {
			invalidarCachesEntidades(usu);
		}
		clSums = fachada.consultarTodos(usu, ChavesModelo.SUMARIO);
		clIntrs = fachada.consultarTodos(usu, ChavesModelo.INTERESSE);
		clProjs = fachada.consultarTodos(usu, ChavesModelo.PROJETO);
		clAcoes = fachada.consultarTodos(usu, ChavesModelo.ACAO);
		clRefs = fachada.consultarTodos(usu, ChavesModelo.REFERENCIA);
		// Registra as inconsistências encontradas
		Collection<Sumario> clSumsInd = new ArrayList<Sumario>(clSums);
		clSumsInd.removeIf((sum) -> {
			String nome = sum.getNome();
			return !nome.startsWith("Sumário do") && !nome.startsWith("Validação do");
		});
		if (clSumsInd.size() != clProjs.size() * 2 + clIntrs.size()) {
			logger.warn("Quantidade de sumários individuais inconsistente: {} (intrs={}, projs={})", clSumsInd.size(), clIntrs.size(),
					clProjs.size());
		}
		if (forcarConsistencia) {
			fachada.excluirSumariosInvalidos(usu);
		}
		clLogs = fachada.consultarTodos(usu, ChavesModelo.LOG);
		// Garante a exibição de um log recém criado
		try {
			fachada.consultarPorNome(usu, usu.getLog().getNome());
		} catch (ErroItemNaoEncontrado e) {
			clLogs.add(usu.getLog());
		}
	}

	private void invalidarCachesEntidades(Usuario usu) throws ErroModelo {
		fachada.invalidarCache(usu, ChavesModelo.SUMARIO);
		fachada.invalidarCache(usu, ChavesModelo.INTERESSE);
		fachada.invalidarCache(usu, ChavesModelo.PROJETO);
		fachada.invalidarCache(usu, ChavesModelo.ACAO);
		fachada.invalidarCache(usu, ChavesModelo.REFERENCIA);
	}

	private void consultarAtualizacoes(Usuario usu) throws ErroModelo {
		CacheTags cacheTag = CacheTags.getCache(usu);
		CacheNotebooks cacheNtb = CacheNotebooks.getCache(usu);
		Collection<String> clModelos = new ArrayList<String>();
		clModelos.add(ChavesModelo.SUMARIO);
		clModelos.add(ChavesModelo.PROJETO);
		clModelos.add(ChavesModelo.ACAO);
		clModelos.add(ChavesModelo.REFERENCIA);
		clModelos.add(ChavesModelo.LOG);
		Collection<String> clIds = new ArrayList<String>();
		for (String modelo : clModelos) {
			String nomeRepo = fachada.consultarRepositorio(modelo);
			Notebook ntb = cacheNtb.get(nomeRepo);
			if (ntb != null) {
				clIds.add(ntb.getGuid());
			} else {
				for (Notebook ntbPilha : cacheNtb.consultarPorPilha(nomeRepo)) {
					clIds.add(ntbPilha.getGuid());
				}
			}
		}
		Collection<String> lsIdsAtu = ClienteEvn.consultarAtualizacoes(usu, clIds);
		if (lsIdsAtu.contains(ClienteEvn.ATU_TAGS)) {
			lsIdsAtu.remove(ClienteEvn.ATU_TAGS);
			cacheTag.invalidar();
			invalidarCachesEntidades(usu);
			logger.debug("atualizar Tags");
		}
		if (lsIdsAtu.contains(ClienteEvn.ATU_NOTEBOOKS)) {
			lsIdsAtu.remove(ClienteEvn.ATU_NOTEBOOKS);
			cacheNtb.invalidar();
			logger.debug("atualizar Notebooks");
		}
		String idLog = cacheNtb.get(fachada.consultarRepositorio(ChavesModelo.LOG)).getGuid();
		if (lsIdsAtu.contains(idLog)) {
			lsIdsAtu.remove(idLog);
			fachada.invalidarCache(usu, ChavesModelo.LOG);
			logger.debug("atualizar Logs");
		}
		if (lsIdsAtu.size() > 0) {
			invalidarCachesEntidades(usu);
			logger.debug("atualizar Entidades");
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
				String modelo = ChavesModelo.PACOTE + "." + tokens[tokens.length - 1].toUpperCase();

				InfoGeracaoSumario<? extends EntidadeEvn<?>> info = mpOperacoesEnt.get(oper);
				EntidadeEvn<?> ent = FabricaEntidade.getInstancia(new HashMap<String, Object>() {
					{
						put("id", id);
					}
				}, EntidadeEvn.class);
				ent = fachada.consultarPorChavePrimaria(sessao.usuario, modelo, ent);
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
			// Cria os sumários individuais, se necessário
			Collection<ProcessoOperacao> clProcessosCriacao = new ArrayList<ProcessoOperacao>();
			clProcessosCriacao.add(new ProcessoCriacaoSumario<Interesse>(mpOperacoesEnt.get(OP_INTERESSE), clIntrs));
			clProcessosCriacao.add(new ProcessoCriacaoSumario<Projeto>(mpOperacoesEnt.get(OP_VAL_PAR_PROJETO), clProjs));
			clProcessosCriacao.add(new ProcessoCriacaoSumario<Projeto>(mpOperacoesEnt.get(OP_INI_PROJETO), clProjs));
			for (ProcessoOperacao processo : clProcessosCriacao) {
				processo.iniciar(false);
			}
			// Cria/atualiza os sumários selecionados
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
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_VAL_PAR_PROJETOS)));
			}
			if (gerarValProjsInd) {
				clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesEnt.get(OP_VAL_PAR_PROJETO), clProjs));
			}
			if (gerarSumAcoesCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_ACOES)));
			}
			if (gerarValAcoesCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_VAL_PAR_ACOES)));
			}
			if (gerarSumRefsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_REFERENCIAS)));
			}
			if (gerarValRefsCol) {
				clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoes.get(OP_VAL_PAR_REFERENCIAS)));
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
