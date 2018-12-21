package meyn.cevn.controle;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import io.opencensus.common.Scope;
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
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
@ManagedBean(name = "console")
@RequestScoped
public class Console implements Serializable {

	private Sessao sessao;

	private final Fachada fachada = FabricaFachada.getFachada();
	private final Logger logger = LogManager.getLogger();

	private String getParametroRequisicao(String nome) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(nome);
	}

	private void gerarStatus() {
		if (sessao != null) {
			synchronized (sessao.saidaPadrao) {
				if (sessao.saidaPadrao.length() == 0) {
					if (sessao.saidaErro.length() > 0) {
						sessao.saidaPadrao.append("CONSOLE INICIADA COM ERRO\n\n");
					}
					sessao.saidaPadrao.append("STATUS EVERNOTE\n\n");
					sessao.saidaPadrao.append("Sumários (atual/esperado): ").append(clSums.size()).append("/")
					        .append(clIntrs.size() + clProjs.size() * 2 + CadastroSumario.QTD_SUMS_COLETIVOS).append("\n");
					sessao.saidaPadrao.append("Interesses: ").append(clIntrs.size()).append("\n");
					sessao.saidaPadrao.append("Projetos: ").append(clProjs.size()).append("\n");
					sessao.saidaPadrao.append("Ações: ").append(clAcoes.size()).append("\n");
					sessao.saidaPadrao.append("Referências: ").append(clRefs.size()).append("\n\n");
				}
			}
		}
	}

	private void gerarErroInesperado(Exception e) {
		if (sessao != null) {
			synchronized (sessao.saidaErro) {
				sessao.saidaErro.append("*** Erro inesperado ***\n\n");
				sessao.saidaErro.append(Erro.toString(e)).append("\n");
			}
		}
		logger.error("Erro inesperado", e);
	}

	private Usuario getUsuario() throws ErroModelo {
		ExternalContext contexto = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession sessao = (HttpSession) contexto.getSession(true);
		// GAE: busca no memcache e, caso não encontre, no Datastore
		Usuario usu = (Usuario) sessao.getAttribute(ChavesControle.USUARIO);
		if (usu == null) {
			usu = FabricaEntidade.getInstancia(Usuario.class);
			usu.setId(sessao.getId());
			sessao.setAttribute(ChavesControle.USUARIO, usu);
		}
		// Configura id do usuário para geração de log
		ThreadContext.put("usuario", usu.getId());
		logger.debug("usuario recuperado");
		// Conecta no serviço do Evernote
		fachada.conectar(usu);
		logger.debug("cliente conectado");
		return usu;
	}

	public Console() throws ErroModelo {
		try (Scope scp = Rastreador.iniciarEscopo("iniciarConsole")) {
			try {
				long tempo = System.currentTimeMillis();
				logger.debug("iniciando console");
				sessao = Sessao.getSessao(getUsuario());
				iniciarEntidades();
				tempo = System.currentTimeMillis() - tempo;
				logger.debug("console iniciada: {} ms", tempo);
			} catch (Exception e) {
				gerarErroInesperado(e);
			}
			gerarStatus();
		}
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

		Logger logger = LogManager.getLogger();

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
					if (m >= 1) {
						tempoProc = String.format("%.0f m %.0f s", m, s);
					} else {
						tempoProc = String.format("%.1f s", s);
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
		gerarStatus();
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

	private abstract static class InfoOperacao {

		static String formatarTxtEnt(String nomeEntPlr, String nomeSubEntPlr, boolean ehSubEntGenMasc) {
			return nomeEntPlr + " d" + (ehSubEntGenMasc ? "os " : "as ") + nomeSubEntPlr;
		}

		String txtOperacao;
		String txtOperacaoMin;
		String txtEntidade;
		String txtEntidadeMin;
		String txtStatusSucesso;
		String txtStatusErro;

		InfoOperacao(String txtOperacao, String txtEntidade, String txtStatusSucesso, String txtStatusErro) {
			this.txtOperacao = txtOperacao;
			this.txtOperacaoMin = txtOperacao.toLowerCase();
			this.txtEntidade = txtEntidade;
			this.txtEntidadeMin = txtEntidade.toLowerCase();
			this.txtStatusSucesso = txtStatusSucesso;
			this.txtStatusErro = txtStatusErro;
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

		void gerarRotuloTempo(Date tempo) {
			sessao.saidaPadrao.append(FORMATO_DATA_HORA.format(tempo));
		}

		void gerarTempoProcessamento(Date inicio, Date fim) {
			sessao.saidaPadrao.append(" (");
			sessao.saidaPadrao.append(String.format("%.1f", (float) (fim.getTime() - inicio.getTime()) / 1000));
			sessao.saidaPadrao.append(" s)\n");
		}
	}

	private static class InfoOperacaoLote extends InfoOperacao {

		Operacao oprLote;

		InfoOperacaoLote(String txtOperacao, String txtEntidade, String txtStatusSucesso, String txtStatusErro, Operacao oprLote) {
			super(txtOperacao, txtEntidade, txtStatusSucesso, txtStatusErro);
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
				gerarRotuloTempo(inicio);
				sessao.saidaPadrao.append(info.txtOperacao).append(" ").append(info.txtEntidadeMin).append("...\n");
			}
			String txtStatus = info.txtStatusSucesso;
			try {
				info.oprLote.executar(sessao.usuario);
			} catch (Exception e) {
				txtStatus = info.txtStatusErro;
				synchronized (sessao.saidaErro) {
					sessao.saidaErro.append("*** Erro ").append(info.txtOperacaoMin).append(" ");
					sessao.saidaErro.append(info.txtEntidadeMin).append(" ***\n\n");
					sessao.saidaErro.append(Erro.toString(e)).append("\n");
				}
				logger.error("Erro {} {}", info.txtOperacaoMin, info.txtEntidadeMin, e);
			}
			fim = new Date();
			synchronized (sessao.saidaPadrao) {
				gerarRotuloTempo(fim);
				sessao.saidaPadrao.append(info.txtEntidade).append(" ").append(txtStatus);
				gerarTempoProcessamento(inicio, fim);
			}
			finalizarOperacao(fim);
		}
	}

	private static class InfoGeracaoSumarios extends InfoOperacaoLote {

		InfoGeracaoSumarios(String nomeSubEntPlr, boolean ehSubEntGenMasc, Operacao operacao) {
			super("Gerando", formatarTxtEnt("Sumários coletivos", nomeSubEntPlr, ehSubEntGenMasc), "gerados com sucesso",
			        "não foram gerados com sucesso", operacao);
		}
	}

	private static class InfoGeracaoValidacoes extends InfoOperacaoLote {

		InfoGeracaoValidacoes(String nomeSubEntPlr, boolean ehSubEntGenMasc, Operacao operacao) {
			super("Gerando", formatarTxtEnt("Validações coletivas", nomeSubEntPlr, ehSubEntGenMasc), "geradas com sucesso",
			        "não foram geradas com sucesso", operacao);
		}
	}

	private class ProcessoGeracaoSumarios extends ProcessoOperacaoLote {

		ProcessoGeracaoSumarios(InfoOperacaoLote info) throws ErroModelo {
			super(info, InfoGeracaoValidacoes.class.isInstance(info));
		}
	}

	@FunctionalInterface
	private static interface OperacaoEntidade<TipoEnt extends EntidadeEvn<?>> {
		void executar(Usuario usu, TipoEnt ent) throws ErroModelo;
	}

	private static class InfoOperacaoEntidade<TipoEnt extends EntidadeEvn<?>> extends InfoOperacao {

		static String formatarTxtEntInd(String nomeEnt, String nomeSubEnt, boolean ehSubEntGenMasc) {
			return nomeEnt + " d" + (ehSubEntGenMasc ? "o " : "a ") + nomeSubEnt;
		}

		String txtEntidadeInd;
		String txtEntidadeIndMin;
		OperacaoEntidade<TipoEnt> oprEntidade;

		InfoOperacaoEntidade(String txtOperacao, String txtEntidade, String txtEntidadeInd, String txtStatusSucesso, String txtStatusErro,
		        OperacaoEntidade<TipoEnt> oprEntidade) {
			super(txtOperacao, txtEntidade, txtStatusSucesso, txtStatusErro);
			this.txtEntidadeInd = txtEntidadeInd;
			this.txtEntidadeIndMin = txtEntidadeInd.toLowerCase();
			this.oprEntidade = oprEntidade;
		}
	}

	private class ProcessoOperacaoEntidade<TipoEnt extends EntidadeEvn<?>> extends ProcessoOperacao {

		InfoOperacaoEntidade<TipoEnt> info;
		Collection<TipoEnt> clEnts;

		ProcessoOperacaoEntidade(boolean requerValidacao, Collection<TipoEnt> clEnts) throws ErroModelo {
			this(requerValidacao, null, clEnts);
		}

		ProcessoOperacaoEntidade(boolean requerValidacao, InfoOperacaoEntidade<TipoEnt> info, Collection<TipoEnt> clEnts)
		        throws ErroModelo {
			super(requerValidacao);
			this.info = info;
			this.clEnts = clEnts;
		}

		@Override
		public void run() {
			super.run();

			Date inicio, fim;
			inicio = fim = new Date();
			iniciarOperacao(inicio);
			if (!clEnts.isEmpty()) {
				synchronized (sessao.saidaPadrao) {
					gerarRotuloTempo(inicio);
					sessao.saidaPadrao.append(info.txtOperacao).append(" ").append(info.txtEntidadeMin).append("...\n");
				}
				for (TipoEnt ent : clEnts) {
					String txtStatus = info.txtStatusSucesso;
					try {
						info.oprEntidade.executar(sessao.usuario, ent);
					} catch (Exception e) {
						txtStatus = info.txtStatusErro;
						synchronized (sessao.saidaErro) {
							sessao.saidaErro.append("*** Erro ").append(info.txtOperacaoMin).append(" ");
							sessao.saidaErro.append(info.txtEntidadeIndMin).append(" \"").append(ent.getNome());
							sessao.saidaErro.append("\" ***\n\n").append(Erro.toString(e)).append("\n");
						}
						logger.error("Erro {} {} \"{}\"", info.txtOperacaoMin, info.txtEntidadeIndMin, ent.getNome(), e);
					}
					fim = new Date();
					synchronized (sessao.saidaPadrao) {
						gerarRotuloTempo(fim);
						sessao.saidaPadrao.append(info.txtEntidadeInd).append(" \"");
						sessao.saidaPadrao.append(ent.getNome()).append("\" ").append(txtStatus);
						gerarTempoProcessamento(inicio, fim);
					}
					inicio = new Date();
				}
			}
			finalizarOperacao(fim);
		}
	}

	private static class InfoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends InfoOperacaoEntidade<TipoEnt> {

		InfoGeracaoSumario(String nomeSubEnt, boolean ehSubEntGenMasc, OperacaoEntidade<TipoEnt> oprSumario) {
			super("Gerando", formatarTxtEnt("Sumários individuais", nomeSubEnt + "s", ehSubEntGenMasc),
			        formatarTxtEntInd("Sumário", nomeSubEnt, ehSubEntGenMasc), "gerado com sucesso", "não foi gerado com sucesso",
			        oprSumario);
		}
	}

	private static class InfoGeracaoValidacao<TipoEnt extends EntidadeEvn<?>> extends InfoOperacaoEntidade<TipoEnt> {

		InfoGeracaoValidacao(String nomeSubEnt, boolean ehSubEntGenMasc, OperacaoEntidade<TipoEnt> oprSumario) {
			super("Gerando", formatarTxtEnt("Validações individuais", nomeSubEnt + "s", ehSubEntGenMasc),
			        formatarTxtEntInd("Validação", nomeSubEnt, ehSubEntGenMasc), "gerada com sucesso", "não foi gerada com sucesso",
			        oprSumario);
		}
	}

	private class ProcessoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends ProcessoOperacaoEntidade<TipoEnt> {

		ProcessoGeracaoSumario(InfoOperacaoEntidade<TipoEnt> info, Collection<TipoEnt> clEnts) throws ErroModelo {
			super(InfoGeracaoValidacao.class.isInstance(info), info, clEnts);
		}

		@SuppressWarnings("unchecked")
		ProcessoGeracaoSumario(InfoOperacaoEntidade<? extends EntidadeEvn<?>> info, List<? extends EntidadeEvn<?>> clEnts)
		        throws ErroModelo {
			this((InfoOperacaoEntidade<TipoEnt>) info, (Collection<TipoEnt>) clEnts);
		}
	}

	private class ProcessoCriacaoSumario<TipoEnt extends EntidadeEvn<?>> extends ProcessoGeracaoSumario<TipoEnt> {

		ProcessoCriacaoSumario(InfoOperacaoEntidade<TipoEnt> info, Collection<TipoEnt> clEnts) throws ErroModelo {
			super(info, clEnts = new ArrayList<TipoEnt>(clEnts));
			String propSum = "sumario" + (validacaoRequerida ? "Validacao" : "");
			clEnts.removeIf((ent) -> {
				return ent.get(propSum) != null;
			});
		}
	}

	private class ProcessoExclusaoSumario extends ProcessoOperacaoEntidade<Sumario> {

		ProcessoExclusaoSumario(Collection<Sumario> clSums) throws ErroModelo {
			super(false, clSums);
			this.info = new InfoOperacaoEntidade<Sumario>("Excluindo", "Sumários", "Sumário", "excluído com sucesso!",
			        "não foi excluído com sucesso!", (Usuario usu, Sumario sum) -> fachada.excluir(usu, ChavesModelo.SUMARIO, sum));
		}
	}

	private class ProcessoExclusaoLogs extends ProcessoOperacaoLote {

		ProcessoExclusaoLogs() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Excluindo", "Logs", "excluídos com sucesso!", "não foram excluídos com sucesso!",
			        (Usuario usu) -> fachada.excluirLogsAntigos(usu));
		}
	}

	private class MapaOperacoesLote extends HashMap<String, InfoOperacaoLote> {
		{
			Operacao oper;
			oper = (Usuario usu) -> fachada.gerarSumarioInteresses(usu);
			put(OP_INTERESSES, new InfoGeracaoSumarios("interesses", true, oper));
			oper = (Usuario usu) -> fachada.gerarSumarioProjetos(usu);
			put(OP_PROJETOS, new InfoGeracaoSumarios("projetos", true, oper));
			oper = (Usuario usu) -> fachada.gerarValidacaoProjetos(usu);
			put(OP_VAL_PROJETOS, new InfoGeracaoValidacoes("projetos", true, oper));
			oper = (Usuario usu) -> fachada.gerarValidacaoParcialProjetos(usu);
			put(OP_VAL_PAR_PROJETOS, new InfoGeracaoValidacoes("projetos", true, oper));
			oper = (Usuario usu) -> fachada.gerarSumarioAcoes(usu);
			put(OP_ACOES, new InfoGeracaoSumarios("ações", false, oper));
			oper = (Usuario usu) -> fachada.gerarSumarioAcoesCalendario(usu);
			put(OP_ACOES_CALEND, new InfoGeracaoSumarios("ações", false, oper));
			oper = (Usuario usu) -> fachada.gerarValidacaoAcoes(usu);
			put(OP_VAL_ACOES, new InfoGeracaoValidacoes("ações", false, oper));
			oper = (Usuario usu) -> fachada.gerarValidacaoParcialAcoes(usu);
			put(OP_VAL_PAR_ACOES, new InfoGeracaoValidacoes("ações", false, oper));
			oper = (Usuario usu) -> fachada.gerarSumarioReferencias(usu);
			put(OP_REFERENCIAS, new InfoGeracaoSumarios("referências", false, oper));
			oper = (Usuario usu) -> fachada.gerarValidacaoReferencias(usu);
			put(OP_VAL_REFERENCIAS, new InfoGeracaoValidacoes("referências", false, oper));
			oper = (Usuario usu) -> fachada.gerarValidacaoParcialReferencias(usu);
			put(OP_VAL_PAR_REFERENCIAS, new InfoGeracaoValidacoes("referências", false, oper));
		}
	};

	private class MapaOperacoesEnt extends HashMap<String, InfoOperacaoEntidade<? extends EntidadeEvn<?>>> {
		{
			OperacaoEntidade<Interesse> oprSumIntr;
			OperacaoEntidade<Projeto> oprSumProj;
			oprSumIntr = (Usuario usu, Interesse intr) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.INTERESSE, intr)
			        .set("sumario", fachada.gerarSumarioInteresse(usu, intr.getId()));
			put(OP_INTERESSE, new InfoGeracaoSumario<Interesse>("interesse", true, oprSumIntr));
			oprSumProj = (Usuario usu, Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj).set("sumario",
			        fachada.gerarSumarioInicialProjeto(usu, proj.getId()));
			put(OP_INI_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, oprSumProj));
			oprSumProj = (Usuario usu, Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj).set("sumario",
			        fachada.gerarSumarioProjeto(usu, proj.getId()));
			put(OP_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", true, oprSumProj));
			oprSumProj = (Usuario usu, Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj)
			        .set("sumarioValidacao", fachada.gerarValidacaoProjeto(usu, proj.getId()));
			put(OP_VAL_PROJETO, new InfoGeracaoValidacao<Projeto>("projeto", true, oprSumProj));
			oprSumProj = (Usuario usu, Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj)
			        .set("sumarioValidacao", fachada.gerarValidacaoParcialProjeto(usu, proj.getId()));
			put(OP_VAL_PAR_PROJETO, new InfoGeracaoValidacao<Projeto>("projeto", true, oprSumProj));
		}

		@SuppressWarnings("unchecked")
		<TipoEnt extends EntidadeEvn<?>> InfoOperacaoEntidade<TipoEnt> get(String key) {
			return (InfoOperacaoEntidade<TipoEnt>) super.get(key);
		}
	};

	private MapaOperacoesLote mpOperacoesLote = new MapaOperacoesLote();
	private MapaOperacoesEnt mpOperacoesEnt = new MapaOperacoesEnt();

	//// MANUTENÇÃO ENTIDADES ////

	boolean entidadesValidadas = false;

	private void iniciarEntidades() throws ErroModelo {
		logger.trace("iniciando entidades (processos={})", sessao.contadorProcessos);
		Usuario usu = sessao.usuario;
		// Carrega todas as entidades se não houver processamento ativo
		if (sessao.contadorProcessos == 0) {
			fachada.verificarAtualizacoesServidor(usu);
			carregarEntidades(usu, Boolean.parseBoolean(getParametroRequisicao("forcarCarregamento")));
		} else {
			carregarSumariosELogs(usu);
		}
		logger.debug("entidades iniciadas");
	}

	private void validarEntidades() throws ErroModelo {
		if (!entidadesValidadas) {
			Usuario usu = sessao.usuario;
			fachada.desatualizarCachesParaValidacao(usu);
			carregarEntidades(usu, false);
			entidadesValidadas = true;
			logger.debug("entidades validadas");
		}
	}

	private void carregarEntidades(Usuario usu, boolean forcar) throws ErroModelo {
		logger.trace("forçar carregamento: {}", forcar);
		if (forcar) {
			fachada.desatualizarCaches(usu);
		}
		clSums = fachada.consultarTodos(usu, ChavesModelo.SUMARIO);
		clIntrs = fachada.consultarTodos(usu, ChavesModelo.INTERESSE);
		clProjs = fachada.consultarTodos(usu, ChavesModelo.PROJETO);
		clAcoes = fachada.consultarTodos(usu, ChavesModelo.ACAO);
		clRefs = fachada.consultarTodos(usu, ChavesModelo.REFERENCIA);
		clLogs = fachada.consultarTodos(usu, ChavesModelo.LOG);
		// Garante a exibição de um log recém criado
		try {
			fachada.consultarLogPorNome(usu, usu.getLog().getNome());
		} catch (ErroItemNaoEncontrado e) {
			clLogs.add(usu.getLog());
		}
	}

	private void carregarSumariosELogs(Usuario usu) throws ErroModelo {
		clSums = fachada.consultarTodos(usu, ChavesModelo.SUMARIO);
		clLogs = fachada.consultarTodos(usu, ChavesModelo.LOG);
	}

	public String getResultadoGeracaoSumariosParametro() {
		try (Scope scp = Rastreador.iniciarEscopo("gerarSumariosParametro")) {
			try {
				sessao.limparSaida();
				String oper = getParametroRequisicao("sumario");
				String id = getParametroRequisicao("id");
				if (id == null) {
					new ProcessoGeracaoSumarios(mpOperacoesLote.get(oper)).iniciar(false);
				} else {
					String[] partesOper = oper.split("_");
					String modelo = ChavesModelo.PACOTE + "." + partesOper[partesOper.length - 1].toUpperCase();
					InfoOperacaoEntidade<? extends EntidadeEvn<?>> info = mpOperacoesEnt.get(oper);
					EntidadeEvn<?> ent = FabricaEntidade.getInstancia(EntidadeEvn.class);
					ent.setId(id);
					ent = fachada.consultarPorChavePrimaria(sessao.usuario, modelo, ent);
					new ProcessoGeracaoSumario<EntidadeEvn<?>>(info, Arrays.asList(ent)).iniciar(false);
				}
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
			return sessao.getStatusProcessamento();
		}
	}

	public void gerarSumarios() {
		try (Scope scp = Rastreador.iniciarEscopo("gerarSumarios")) {
			try {
				sessao.zerarTempoProcessamento();
				// Cria/atualiza os sumários selecionados
				Collection<ProcessoOperacao> clProcessos = new ArrayList<ProcessoOperacao>();
				if (gerarSumIntrsCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_INTERESSES)));
				}
				if (gerarSumIntrsInd) {
					clProcessos.add(new ProcessoGeracaoSumario<Interesse>(mpOperacoesEnt.get(OP_INTERESSE), clIntrs));
				}
				if (gerarSumProjsCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_PROJETOS)));
				}
				if (gerarSumProjsInd) {
					clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesEnt.get(OP_PROJETO), clProjs));
				}
				if (gerarValProjsCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_VAL_PAR_PROJETOS)));
				}
				if (gerarValProjsInd) {
					clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesEnt.get(OP_VAL_PAR_PROJETO), clProjs));
				}
				if (gerarSumAcoesCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_ACOES)));
				}
				if (gerarValAcoesCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_VAL_PAR_ACOES)));
				}
				if (gerarSumRefsCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_REFERENCIAS)));
				}
				if (gerarValRefsCol) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesLote.get(OP_VAL_PAR_REFERENCIAS)));
				}
				for (ProcessoOperacao processo : clProcessos) {
					processo.iniciar(true);
				}
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public void forcarConsistenciaSumarios() throws ErroModelo {
		try (Scope scp = Rastreador.iniciarEscopo("forcarConsistenciaSumarios")) {
			try {
				sessao.zerarTempoProcessamento();
				List<ProcessoOperacao> lsProcessos = new ArrayList<ProcessoOperacao>();
				// Exclusão dos sumários inválidos
				lsProcessos.add(new ProcessoExclusaoSumario(fachada.consultarSumariosInvalidos(sessao.usuario)));
				// Criação dos sumários individuais faltantes
				lsProcessos.add(new ProcessoCriacaoSumario<Interesse>(mpOperacoesEnt.get(OP_INTERESSE), clIntrs));
				lsProcessos.add(new ProcessoCriacaoSumario<Projeto>(mpOperacoesEnt.get(OP_VAL_PAR_PROJETO), clProjs));
				lsProcessos.add(new ProcessoCriacaoSumario<Projeto>(mpOperacoesEnt.get(OP_INI_PROJETO), clProjs));
				// Executa um novo thread para não bloquear a tela, pois a criação das
				// validações e sumários de projeto precisa ser executada na ordem e pode ser
				// demorada
				new Thread() {
					public void run() {
						lsProcessos.get(0).iniciar(true);
						lsProcessos.get(1).iniciar(true);
						lsProcessos.get(2).iniciar(false);
						lsProcessos.get(3).iniciar(false);
					}
				}.start();
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public void excluirSumarios() {
		try (Scope scp = Rastreador.iniciarEscopo("excluirSumarios")) {
			try {
				sessao.zerarTempoProcessamento();
				new ProcessoExclusaoSumario(clSums).iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public void excluirLogs() {
		try (Scope scp = Rastreador.iniciarEscopo("excluirLogs")) {
			try {
				sessao.zerarTempoProcessamento();
				new ProcessoExclusaoLogs().iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}
}
