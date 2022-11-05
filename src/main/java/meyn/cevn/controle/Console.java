package meyn.cevn.controle;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import io.opencensus.common.Scope;
import meyn.cevn.SessaoEvn;
import meyn.cevn.log4j.ThreadEvn;
import meyn.cevn.modelo.FabricaFachada;
import meyn.cevn.modelo.FachadaEvn;
import meyn.cevn.modelo.cliente.ErroClienteNaoConectado;
import meyn.cevn.modelo.entidade.EntidadeEvn;
import meyn.cevn.modelo.entidade.acao.Acao;
import meyn.cevn.modelo.entidade.interesse.Interesse;
import meyn.cevn.modelo.entidade.log.Log;
import meyn.cevn.modelo.entidade.projeto.Projeto;
import meyn.cevn.modelo.entidade.referencia.Referencia;
import meyn.cevn.modelo.entidade.sumario.ChavesSumarioAPI;
import meyn.cevn.modelo.entidade.sumario.Sumario;
import meyn.cevn.modelo.entidade.usuario.Usuario;
import meyn.cevn.util.SimpleDateFormatEvn;
import meyn.util.Erro;
import meyn.util.ErroExecucao;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
@ManagedBean(name = "console")
@RequestScoped
public class Console implements Serializable {

	//// FACHADA ////

	private static FachadaEvn fachada;

	static {
		try {
			fachada = FabricaFachada.getFachada();
		} catch (ErroModelo e) {
			LogManager.getLogger().error("erro iniciando fachada", e);
		}
	}

	//// SESSÃO ////

	public static class Sessao extends SessaoEvn {

		static Sessao recuperarSessao(Usuario usu) {
			return recuperarSessao(usu, Sessao.class);
		}

		// Monitor para sincronização
		final Object semaforo = new Object();

		boolean primeiroAcesso = true;

		boolean novoProcessamento = true;
		String tempoProcessamento = "";
		Date inicioProcessamento = null;
		Date fimProcessamento = null;
		StringBuilder saidaPadrao = new StringBuilder();
		StringBuilder saidaErro = new StringBuilder();
		int contadorOperacoes = 0;
		int estrategiaCarregamento;
		int estrategiaCarregamentoEncadeada;

		Collection<Sumario> clSums;
		Collection<Interesse> clIntrs;
		Collection<Projeto> clProjs;
		Collection<Acao> clAcoes;
		Collection<Referencia> clRefs;
		Collection<Log> clLogs;

		public Sessao(Usuario usu) throws ErroModelo {
			super(usu);
		}

		@Override
		protected void processarExpiracao() throws ErroModelo {
			fachada.desativarServicoLog(usuario);
		}
	}

	public String getTempoProcessamento() {
		if (sessao.fimProcessamento != null) {
			long ini = sessao.inicioProcessamento.getTime();
			long fim = sessao.fimProcessamento.getTime();
			if (fim >= ini) {
				float tempo = (float)(fim - ini);
				Float m = tempo / 60000;
				Float s = (tempo / 1000) % 60;
				if (m >= 1) {
					sessao.tempoProcessamento = String.format("%.0f m %.0f s", m, s);
				} else {
					sessao.tempoProcessamento = String.format("%.1f s", s);
				}
			}
		}
		return sessao.tempoProcessamento;
	}

	public void limparTempoProcessamento() {
		sessao.tempoProcessamento = "";
		sessao.inicioProcessamento = sessao.fimProcessamento = null;
	}

	public String getResultadoProcessamento() {
		return sessao.saidaErro.length() == 0 ? "ok" : "erro";
	}

	public String getUrlLog() {
		return sessao.usuario.getLog().getURL();
	}

	public String getSaidaPadrao() {
		return sessao.saidaPadrao.toString();
	}

	public String getSaidaErro() {
		return sessao.saidaErro.toString();
	}

	public void limparSaida() {
		synchronized (sessao.saidaPadrao) {
			sessao.saidaPadrao.setLength(0);
		}
		synchronized (sessao.saidaErro) {
			sessao.saidaErro.setLength(0);
		}
	}

	//// INICIAÇÃO CONSOLE ////

	private static final String URI_CONSOLE = "/faces/console.xhtml";

	private final String urlGeracao;

	private transient HttpServletRequest request;
	private transient Sessao sessao;

	private transient ProcessoCarregamentoEntidades carregamentoEmCurso = null;
	private transient ProcessoConsistenciaSumarios consistenciaSumariosEmCurso = null;

	private transient Logger logger = LogManager.getLogger();

	private boolean consoleConfigurada = false;
	
	private String getParametroRequisicao(String nome) {
		return request.getParameter(nome);
	}

	public Console() throws ErroModelo {
		request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		urlGeracao = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + URI_CONSOLE + "?"
				+ QS_GERACAO;
		iniciarConsole();
	}

	private void iniciarConsole() throws ErroModelo {
		try (Scope scp = RastreadorEvn.iniciarEscopo("iniciarConsole")) {
			try {
				long tempo = System.currentTimeMillis();
				logger.debug("iniciando console");
				sessao = Sessao.recuperarSessao(recuperarUsuarioConectado());
				if (!isProcessando()) {
					if (sessao.novoProcessamento) {
						limparTempoProcessamento();
					}
					iniciarOperacoesSumario();
					iniciarEntidades();
					consoleConfigurada = true;
				}
				logger.trace("status processamento: operações: {}", sessao.contadorOperacoes);
				logger.trace("status processamento: fila: {}", getTamanhoFilaServidor());
				logger.debug("console iniciada: {} ms", System.currentTimeMillis() - tempo);
			} catch (Exception e) {
				gerarErroInesperado(e);
			}
		}
	}

	// Reinicia a console com a nova estratégia e o restante dos parâmetros
	// originais
	private void reiniciarConsole(int estrategiaCarregamento) {
		try {
			ExternalContext ctx = FacesContext.getCurrentInstance().getExternalContext();
			Map<String, List<String>> mpParams = new HashMap<>();
			mpParams.put(PARAM_ESTRATEGIA_CARREGAMENTO, Arrays.asList(String.valueOf(estrategiaCarregamento)));
			for (Entry<String, String> param : ctx.getRequestParameterMap().entrySet()) {
				mpParams.putIfAbsent(param.getKey(), Arrays.asList(param.getValue()));
			}
			ctx.redirect(ctx.encodeRedirectURL(request.getRequestURL().toString(), mpParams));
		} catch (IOException e) {
			throw new ErroExecucao("erro reiniciando console", e);
		}
	}

	private Usuario recuperarUsuarioConectado() throws ErroModelo {
		// GAE: busca no memcache e, caso não encontre, no Datastore
		Usuario usu = (Usuario) request.getSession().getAttribute(ChavesControle.USUARIO);
		if (usu == null) {
			usu = recuperarUsuarioSessaoHTTP();
		}
		// Configura id do usuário para geração de log
		ThreadContext.put(ThreadEvn.ID_USUARIO, usu.getId());
		logger.debug("usuário recuperado");
		// Conecta no serviço do Evernote
		if (usu.getLog() == null) {
			conectarUsuario(usu);
		}
		logger.debug("cliente conectado");
		return usu;
	}

	// Evita concorrência na recuperação inicial do usuário partir da sessão HTTP
	private Usuario recuperarUsuarioSessaoHTTP() {
		synchronized (Console.class) {
			HttpSession sessaoHTTP = request.getSession();
			Usuario usu = (Usuario) sessaoHTTP.getAttribute(ChavesControle.USUARIO);
			if (usu == null) {
				usu = FabricaEntidade.instanciar(Usuario.class);
				usu.setId(sessaoHTTP.getId());
				usu.setTempoMaximoInatividade(sessaoHTTP.getMaxInactiveInterval() * 1000);
				usu.setURLGeracao(urlGeracao);
				sessaoHTTP.setAttribute(ChavesControle.USUARIO, usu);
			}
			return usu;
		}
	}

	// Evita concorrência na conexão inicial do usuário
	private void conectarUsuario(Usuario usu) throws ErroModelo {
		synchronized (fachada) {
			if (usu.getLog() == null) {
				fachada.conectar(usu);
				usu.setLog(fachada.gerarLog(usu));
			}
		}
	}

	private void iniciarEntidades() throws ErroModelo {
		// Carrega entidades se não houver processamento ativo
		logger.debug("iniciando entidades");
		if (sessao.novoProcessamento) {
			obterEstrategiaCarregamentoInicial();
		} else {
			obterEstrategiaCarregamentoEncadeada();
		}
		// Força carregamento no primeiro acesso
		if (sessao.primeiroAcesso && !isCarregamentoForcado() && !isNaoCarregar()) {
			reiniciarConsole(ES_FORCAR_CARREG_ASSINC_ST);
		} else {
			// Mantém status de novo processamento enquanto houver encadeamento
			sessao.novoProcessamento = !definirEstrategiaCarregamentoEncadeada();
			carregamentoEmCurso = new ProcessoCarregamentoEntidades();
			carregamentoEmCurso.iniciar();
			// Verifica consistência dos sumários no primeiro acesso
			if (sessao.primeiroAcesso) {
				consistenciaSumariosEmCurso = new ProcessoConsistenciaSumarios();
				consistenciaSumariosEmCurso.iniciar(true);
				sessao.primeiroAcesso = false;
			}
		}
		logger.debug("entidades iniciadas");
	}

	private void validarEntidades() throws ErroModelo {
		if (!entidadesValidadas) {
			verificarAtualizacoesEntidades(true, true);
			entidadesValidadas = true;
			logger.debug("entidades validadas");
		}
	}

	private void verificarAtualizacoesEntidades(boolean forcarAtualizacao, boolean validarEntidades) throws ErroModelo {
		logger.trace("forçar atualização cache: {}", forcarAtualizacao);
		Usuario usu = sessao.usuario;
		boolean atualizar;
		boolean atualizarLog;
		int statusCacheFachada;
		// Força atualização do cache da fachada com validação das entidades
		if (forcarAtualizacao) {
			fachada.desatualizarCache(usu, validarEntidades);
			atualizarLog = atualizar = true;
		} else {
			// Verifica necessidade de atualizar cache da sessão
			statusCacheFachada = fachada.verificarStatusCache(usu);
			atualizar = statusCacheFachada == FachadaEvn.STATUS_CACHE_DESATUALIZADO;
			atualizarLog = atualizar || statusCacheFachada == FachadaEvn.STATUS_CACHE_LOG_DESATUALIZADO;
			logger.trace("status cache fachada: {}", statusCacheFachada);
		}
		if (atualizar) {
			sessao.clSums = fachada.consultarTodos(usu, Sumario.class);
			sessao.clIntrs = fachada.consultarTodos(usu, Interesse.class);
			sessao.clProjs = fachada.consultarTodos(usu, Projeto.class);
			sessao.clAcoes = fachada.consultarTodos(usu, Acao.class);
			sessao.clRefs = fachada.consultarTodos(usu, Referencia.class);
			logger.trace("cache da sessão atualizado (entidades)");
		}
		if (atualizarLog) {
			sessao.clLogs = fachada.consultarTodos(usu, Log.class);
			// Garante a exibição de um log recém criado
			try {
				fachada.consultarLog(usu);
			} catch (ErroItemNaoEncontrado e) {
				sessao.clLogs.add(usu.getLog());
			}
			logger.trace("cache da sessão atualizado (logs)");
		}
		carregarEntidades();
	}

	private void carregarEntidades() {
		clSums = sessao.clSums;
		clIntrs = sessao.clIntrs;
		clProjs = sessao.clProjs;
		clAcoes = sessao.clAcoes;
		clRefs = sessao.clRefs;
		clLogs = sessao.clLogs;
	}

	private void gerarErroInesperado(Exception e) {
		if (sessao != null) {
			synchronized (sessao.saidaErro) {
				sessao.saidaErro.append("*** Erro inesperado ***\n\n");
				sessao.saidaErro.append(Erro.toString(e)).append("\n");
			}
		}
		logger.error("erro inesperado", e);
	}

	//// CARREGAMENTO ENTIDADES ////

	private static final String PARAM_ESTRATEGIA_CARREGAMENTO = "estrategiaCarregamento";

	// Encadeamento de estratégias:
	// a) Primeiro carregamento após atualização assíncrona com status deve ser
	// síncrono e sem status, pois assume-se que nenhuma operação foi realizada e
	// nada precisa ser verificado no servidor.
	// b) Primeiro carregamento após atualização síncrona com status deve ser
	// assíncrono, pois assume-se que alguma operação foi realizada e alterações
	// podem ter sido feitas no servidor.

	// Verificar atualizações no servidor e carregar assíncrono com status
	private static final int ES_VERIF_CARREG_ASSINC_ST = 0;
	// Forçar atualizações e carregar assíncrono com status
	private static final int ES_FORCAR_CARREG_ASSINC_ST = 1;
	// Verificar atualizações no servidor e carregar síncrono com status
	private static final int ES_VERIF_CARREG_SINC_ST = 2;
	// Verificar atualizações no servidor e carregar síncrono sem status
	private static final int ES_VERIF_CARREG_SINC = 4;
	// Forçar atualizações e carregar assíncrono com status
	private static final int ES_FORCAR_CARREG_SINC = 5;
	// Carregar síncrono sem status
	private static final int ES_CARREG_SINC = 6;
	// Não carregar
	private static final int ES_NAO_CARREG = 7;

	private static final Map<Integer, Integer> MP_ESTRATEGIA_ENCAD = new HashMap<>();

	static {
		MP_ESTRATEGIA_ENCAD.put(ES_VERIF_CARREG_ASSINC_ST, ES_CARREG_SINC);
		MP_ESTRATEGIA_ENCAD.put(ES_FORCAR_CARREG_ASSINC_ST, ES_CARREG_SINC);
		MP_ESTRATEGIA_ENCAD.put(ES_VERIF_CARREG_SINC_ST, ES_VERIF_CARREG_ASSINC_ST);
	}

	private boolean entidadesValidadas = false;

	boolean isCarregamentoAssincrono() {
		return sessao.estrategiaCarregamento == ES_VERIF_CARREG_ASSINC_ST
				|| sessao.estrategiaCarregamento == ES_FORCAR_CARREG_ASSINC_ST;
	}

	boolean isCarregamentoComStatus() {
		return sessao.estrategiaCarregamento == ES_VERIF_CARREG_ASSINC_ST
				|| sessao.estrategiaCarregamento == ES_FORCAR_CARREG_ASSINC_ST
				|| sessao.estrategiaCarregamento == ES_VERIF_CARREG_SINC_ST;
	}

	boolean isVerificacaoECarregamento() {
		return sessao.estrategiaCarregamento == ES_VERIF_CARREG_ASSINC_ST
				|| sessao.estrategiaCarregamento == ES_VERIF_CARREG_SINC_ST
				|| sessao.estrategiaCarregamento == ES_VERIF_CARREG_SINC;
	}

	boolean isCarregamentoForcado() {
		return sessao.estrategiaCarregamento == ES_FORCAR_CARREG_ASSINC_ST
				|| sessao.estrategiaCarregamento == ES_FORCAR_CARREG_SINC;
	}

	boolean isNaoCarregar() {
		return sessao.estrategiaCarregamento == ES_NAO_CARREG;
	}

	private void obterEstrategiaCarregamentoInicial() {
		String param = getParametroRequisicao(PARAM_ESTRATEGIA_CARREGAMENTO);
		logger.trace("parâmetro estratégia: {}", param);
		if (param != null) {
			sessao.estrategiaCarregamento = Integer.parseInt(param);
		} else {
			// Solicitações geradas pelos componentes Primefaces
			sessao.estrategiaCarregamento = ES_CARREG_SINC;
		}
		logger.trace("estratégia carregamento (inicial): {}", sessao.estrategiaCarregamento);
	}

	private boolean definirEstrategiaCarregamentoEncadeada() {
		// Assume que algum sumário pode ser criado no primeiro acesso
		if (sessao.primeiroAcesso) {
			sessao.estrategiaCarregamentoEncadeada = ES_VERIF_CARREG_SINC;
			return true;
		} else {
			if (MP_ESTRATEGIA_ENCAD.containsKey(sessao.estrategiaCarregamento)) {
				sessao.estrategiaCarregamentoEncadeada = MP_ESTRATEGIA_ENCAD.get(sessao.estrategiaCarregamento);
				return true;
			}
		}
		return false;
	}

	private void obterEstrategiaCarregamentoEncadeada() {
		sessao.estrategiaCarregamento = sessao.estrategiaCarregamentoEncadeada;
		logger.trace("estratégia carregamento (encadeada): {}", sessao.estrategiaCarregamento);
	}

	private class ProcessoCarregamentoEntidades extends ProcessoOperacaoLote {

		ProcessoCarregamentoEntidades() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Carregando", "Entidades", "carregadas com sucesso!",
					"carregadas com erro!", () -> verificarAtualicacoesECarregar(isCarregamentoForcado()));
		}

		void verificarAtualicacoesECarregar(boolean forcar) throws ErroModelo {
			try (Scope scp = RastreadorEvn.iniciarEscopo("verificarAtualizacoesECarregarEntidades")) {
				verificarAtualizacoesEntidades(forcar, false);
			}
		}

		void carregar() {
			try (Scope scp = RastreadorEvn.iniciarEscopo("carregarEntidades")) {
				carregarEntidades();
			}
		}

		void iniciar() throws ErroModelo {
			if (!isNaoCarregar()) {
				if (isCarregamentoComStatus()) {
					iniciar(isCarregamentoAssincrono());
				} else {
					try {
						iniciarOperacao(new Date());
						if (isVerificacaoECarregamento()) {
							verificarAtualicacoesECarregar(false);
						} else {
							if (isCarregamentoForcado()) {
								verificarAtualicacoesECarregar(true);
							} else {
								carregar();
							}
						}
					} finally {
						finalizarOperacao(new Date());
					}
				}
			} else {
				sessao.contadorOperacoes--;
			}
		}
	}

	//// OPERAÇÕES ////

	@FunctionalInterface
	private static interface Operacao {
		void executar() throws ErroModelo;
	}

	private abstract static class InfoOperacao {

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

	private abstract class ProcessoOperacao extends ThreadEvn {

		private SimpleDateFormat formatoDataHora = new SimpleDateFormatEvn("[HH:mm:ss] ");

		ProcessoOperacao(boolean validacaoRequerida) throws ErroModelo {
			super(sessao.usuario.getId());
			sessao.contadorOperacoes++;
			if (validacaoRequerida) {
				validarEntidades();
			}
		}

		void iniciar(boolean assincrono) {
			if (assincrono) {
				start();
			} else {
				run();
			}
		}

		void aguardar() {
			try {
				join();
			} catch (InterruptedException e) {
				throw new ErroExecucao("operação abortada: " + this);
			}
		}

		void iniciarOperacao(Date inicio) {
			synchronized (sessao.semaforo) {
				logger.trace("operação iniciada: {} (operações={})", this, sessao.contadorOperacoes);
				if (sessao.inicioProcessamento == null) {
					sessao.inicioProcessamento = inicio;
				}
			}
		}

		void finalizarOperacao(Date fim) {
			synchronized (sessao.semaforo) {
				sessao.contadorOperacoes--;
				if (!isProcessando()) {
					sessao.fimProcessamento = fim;
				}
				logger.trace("operação finalizada: {} (operações={})", this, sessao.contadorOperacoes);
			}
		}

		void gerarRotuloTempo(Date tempo) {
			sessao.saidaPadrao.append(formatoDataHora.format(tempo));
		}

		void gerarTempoProcessamento(Date inicio, Date fim) {
			sessao.saidaPadrao.append(" (");
			sessao.saidaPadrao.append(String.format("%.1f", (float) (fim.getTime() - inicio.getTime()) / 1000));
			sessao.saidaPadrao.append(" s)\n");
		}
	}

	public void aguardarOperacao(ProcessoOperacao processoOperacao) {
		if (processoOperacao != null) {
			logger.trace("aguardando operação: {}", processoOperacao);
			processoOperacao.aguardar();
		}
	}

	//// OPERAÇÕES EM LOTE DE ENTIDADES ////

	private static class InfoOperacaoLote extends InfoOperacao {

		Operacao oprLote;

		InfoOperacaoLote(String txtOperacao, String txtEntidade, String txtStatusSucesso, String txtStatusErro,
				Operacao oprLote) {
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
		public String toString() {
			return "'" + info.txtOperacaoMin + " " + info.txtEntidadeMin + "'";
		}

		@Override
		public void run() {
			super.run();
			Date inicio;
			Date fim = null;
			inicio = new Date();
			try {
				iniciarOperacao(inicio);
				synchronized (sessao.saidaPadrao) {
					gerarRotuloTempo(inicio);
					sessao.saidaPadrao.append(info.txtOperacao).append(" ").append(info.txtEntidadeMin).append("...\n");
				}
				String txtStatus = info.txtStatusSucesso;
				try {
					info.oprLote.executar();
				} catch (Exception e) {
					txtStatus = info.txtStatusErro;
					synchronized (sessao.saidaErro) {
						sessao.saidaErro.append("*** Erro ").append(info.txtOperacaoMin).append(" ");
						sessao.saidaErro.append(info.txtEntidadeMin).append(" ***\n\n");
						sessao.saidaErro.append(Erro.toString(e)).append("\n");
					}
					logger.error("erro {} {}", info.txtOperacaoMin, info.txtEntidadeMin, e);
				}
				fim = new Date();
				synchronized (sessao.saidaPadrao) {
					gerarRotuloTempo(fim);
					sessao.saidaPadrao.append(info.txtEntidade).append(" ").append(txtStatus);
					gerarTempoProcessamento(inicio, fim);
				}
			} finally {
				finalizarOperacao(fim);
			}
		}
	}

	//// OPERAÇÕES EM ENTIDADE ////

	@FunctionalInterface
	private static interface OperacaoEntidade {
		void executar(EntidadeEvn<?> ent) throws ErroModelo;
	}

	private static class InfoOperacaoEntidade extends InfoOperacao {

		static final Map<String, Class<? extends EntidadeEvn<?>>> MAPA_TIPOS = new HashMap<>();

		String txtEntidadeInd;
		String txtEntidadeIndMin;
		Class<? extends EntidadeEvn<?>> tipoEntidade;
		OperacaoEntidade oprEntidade;

		InfoOperacaoEntidade(String txtOperacao, String txtEntidade, String txtEntidadeInd, String txtStatusSucesso,
				String txtStatusErro, Class<? extends EntidadeEvn<?>> tipoEntidade, OperacaoEntidade oprEntidade) {
			super(txtOperacao, txtEntidade, txtStatusSucesso, txtStatusErro);
			this.txtEntidadeInd = txtEntidadeInd + " ";
			this.txtEntidadeIndMin = txtEntidadeInd.toLowerCase();
			this.tipoEntidade = tipoEntidade;
			this.oprEntidade = oprEntidade;
		}
	}

	private class ProcessoOperacaoEntidade extends ProcessoOperacao {

		InfoOperacaoEntidade info;
		Collection<? extends EntidadeEvn<?>> clEnts;

		ProcessoOperacaoEntidade(boolean requerValidacao, Collection<? extends EntidadeEvn<?>> clEnts)
				throws ErroModelo {
			this(requerValidacao, null, clEnts);
		}

		ProcessoOperacaoEntidade(boolean requerValidacao, InfoOperacaoEntidade info,
				Collection<? extends EntidadeEvn<?>> clEnts) throws ErroModelo {
			// Só valida entidades se há processamento a ser realizado
			super(requerValidacao && !clEnts.isEmpty());
			this.info = info;
			this.clEnts = clEnts;
		}

		@Override
		public String toString() {
			return "'" + info.txtOperacaoMin + " " + info.txtEntidadeIndMin.trim() + "'";
		}

		@Override
		public void run() {
			super.run();
			Date inicio;
			Date fim;
			inicio = fim = new Date();
			try {
				iniciarOperacao(inicio);
				if (!clEnts.isEmpty()) {
					synchronized (sessao.saidaPadrao) {
						gerarRotuloTempo(inicio);
						sessao.saidaPadrao.append(info.txtOperacao).append(" ").append(info.txtEntidadeMin)
								.append("...\n");
					}
					for (EntidadeEvn<?> ent : clEnts) {
						String txtStatus = info.txtStatusSucesso;
						try {
							info.oprEntidade.executar(ent);
						} catch (Exception e) {
							txtStatus = info.txtStatusErro;
							synchronized (sessao.saidaErro) {
								sessao.saidaErro.append("*** Erro ").append(info.txtOperacaoMin).append(" ");
								sessao.saidaErro.append(info.txtEntidadeIndMin).append("\"").append(ent.getNome());
								sessao.saidaErro.append("\" ***\n\n").append(Erro.toString(e)).append("\n");
							}
							logger.error("erro {} {} \"{}\"", info.txtOperacaoMin, info.txtEntidadeIndMin, ent.getNome(),
									e);
						}
						fim = new Date();
						synchronized (sessao.saidaPadrao) {
							gerarRotuloTempo(fim);
							sessao.saidaPadrao.append(info.txtEntidadeInd).append("\"");
							sessao.saidaPadrao.append(ent.getNome()).append("\" ").append(txtStatus);
							gerarTempoProcessamento(inicio, fim);
						}
						inicio = new Date();
					}
				}
			} finally {
				finalizarOperacao(fim);
			}
		}
	}

	public boolean isProcessando() {
		return sessao.contadorOperacoes > 0 || getTamanhoFilaServidor() > 0;
	}

	public int getTamanhoFilaServidor() {
		try {
			return sessao.usuario != null ? fachada.consultarTamanhoFilaProcessamento(sessao.usuario) : 0;
		} catch (ErroClienteNaoConectado e) {
			logger.warn("fachada não conectada: assume fila do servidor vazia ");
			return 0;
		} catch (ErroModelo e) {
			throw new ErroExecucao("erro inesperado", e);
		}
	}

	public void limparConsole() {
		if (!isProcessando()) {
			limparTempoProcessamento();
		}
		limparSaida();
	}

	//// SUMÁRIOS ////

	private static final String PR_SUMARIO = "sumario";
	private static final String PR_ID_ENTIDADE = "id";
	
	private static final String SM_TODOS = "todos";
	private static final String SM_PRE_PROCESSADO = "_pre_processado";

	private static final String QS_GERACAO = PARAM_ESTRATEGIA_CARREGAMENTO + "=" + ES_VERIF_CARREG_SINC_ST + "&";

	private class MapaOperacoesSum extends HashMap<String, InfoOperacaoLote> {
		MapaOperacoesSum(Usuario usu) {
			Operacao oper;
			oper = () -> fachada.gerarSumarioInteresses(usu);
			put(SM_INTERESSES, new InfoGeracaoSumarios(TXT_INTERESSES, true, oper));
			oper = () -> fachada.gerarSumarioProjetos(usu);
			put(SM_PROJETOS, new InfoGeracaoSumarios(TXT_PROJETOS, true, oper));
			oper = () -> fachada.gerarValidacaoProjetos(usu);
			put(SM_VAL_PROJETOS, new InfoGeracaoValidacoes(TXT_PROJETOS, true, oper));
			oper = () -> fachada.gerarValidacaoProjetosPreProcessada(usu);
			put(SM_VAL_PROJETOS_PRE, new InfoGeracaoValidacoes(TXT_PROJETOS, true, oper));
			oper = () -> fachada.gerarSumarioAcoes(usu);
			put(SM_ACOES, new InfoGeracaoSumarios(TXT_ACOES, false, oper));
			oper = () -> fachada.gerarSumarioAcoesCalendario(usu);
			put(SM_ACOES_CALEND, new InfoGeracaoSumarios(TXT_ACOES, false, oper));
			oper = () -> fachada.gerarValidacaoAcoes(usu);
			put(SM_VAL_ACOES, new InfoGeracaoValidacoes(TXT_ACOES, false, oper));
			oper = () -> fachada.gerarValidacaoAcoesPreProcessada(usu);
			put(SM_VAL_ACOES_PRE, new InfoGeracaoValidacoes(TXT_ACOES, false, oper));
			oper = () -> fachada.gerarSumarioReferencias(usu);
			put(SM_REFERENCIAS, new InfoGeracaoSumarios(TXT_REFERENCIAS, false, oper));
			oper = () -> fachada.gerarValidacaoReferencias(usu);
			put(SM_VAL_REFERENCIAS, new InfoGeracaoValidacoes(TXT_REFERENCIAS, false, oper));
			oper = () -> fachada.gerarValidacaoReferenciasPreProcessada(usu);
			put(SM_VAL_REFERENCIAS_PRE, new InfoGeracaoValidacoes(TXT_REFERENCIAS, false, oper));
		}
	}

	private class MapaOperacoesSumInd extends HashMap<String, InfoOperacaoEntidade> {

		MapaOperacoesSumInd(Usuario usu) {
			OperacaoEntidade oper;
			oper = intr -> fachada.gerarSumarioInteresse(usu, intr.getId());
			put(SM_INTERESSE, new InfoGeracaoSumario(TXT_INTERESSE, oper));
			InfoOperacaoEntidade.MAPA_TIPOS.put(TXT_INTERESSE, Interesse.class);
			oper = proj -> fachada.gerarSumarioInicialProjeto(usu, proj.getId());
			put(SM_INI_PROJETO, new InfoGeracaoSumario(TXT_PROJETO, oper));
			oper = proj -> fachada.gerarSumarioProjeto(usu, proj.getId());
			put(SM_PROJETO, new InfoGeracaoSumario(TXT_PROJETO, oper));
			oper = proj -> fachada.gerarValidacaoProjeto(usu, proj.getId());
			put(SM_VAL_PROJETO, new InfoGeracaoValidacao(TXT_PROJETO, oper));
			oper = proj -> fachada.gerarValidacaoProjetoPreProcessada(usu, proj.getId());
			put(SM_VAL_PROJETO_PRE, new InfoGeracaoValidacao(TXT_PROJETO, oper));
			InfoOperacaoEntidade.MAPA_TIPOS.put(TXT_PROJETO, Projeto.class);
		}

		InfoOperacaoEntidade get(String key) {
			return super.get(key);
		}
	}

	private MapaOperacoesSum mpOperacoesSum = null;
	private MapaOperacoesSumInd mpOperacoesSumInd = null;

	private void iniciarOperacoesSumario() {
		mpOperacoesSum = new MapaOperacoesSum(sessao.usuario);
		mpOperacoesSumInd = new MapaOperacoesSumInd(sessao.usuario);
	}

	private Collection<Sumario> clSums = Collections.emptyList();
	private Collection<Sumario> clSumsFiltrado;

	public Collection<Sumario> getSumarios() {
		return clSums;
	}

	public Collection<Sumario> getSumariosFiltrado() {
		return clSumsFiltrado;
	}

	public void setSumariosFiltrado(Collection<Sumario> clSumariosFiltrado) {
		this.clSumsFiltrado = clSumariosFiltrado;
	}

	public String getTituloSumarios() {
		return "Sumários (" + clSums.size() + ")";
	}

	private static String formatarDescricaoSum(String nomeSumPlr, String nomeEntPlr, boolean ehEntGenMasc) {
		return nomeSumPlr + " d" + (ehEntGenMasc ? "os " : "as ") + nomeEntPlr;
	}

	private static class InfoGeracaoSumarios extends InfoOperacaoLote {

		InfoGeracaoSumarios(String nomeEntPlr, boolean ehEntGenMasc, Operacao operacao) {
			super("Gerando", formatarDescricaoSum("Sumários coletivos", nomeEntPlr, ehEntGenMasc), "gerados com sucesso",
					"não foram gerados com sucesso", operacao);
		}
	}

	private static class InfoGeracaoValidacoes extends InfoOperacaoLote {

		InfoGeracaoValidacoes(String nomeEntPlr, boolean ehEntGenMasc, Operacao operacao) {
			super("Gerando", formatarDescricaoSum("Validações coletivas", nomeEntPlr, ehEntGenMasc), "geradas com sucesso",
					"não foram geradas com sucesso", operacao);
		}
	}

	private class ProcessoGeracaoSumarios extends ProcessoOperacaoLote {

		ProcessoGeracaoSumarios(InfoOperacaoLote info) throws ErroModelo {
			super(info, info instanceof InfoGeracaoValidacoes);
		}
	}

	private static class InfoGeracaoSumario extends InfoOperacaoEntidade {

		InfoGeracaoSumario(String nomeEnt, OperacaoEntidade oprSumario) {
			super("Gerando", formatarDescricaoSum("Sumários individuais", nomeEnt + "s", true), "Sumário do " + nomeEnt,
					"gerado com sucesso", "não foi gerado com sucesso", MAPA_TIPOS.get(nomeEnt), oprSumario);
		}
	}

	private static class InfoGeracaoValidacao extends InfoOperacaoEntidade {

		InfoGeracaoValidacao(String nomeEnt, OperacaoEntidade oprSumario) {
			super("Gerando", formatarDescricaoSum("Validações individuais", nomeEnt + "s", true), "Validação do " + nomeEnt,
					"gerada com sucesso", "não foi gerada com sucesso", MAPA_TIPOS.get(nomeEnt), oprSumario);
		}
	}

	private class ProcessoGeracaoSumario extends ProcessoOperacaoEntidade {

		ProcessoGeracaoSumario(InfoOperacaoEntidade info, Collection<? extends EntidadeEvn<?>> clEnts)
				throws ErroModelo {
			super(info instanceof InfoGeracaoValidacao, info, clEnts);
		}
	}

	private class ProcessoExclusaoSumario extends ProcessoOperacaoEntidade {

		ProcessoExclusaoSumario(Collection<Sumario> clSums) throws ErroModelo {
			super(false, clSums);
			this.info = new InfoOperacaoEntidade("Excluindo", "Sumários", "Sumário", "excluído com sucesso!",
					"não foi excluído com sucesso!", Sumario.class, sum -> fachada.excluir(sessao.usuario, sum));
		}
	}

	private class ProcessoConsistenciaSumarios extends ProcessoOperacaoLote {

		ProcessoConsistenciaSumarios() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Verificando consistência dos", "Sumários", "verificados com sucesso!",
					"verificados com erro!", () -> {
						List<ProcessoOperacao> clProcs = new ArrayList<>();
						// Exclusão dos sumários inválidos
						clProcs.add(new ProcessoExclusaoSumario(fachada.consultarSumariosInvalidos(sessao.usuario)));
						// Criação dos sumários individuais faltantes
						List<Interesse> lsIntrsGer = new ArrayList<>(clIntrs);
						lsIntrsGer.removeIf(Interesse::isSumarioDefinido);
						clProcs.add(new ProcessoGeracaoSumario(mpOperacoesSumInd.get(SM_INTERESSE), lsIntrsGer));
						List<Projeto> lsProjsGer = new ArrayList<>(clProjs);
						lsProjsGer.removeIf(Projeto::isSumarioValidacaoDefinido);
						clProcs.add(new ProcessoGeracaoSumario(mpOperacoesSumInd.get(SM_VAL_PROJETO_PRE), lsProjsGer));
						lsProjsGer = new ArrayList<>(clProjs);
						lsProjsGer.removeIf(Projeto::isSumarioDefinido);
						clProcs.add(new ProcessoGeracaoSumario(mpOperacoesSumInd.get(SM_INI_PROJETO), lsProjsGer));
						// Executar processos
						for (ProcessoOperacao processo : clProcs) {
							processo.iniciar(false);
						}
					});
		}

		@Override
		public void run() {
			aguardarOperacao(carregamentoEmCurso);
			super.run();
		}
	}

	public String getResultadoGeracaoSumariosParametro() {
		try (Scope scp = RastreadorEvn.iniciarEscopo("gerarSumariosParametro")) {
			try {
				aguardarOperacao(consistenciaSumariosEmCurso);
				if (!consoleConfigurada) {
					throw new ErroExecucao("console não configurada: status de processamento ativo");
				}
				limparSaida();
				String sumario = getParametroRequisicao(PR_SUMARIO);
				String idEnt = getParametroRequisicao(PR_ID_ENTIDADE);
				if (sumario.equals(SM_TODOS)) {
					gerarSumarios();
				} else {
					if (idEnt == null) {
						new ProcessoGeracaoSumarios(mpOperacoesSum.get(sumario)).iniciar(false);
					} else {
						InfoOperacaoEntidade info = mpOperacoesSumInd.get(sumario);
						EntidadeEvn<?> ent = FabricaEntidade.instanciar(info.tipoEntidade);
						ent.setId(idEnt);
						ent = fachada.consultarPorChavePrimaria(sessao.usuario, ent);
						new ProcessoGeracaoSumario(info, Arrays.asList(ent)).iniciar(false);
					}
				}
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			} finally {
				// Cancela carregamento encadeado pois a operação não foi chamada da console
				sessao.novoProcessamento = true;
			}
			return getResultadoProcessamento();
		}
	}

	public void gerarSumarios() {
		try (Scope scp = RastreadorEvn.iniciarEscopo("gerarSumarios")) {
			aguardarOperacao(consistenciaSumariosEmCurso);
			if (!consoleConfigurada) {
				throw new ErroExecucao("console não configurada: status de processamento ativo");
			}
			try {
				Collection<ProcessoOperacao> clProcessos = new ArrayList<>();
				if (gerarSumColIntrs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_INTERESSES)));
				}
				if (gerarSumIndIntrs) {
					clProcessos.add(new ProcessoGeracaoSumario(mpOperacoesSumInd.get(SM_INTERESSE), clIntrs));
				}
				if (gerarSumColProjs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_PROJETOS)));
				}
				if (gerarSumIndProjs) {
					clProcessos.add(new ProcessoGeracaoSumario(mpOperacoesSumInd.get(SM_PROJETO), clProjs));
				}
				if (gerarValColProjs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_VAL_PROJETOS_PRE)));
				}
				if (gerarValIndProjs) {
					clProcessos.add(new ProcessoGeracaoSumario(mpOperacoesSumInd.get(SM_VAL_PROJETO_PRE), clProjs));
				}
				if (gerarSumColAcoes) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_ACOES)));
				}
				if (gerarValColAcoes) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_VAL_ACOES_PRE)));
				}
				if (gerarSumColRefs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_REFERENCIAS)));
				}
				if (gerarValColRefs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(SM_VAL_REFERENCIAS_PRE)));
				}
				for (ProcessoOperacao processo : clProcessos) {
					processo.iniciar(true);
				}
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public void verificarConsistenciaSumarios() throws ErroModelo {
		try (Scope scp = RastreadorEvn.iniciarEscopo("verificarConsistenciaSumarios")) {
			try {
				new ProcessoConsistenciaSumarios().iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public void excluirSumarios() {
		try (Scope scp = RastreadorEvn.iniciarEscopo("excluirSumarios")) {
			try {
				new ProcessoExclusaoSumario(clSums).iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	//// INTERESSES ////

	private static final String SM_INTERESSES = ChavesSumarioAPI.INTERESSES;
	private static final String TXT_INTERESSES = "interesses";
	private static final String SM_INTERESSE = ChavesSumarioAPI.INTERESSE;
	private static final String TXT_INTERESSE = "interesse";
	private Collection<Interesse> clIntrs = Collections.emptyList();
	private Collection<Interesse> clIntrsFiltrado;
	private boolean gerarSumColIntrs = true;
	private boolean gerarSumIndIntrs = true;

	public Collection<Interesse> getInteresses() {
		return clIntrs;
	}

	public Collection<Interesse> getInteressesFiltrado() {
		return clIntrsFiltrado;
	}

	public void setInteressesFiltrado(Collection<Interesse> clIntrsFiltrado) {
		this.clIntrsFiltrado = clIntrsFiltrado;
	}

	public boolean isGerarSumColInteresses() {
		return gerarSumColIntrs;
	}

	public void setGerarSumColInteresses(boolean gerarSumColIntrs) {
		this.gerarSumColIntrs = gerarSumColIntrs;
	}

	public boolean isGerarSumIndInteresses() {
		return gerarSumIndIntrs;
	}

	public void setGerarSumIndInteresses(boolean gerarSumIndIntrs) {
		this.gerarSumIndIntrs = gerarSumIndIntrs;
	}

	public String getTituloInteresses() {
		return "Interesses (" + clIntrs.size() + ")";
	}

	//// PROJETOS ////

	private static final String SM_PROJETOS = ChavesSumarioAPI.PROJETOS;
	private static final String SM_VAL_PROJETOS = ChavesSumarioAPI.VALIDACAO + ChavesSumarioAPI.PROJETOS;
	private static final String SM_VAL_PROJETOS_PRE = SM_VAL_PROJETOS + SM_PRE_PROCESSADO;
	private static final String TXT_PROJETOS = "projetos";
	private static final String SM_INI_PROJETO = "inicio_" + ChavesSumarioAPI.PROJETO;
	private static final String SM_PROJETO = ChavesSumarioAPI.PROJETO;
	private static final String SM_VAL_PROJETO = ChavesSumarioAPI.VALIDACAO + ChavesSumarioAPI.PROJETO;
	private static final String SM_VAL_PROJETO_PRE = SM_VAL_PROJETO + SM_PRE_PROCESSADO;
	private static final String TXT_PROJETO = "projeto";
	private Collection<Projeto> clProjs = Collections.emptyList();
	private Collection<Projeto> clProjsFiltrado;
	private boolean gerarSumColProjs = true;
	private boolean gerarSumIndProjs = true;
	private boolean gerarValColProjs = true;
	private boolean gerarValIndProjs = true;

	public Collection<Projeto> getProjetos() {
		return clProjs;
	}

	public Collection<Projeto> getProjetosFiltrado() {
		return clProjsFiltrado;
	}

	public void setclProjetosFiltradoFiltrado(Collection<Projeto> clProjsFiltrado) {
		this.clProjsFiltrado = clProjsFiltrado;
	}

	public boolean isGerarSumColProjetos() {
		return gerarSumColProjs;
	}

	public void setGerarSumColProjetos(boolean gerarSumColProjs) {
		this.gerarSumColProjs = gerarSumColProjs;
	}

	public boolean isGerarSumIndProjetos() {
		return gerarSumIndProjs;
	}

	public void setGerarSumIndProjetos(boolean gerarSumIndProjs) {
		this.gerarSumIndProjs = gerarSumIndProjs;
	}

	public boolean isGerarValColProjetos() {
		return gerarValColProjs;
	}

	public void setGerarValColProjetos(boolean gerarValColProjs) {
		this.gerarValColProjs = gerarValColProjs;
	}

	public boolean isGerarValIndProjetos() {
		return gerarValIndProjs;
	}

	public void setGerarValIndProjetos(boolean gerarValIndProjs) {
		this.gerarValIndProjs = gerarValIndProjs;
	}

	public String getTituloProjetos() {
		return "Projetos (" + clProjs.size() + ")";
	}

	//// AÇÕES ////

	private static final String SM_ACOES = ChavesSumarioAPI.ACOES;
	private static final String SM_ACOES_CALEND = ChavesSumarioAPI.ACOES_CALENDARIO;
	private static final String SM_VAL_ACOES = ChavesSumarioAPI.VALIDACAO + ChavesSumarioAPI.ACOES;
	private static final String SM_VAL_ACOES_PRE = SM_VAL_ACOES + SM_PRE_PROCESSADO;
	private static final String TXT_ACOES = "ações";
	private Collection<Acao> clAcoes = Collections.emptyList();
	private Collection<Acao> clAcoesFiltrado;
	private boolean gerarSumColAcoes = true;
	private boolean gerarValColAcoes = true;

	public Collection<Acao> getAcoes() {
		return clAcoes;
	}

	public Collection<Acao> getAcoesFiltrado() {
		return clAcoesFiltrado;
	}

	public void setAcoesFiltrado(Collection<Acao> clAcoesFiltrado) {
		this.clAcoesFiltrado = clAcoesFiltrado;
	}

	public boolean isGerarSumColAcoes() {
		return gerarSumColAcoes;
	}

	public void setGerarSumColAcoes(boolean gerarSumColAcoes) {
		this.gerarSumColAcoes = gerarSumColAcoes;
	}

	public boolean isGerarValColAcoes() {
		return gerarValColAcoes;
	}

	public void setGerarValColAcoes(boolean gerarValColAcoes) {
		this.gerarValColAcoes = gerarValColAcoes;
	}

	public String getTituloAcoes() {
		return "Ações (" + clAcoes.size() + ")";
	}

	//// REFERÊNCIAS ////

	private static final String SM_REFERENCIAS = ChavesSumarioAPI.REFERENCIAS;
	private static final String SM_VAL_REFERENCIAS = ChavesSumarioAPI.VALIDACAO + ChavesSumarioAPI.REFERENCIAS;
	private static final String SM_VAL_REFERENCIAS_PRE = SM_VAL_REFERENCIAS + SM_PRE_PROCESSADO;
	private static final String TXT_REFERENCIAS = "referências";
	private Collection<Referencia> clRefs = Collections.emptyList();
	private Collection<Referencia> clRefsFiltrado;
	private boolean gerarSumColRefs = true;
	private boolean gerarValColRefs = true;

	public Collection<Referencia> getReferencias() {
		return clRefs;
	}

	public Collection<Referencia> getReferenciasFiltrado() {
		return clRefsFiltrado;
	}

	public void setReferenciasFiltrado(Collection<Referencia> clRefsFiltrado) {
		this.clRefsFiltrado = clRefsFiltrado;
	}

	public boolean isGerarSumColReferencias() {
		return gerarSumColRefs;
	}

	public void setGerarSumColReferencias(boolean gerarSumColRefs) {
		this.gerarSumColRefs = gerarSumColRefs;
	}

	public boolean isGerarValColReferencias() {
		return gerarValColRefs;
	}

	public void setGerarValColReferencias(boolean gerarValColRefs) {
		this.gerarValColRefs = gerarValColRefs;
	}

	public String getTituloReferencias() {
		return "Referências (" + clRefs.size() + ")";
	}

	//// CONFIGURADOR ////

	private Collection<Log> clLogs = Collections.emptyList();

	public Collection<Log> getLogs() {
		return clLogs;
	}

	private class ProcessoExclusaoLogs extends ProcessoOperacaoLote {

		ProcessoExclusaoLogs() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Excluindo", "Logs", "excluídos com sucesso!",
					"não foram excluídos com sucesso!", () -> fachada.excluirLogsAntigos(sessao.usuario));
		}
	}

	public void excluirLogs() {
		try (Scope scp = RastreadorEvn.iniciarEscopo("excluirLogs")) {
			try {
				new ProcessoExclusaoLogs().iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public String getResultadoExclusaoLogs() {
		excluirLogs();
		return getResultadoProcessamento();
	}
}
