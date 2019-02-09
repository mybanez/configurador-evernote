package meyn.cevn.controle;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

	private static final String URL_GERACAO = "/faces/console.xhtml?estrategiaCarregamento=2&";

	private Sessao sessao;
	private Fachada fachada = FabricaFachada.getFachada();
	private Logger logger = LogManager.getLogger();

	public Console() throws ErroModelo {
		try (Scope scp = Rastreador.iniciarEscopo("iniciarConsole")) {
			try {
				long tempo = System.currentTimeMillis();
				logger.debug("iniciando console");
				sessao = Sessao.getSessao(getUsuarioConectado());
				mpOperacoesSum = new MapaOperacoesSum(sessao.usuario);
				mpOperacoesSumInd = new MapaOperacoesSumInd(sessao.usuario);
				iniciarEntidades();
				logger.trace("status processamento: operações: {}", sessao.contadorOperacoes);
				logger.trace("status processamento: fila: {}", getTamanhoFilaServidor());
				logger.debug("console iniciada: {} ms", System.currentTimeMillis() - tempo);
			} catch (Exception e) {
				gerarErroInesperado(e);
			}
		}
	}

	private Usuario getUsuarioConectado() throws ErroModelo {
		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
		HttpSession sessao = (HttpSession) externalContext.getSession(true);
		// GAE: busca no memcache e, caso não encontre, no Datastore
		Usuario usu = (Usuario) sessao.getAttribute(ChavesControle.USUARIO);
		if (usu == null) {
			usu = FabricaEntidade.getInstancia(Usuario.class);
			usu.setId(sessao.getId());
			sessao.setAttribute(ChavesControle.USUARIO, usu);
			// Configura URL para geração de sumários
			String urlGeracao = externalContext.getRequestScheme() + "://";
			urlGeracao += externalContext.getRequestServerName() + ":";
			urlGeracao += externalContext.getRequestServerPort();
			urlGeracao += URL_GERACAO;
			ContextoEvn.getContexto(usu).setURLGeracao(urlGeracao);
		} 
		// Configura id do usuário para geração de log
		ThreadContext.put("usuario", usu.getId());
		logger.debug("usuario recuperado");
		// Conecta no serviço do Evernote
		fachada.conectar(usu);
		logger.debug("cliente conectado");
		return usu;
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

	private String getParametroRequisicao(String nome) {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get(nome);
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
		boolean novoProcessamento = true;
		String tempoProcessamento = "";
		Date inicioProcessamento = null;
		Date fimProcessamento = null;
		StringBuffer saidaPadrao = new StringBuffer();
		StringBuffer saidaErro = new StringBuffer();
		int contadorOperacoes = 0;
		int estrategiaCarregamento;
		int estrategiaCarregamentoEncadeada;

		Sessao(Usuario usuario) throws ErroModelo {
			this.usuario = usuario;
			LogManager.getLogger().debug("sessão console criada");
		}

		boolean isNova() {
			if (nova) {
				nova = false;
				return true;
			}
			return false;
		}
	}

	public void limparTempoProcessamento() {
		sessao.tempoProcessamento = "";
		sessao.inicioProcessamento = sessao.fimProcessamento = null;
	}

	public String getTempoProcessamento() {
		if (sessao.fimProcessamento != null) {
			long ini = sessao.inicioProcessamento.getTime();
			long fim = sessao.fimProcessamento.getTime();
			if (fim >= ini) {
				float tempo = (float) (fim - ini);
				Float m = tempo / 60000, s = (tempo / 1000) % 60;
				if (m >= 1) {
					sessao.tempoProcessamento = String.format("%.0f m %.0f s", m, s);
				} else {
					sessao.tempoProcessamento = String.format("%.1f s", s);
				}
			}
		}
		return sessao.tempoProcessamento;
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

	//// CARREGAMENTO ENTIDADES ////

	private class ProcessoCarregamentoEntidades extends ProcessoOperacaoLote {

		// Verificar atualizações e carregar assíncrono com status
		static final int ES_VER_CARREG_ASSINC_ST = 0;
		// Forçar atualizações e carregar assíncrono com status
		static final int ES_FORCAR_CARREG_ASSINC_ST = 1;
		// Verificar atualizações e carregar síncrono com status
		static final int ES_VER_CARREG_SINC_ST = 2;
		// Verificar atualizações e carregar síncrono sem status
		static final int ES_VER_CARREG_SINC = 3;
		// Carregar síncrono sem status
		static final int ES_CARREG_SINC = 4;
		// Não carregar
		static final int ES_NULA = 5;

		ProcessoCarregamentoEntidades() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Carregando", "Entidades", "carregadas com sucesso!", "carregadas com erro!",
			        () -> verificarECarregar(sessao.estrategiaCarregamento == ES_FORCAR_CARREG_ASSINC_ST));
		}

		boolean isCarregamentoAssincrono() {
			return sessao.estrategiaCarregamento == ES_VER_CARREG_ASSINC_ST || sessao.estrategiaCarregamento == ES_FORCAR_CARREG_ASSINC_ST;
		}

		boolean isCarregamentoComStatus() {
			return sessao.estrategiaCarregamento == ES_VER_CARREG_ASSINC_ST || sessao.estrategiaCarregamento == ES_FORCAR_CARREG_ASSINC_ST
			        || sessao.estrategiaCarregamento == ES_VER_CARREG_SINC_ST;
		}

		boolean isCarregamentoComVerificacao() {
			return sessao.estrategiaCarregamento == ES_VER_CARREG_SINC || sessao.estrategiaCarregamento == ES_VER_CARREG_ASSINC_ST
			        || sessao.estrategiaCarregamento == ES_VER_CARREG_SINC_ST;
		}

		void verificarECarregar(boolean forcar) throws ErroModelo {
			try (Scope scp = Rastreador.iniciarEscopo("verificarECarregarEntidades")) {
				if (!forcar) {
					fachada.verificarAtualizacoesServidor(sessao.usuario);
				}
				carregarEntidades(forcar);
			}
		}

		void carregar() throws ErroModelo {
			try (Scope scp = Rastreador.iniciarEscopo("carregarEntidades")) {
				carregarEntidades(false);
			}
		}

		void iniciar() throws ErroModelo {
			if (sessao.estrategiaCarregamento != ES_NULA) {
				if (isCarregamentoComStatus()) {
					iniciar(isCarregamentoAssincrono());
				} else {
					iniciarOperacao(new Date());
					if (isCarregamentoComVerificacao()) {
						verificarECarregar(false);
					} else {
						carregar();
					}
					finalizarOperacao(new Date());
				}
			} else {
				sessao.contadorOperacoes--;
			}
		}
	}

	// Encadeamento de estratégias:
	// a) Primeiro carregamento após atualização assíncrona com status deve ser
	// síncrono
	// e sem status, pois assume-se que nenhuma operação foi realizada e nada
	// precisa
	// ser verificado no servidor.
	// b) Primeiro carregamento após atualização síncrona com status deve ser
	// assíncrono,
	// pois assume-se que alguma operação foi realizada e alterações podem ter sido
	// feitas no servidor.
	private static final HashMap<Integer, Integer> MP_ESTRATEGIAS = new HashMap<Integer, Integer>() {
		{
			put(ProcessoCarregamentoEntidades.ES_VER_CARREG_ASSINC_ST, ProcessoCarregamentoEntidades.ES_CARREG_SINC);
			put(ProcessoCarregamentoEntidades.ES_FORCAR_CARREG_ASSINC_ST, ProcessoCarregamentoEntidades.ES_CARREG_SINC);
			put(ProcessoCarregamentoEntidades.ES_VER_CARREG_SINC_ST, ProcessoCarregamentoEntidades.ES_VER_CARREG_ASSINC_ST);
		}
	};

	private boolean entidadesValidadas = false;

	private void obterEstrategiaCarregamentoInicial() {
		String param = getParametroRequisicao("estrategiaCarregamento");
		logger.trace("parâmetro estratégia: {}", param);
		try {
			sessao.estrategiaCarregamento = Integer.parseInt(param);
		} catch (NumberFormatException e) {
			sessao.estrategiaCarregamento = ProcessoCarregamentoEntidades.ES_CARREG_SINC;
		}
		logger.trace("estratégia carregamento (inicial): {}", sessao.estrategiaCarregamento);
	}

	private boolean definirEstrategiaCarregamentoEncadeada() {
		if (MP_ESTRATEGIAS.containsKey(sessao.estrategiaCarregamento)) {
			sessao.estrategiaCarregamentoEncadeada = MP_ESTRATEGIAS.get(sessao.estrategiaCarregamento);
			return true;
		}
		return false;
	}

	private void obterEstrategiaCarregamentoEncadeada() {
		sessao.estrategiaCarregamento = sessao.estrategiaCarregamentoEncadeada;
		logger.trace("estratégia carregamento (encadeada): {}", sessao.estrategiaCarregamento);
	}

	private void iniciarEntidades() throws ErroModelo {
		// Carrega entidades se não houver processamento ativo
		if (!isProcessando()) {
			logger.debug("iniciando entidades");
			if (sessao.novoProcessamento) {
				limparTempoProcessamento();
				obterEstrategiaCarregamentoInicial();
			} else {
				obterEstrategiaCarregamentoEncadeada();
			}
			// Muda status se existe estratégia de carregamento encadeada
			sessao.novoProcessamento = !definirEstrategiaCarregamentoEncadeada();
			new ProcessoCarregamentoEntidades().iniciar();
			logger.debug("entidades iniciadas");
		}
	}

	private void validarEntidades() throws ErroModelo {
		if (!entidadesValidadas) {
			fachada.desatualizarCachesParaValidacao(sessao.usuario);
			carregarEntidades(false);
			entidadesValidadas = true;
			logger.debug("entidades validadas");
		}
	}

	private void carregarEntidades(boolean forcar) throws ErroModelo {
		logger.trace("forçar carregamento: {}", forcar);
		Usuario usu = sessao.usuario;
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
		// Verifica se existe alguma inconsistência nos sumários quando for nova sessão
		if (sessao.isNova()) {
			new ProcessoConsistenciaSumarios().iniciar(false);
			// Corrige a estratégia encadeada assumindo que algum sumário foi criado
			sessao.estrategiaCarregamentoEncadeada = ProcessoCarregamentoEntidades.ES_VER_CARREG_SINC;
		}
	}

	//// OPERAÇÕES ////

	private static final SimpleDateFormat FORMATO_DATA_HORA;

	static {
		FORMATO_DATA_HORA = new SimpleDateFormat("[HH:mm:ss] ");
		FORMATO_DATA_HORA.setTimeZone(FusoHorario.FORTALEZA);
	}

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

	private abstract class ProcessoOperacao extends Thread {

		ProcessoOperacao(boolean validacaoRequerida) throws ErroModelo {
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

		@Override
		public void run() {
			ThreadContext.put("usuario", sessao.usuario.getId());
		}

		void iniciarOperacao(Date inicio) {
			synchronized (sessao) {
				logger.trace("operação iniciada (operações={})", sessao.contadorOperacoes);
				if (sessao.inicioProcessamento == null) {
					sessao.inicioProcessamento = inicio;
				}
			}
		}

		void finalizarOperacao(Date fim) {
			synchronized (sessao) {
				sessao.contadorOperacoes--;
				if (!isProcessando()) {
					sessao.fimProcessamento = fim;
				}
				logger.trace("operação finalizada (operações={})", sessao.contadorOperacoes);
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
				info.oprLote.executar();
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

	@FunctionalInterface
	private static interface OperacaoEntidade<TipoEnt extends EntidadeEvn<?>> {
		void executar(TipoEnt ent) throws ErroModelo;
	}

	private static class InfoOperacaoEntidade<TipoEnt extends EntidadeEvn<?>> extends InfoOperacao {

		String txtEntidadeInd;
		String txtEntidadeIndMin;
		OperacaoEntidade<TipoEnt> oprEntidade;

		InfoOperacaoEntidade(String txtOperacao, String txtEntidade, String txtStatusSucesso, String txtStatusErro,
		        OperacaoEntidade<TipoEnt> oprEntidade) {
			this(txtOperacao, txtEntidade, "", txtStatusSucesso, txtStatusErro, oprEntidade);
		}

		InfoOperacaoEntidade(String txtOperacao, String txtEntidade, String txtEntidadeInd, String txtStatusSucesso, String txtStatusErro,
		        OperacaoEntidade<TipoEnt> oprEntidade) {
			super(txtOperacao, txtEntidade, txtStatusSucesso, txtStatusErro);
			txtEntidadeInd += txtEntidadeInd.isEmpty() ? "" : " ";
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
			// Só valida entidades se há processamento a ser realizado
			super(requerValidacao && !clEnts.isEmpty());
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
						info.oprEntidade.executar(ent);
					} catch (Exception e) {
						txtStatus = info.txtStatusErro;
						synchronized (sessao.saidaErro) {
							sessao.saidaErro.append("*** Erro ").append(info.txtOperacaoMin).append(" ");
							sessao.saidaErro.append(info.txtEntidadeIndMin).append("\"").append(ent.getNome());
							sessao.saidaErro.append("\" ***\n\n").append(Erro.toString(e)).append("\n");
						}
						logger.error("Erro {} {}\"{}\"", info.txtOperacaoMin, info.txtEntidadeIndMin, ent.getNome(), e);
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
			finalizarOperacao(fim);
		}
	}

	public boolean isProcessando() {
		return sessao.contadorOperacoes > 0 || fachada.consultarTamanhoFilaServidor(sessao.usuario) > 0;
	}

	public int getTamanhoFilaServidor() {
		return fachada.consultarTamanhoFilaServidor(sessao.usuario);
	}

	public void limparConsole() {
		if (!isProcessando()) {
			limparTempoProcessamento();
		}
		limparSaida();
	}

	//// SUMÁRIOS ////

	private static final String OP_TODOS = "todos";

	private Collection<Sumario> clSums = Collections.emptyList();
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

	public String getTituloSumarios() throws ErroModelo {
		return "Sumários (" + clSums.size() + "/" + (clIntrs.size() + clProjs.size() * 2 + CadastroSumario.QTD_SUMS_COLETIVOS) + ")";
	}

	private static String formatarTxtSum(String nomeSumPlr, String nomeEntPlr, boolean ehEntGenMasc) {
		return nomeSumPlr + " d" + (ehEntGenMasc ? "os " : "as ") + nomeEntPlr;
	}

	private static class InfoGeracaoSumarios extends InfoOperacaoLote {

		InfoGeracaoSumarios(String nomeEntPlr, boolean ehEntGenMasc, Operacao operacao) {
			super("Gerando", formatarTxtSum("Sumários coletivos", nomeEntPlr, ehEntGenMasc), "gerados com sucesso",
			        "não foram gerados com sucesso", operacao);
		}
	}

	private static class InfoGeracaoValidacoes extends InfoOperacaoLote {

		InfoGeracaoValidacoes(String nomeEntPlr, boolean ehEntGenMasc, Operacao operacao) {
			super("Gerando", formatarTxtSum("Validações coletivas", nomeEntPlr, ehEntGenMasc), "geradas com sucesso",
			        "não foram geradas com sucesso", operacao);
		}
	}

	private class ProcessoGeracaoSumarios extends ProcessoOperacaoLote {

		ProcessoGeracaoSumarios(InfoOperacaoLote info) throws ErroModelo {
			super(info, InfoGeracaoValidacoes.class.isInstance(info));
		}
	}

	private static class InfoGeracaoSumario<TipoEnt extends EntidadeEvn<?>> extends InfoOperacaoEntidade<TipoEnt> {

		InfoGeracaoSumario(String nomeEnt, OperacaoEntidade<TipoEnt> oprSumario) {
			super("Gerando", formatarTxtSum("Sumários individuais", nomeEnt + "s", true), "Sumário do " + nomeEnt, "gerado com sucesso",
			        "não foi gerado com sucesso", oprSumario);
		}
	}

	private static class InfoGeracaoValidacao<TipoEnt extends EntidadeEvn<?>> extends InfoOperacaoEntidade<TipoEnt> {

		InfoGeracaoValidacao(String nomeEnt, OperacaoEntidade<TipoEnt> oprSumario) {
			super("Gerando", formatarTxtSum("Validações individuais", nomeEnt + "s", true), "Validação do " + nomeEnt, "gerada com sucesso",
			        "não foi gerada com sucesso", oprSumario);
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

		public ProcessoCriacaoSumario(InfoOperacaoEntidade<TipoEnt> info, Collection<TipoEnt> clEnts) throws ErroModelo {
			super(info, (Collection<TipoEnt>) new ArrayList<TipoEnt>() {
				{
					String propSum = InfoGeracaoSumario.class.isInstance(info) ? "sumario" : "sumarioValidacao";
					for (TipoEnt ent : clEnts) {
						if (ent.get(propSum) == null) {
							add(ent);
						}
					}
				}
			});
		}
	}

	private class ProcessoExclusaoSumario extends ProcessoOperacaoEntidade<Sumario> {

		ProcessoExclusaoSumario(Collection<Sumario> clSums) throws ErroModelo {
			super(false, clSums);
			this.info = new InfoOperacaoEntidade<Sumario>("Excluindo", "Sumários", "excluído com sucesso!", "não foi excluído com sucesso!",
			        (Sumario sum) -> fachada.excluir(sessao.usuario, ChavesModelo.SUMARIO, sum));
		}
	}

	private class ProcessoConsistenciaSumarios extends ProcessoOperacaoLote {

		ProcessoConsistenciaSumarios() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Verificando consistência dos", "Sumários", "verificados com sucesso!",
			        "verificados com erro!", () -> {
				        Collection<ProcessoOperacao> clProcessos = new ArrayList<ProcessoOperacao>();
				        // Exclusão dos sumários inválidos
				        clProcessos.add(new ProcessoExclusaoSumario(fachada.consultarSumariosInvalidos(sessao.usuario)));
				        // Criação dos sumários individuais faltantes
				        clProcessos.add(new ProcessoCriacaoSumario<Interesse>(mpOperacoesSumInd.get(OP_INTERESSE), clIntrs));
				        clProcessos.add(new ProcessoCriacaoSumario<Projeto>(mpOperacoesSumInd.get(OP_VAL_PAR_PROJETO), clProjs));
				        clProcessos.add(new ProcessoCriacaoSumario<Projeto>(mpOperacoesSumInd.get(OP_INI_PROJETO), clProjs));
				        // Executar processos
				        for (ProcessoOperacao processo : clProcessos) {
					        processo.iniciar(false);
				        }
			        });
		}
	}

	private class MapaOperacoesSum extends HashMap<String, InfoOperacaoLote> {
		MapaOperacoesSum(Usuario usu) {
			Operacao oper;
			oper = () -> fachada.gerarSumarioInteresses(usu);
			put(OP_INTERESSES, new InfoGeracaoSumarios("interesses", true, oper));
			oper = () -> fachada.gerarSumarioProjetos(usu);
			put(OP_PROJETOS, new InfoGeracaoSumarios("projetos", true, oper));
			oper = () -> fachada.gerarValidacaoProjetos(usu);
			put(OP_VAL_PROJETOS, new InfoGeracaoValidacoes("projetos", true, oper));
			oper = () -> fachada.gerarValidacaoParcialProjetos(usu);
			put(OP_VAL_PAR_PROJETOS, new InfoGeracaoValidacoes("projetos", true, oper));
			oper = () -> fachada.gerarSumarioAcoes(usu);
			put(OP_ACOES, new InfoGeracaoSumarios("ações", false, oper));
			oper = () -> fachada.gerarSumarioAcoesCalendario(usu);
			put(OP_ACOES_CALEND, new InfoGeracaoSumarios("ações", false, oper));
			oper = () -> fachada.gerarValidacaoAcoes(usu);
			put(OP_VAL_ACOES, new InfoGeracaoValidacoes("ações", false, oper));
			oper = () -> fachada.gerarValidacaoParcialAcoes(usu);
			put(OP_VAL_PAR_ACOES, new InfoGeracaoValidacoes("ações", false, oper));
			oper = () -> fachada.gerarSumarioReferencias(usu);
			put(OP_REFERENCIAS, new InfoGeracaoSumarios("referências", false, oper));
			oper = () -> fachada.gerarValidacaoReferencias(usu);
			put(OP_VAL_REFERENCIAS, new InfoGeracaoValidacoes("referências", false, oper));
			oper = () -> fachada.gerarValidacaoParcialReferencias(usu);
			put(OP_VAL_PAR_REFERENCIAS, new InfoGeracaoValidacoes("referências", false, oper));
		}
	};

	private class MapaOperacoesSumInd extends HashMap<String, InfoOperacaoEntidade<? extends EntidadeEvn<?>>> {
		MapaOperacoesSumInd(Usuario usu) {
			OperacaoEntidade<Interesse> oprSumIntr;
			OperacaoEntidade<Projeto> oprSumProj;
			oprSumIntr = (Interesse intr) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.INTERESSE, intr).set("sumario",
			        fachada.gerarSumarioInteresse(usu, intr.getId()));
			put(OP_INTERESSE, new InfoGeracaoSumario<Interesse>("interesse", oprSumIntr));
			oprSumProj = (Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj).set("sumario",
			        fachada.gerarSumarioInicialProjeto(usu, proj.getId()));
			put(OP_INI_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", oprSumProj));
			oprSumProj = (Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj).set("sumario",
			        fachada.gerarSumarioProjeto(usu, proj.getId()));
			put(OP_PROJETO, new InfoGeracaoSumario<Projeto>("projeto", oprSumProj));
			oprSumProj = (Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj).set("sumarioValidacao",
			        fachada.gerarValidacaoProjeto(usu, proj.getId()));
			put(OP_VAL_PROJETO, new InfoGeracaoValidacao<Projeto>("projeto", oprSumProj));
			oprSumProj = (Projeto proj) -> fachada.consultarPorChavePrimaria(usu, ChavesModelo.PROJETO, proj).set("sumarioValidacao",
			        fachada.gerarValidacaoParcialProjeto(usu, proj.getId()));
			put(OP_VAL_PAR_PROJETO, new InfoGeracaoValidacao<Projeto>("projeto", oprSumProj));
		}

		@SuppressWarnings("unchecked")
		<TipoEnt extends EntidadeEvn<?>> InfoOperacaoEntidade<TipoEnt> get(String key) {
			return (InfoOperacaoEntidade<TipoEnt>) super.get(key);
		}
	};

	private MapaOperacoesSum mpOperacoesSum;
	private MapaOperacoesSumInd mpOperacoesSumInd;

	public String getResultadoGeracaoSumariosParametro() {
		try (Scope scp = Rastreador.iniciarEscopo("gerarSumariosParametro")) {
			try {
				limparSaida();
				String oper = getParametroRequisicao("sumario");
				String id = getParametroRequisicao("id");
				if (oper.equals(OP_TODOS)) {
					gerarSumarios();
				} else {
					if (id == null) {
						new ProcessoGeracaoSumarios(mpOperacoesSum.get(oper)).iniciar(false);
					} else {
						String[] partesOper = oper.split("_");
						String modelo = ChavesModelo.PACOTE + "." + partesOper[partesOper.length - 1].toUpperCase();
						InfoOperacaoEntidade<? extends EntidadeEvn<?>> info = mpOperacoesSumInd.get(oper);
						EntidadeEvn<?> ent = FabricaEntidade.getInstancia(EntidadeEvn.class);
						ent.setId(id);
						ent = fachada.consultarPorChavePrimaria(sessao.usuario, modelo, ent);
						new ProcessoGeracaoSumario<EntidadeEvn<?>>(info, Arrays.asList(ent)).iniciar(false);
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
		try (Scope scp = Rastreador.iniciarEscopo("gerarSumarios")) {
			try {
				Collection<ProcessoOperacao> clProcessos = new ArrayList<ProcessoOperacao>();
				if (gerarSumColIntrs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_INTERESSES)));
				}
				if (gerarSumIndIntrs) {
					clProcessos.add(new ProcessoGeracaoSumario<Interesse>(mpOperacoesSumInd.get(OP_INTERESSE), clIntrs));
				}
				if (gerarSumColProjs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_PROJETOS)));
				}
				if (gerarSumIndProjs) {
					clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesSumInd.get(OP_PROJETO), clProjs));
				}
				if (gerarValColProjs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_VAL_PAR_PROJETOS)));
				}
				if (gerarValIndProjs) {
					clProcessos.add(new ProcessoGeracaoSumario<Projeto>(mpOperacoesSumInd.get(OP_VAL_PAR_PROJETO), clProjs));
				}
				if (gerarSumColAcoes) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_ACOES)));
				}
				if (gerarValColAcoes) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_VAL_PAR_ACOES)));
				}
				if (gerarSumColRefs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_REFERENCIAS)));
				}
				if (gerarValColRefs) {
					clProcessos.add(new ProcessoGeracaoSumarios(mpOperacoesSum.get(OP_VAL_PAR_REFERENCIAS)));
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
		try (Scope scp = Rastreador.iniciarEscopo("verificarConsistenciaSumarios")) {
			try {
				new ProcessoConsistenciaSumarios().iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	public void excluirSumarios() {
		try (Scope scp = Rastreador.iniciarEscopo("excluirSumarios")) {
			try {
				new ProcessoExclusaoSumario(clSums).iniciar(true);
			} catch (ErroModelo e) {
				gerarErroInesperado(e);
			}
		}
	}

	//// INTERESSES ////

	private static final String OP_INTERESSES = CadastroSumario.OP_INTERESSES;
	private static final String OP_INTERESSE = CadastroSumario.OP_INTERESSE;
	private Collection<Interesse> clIntrs = Collections.emptyList();
	private Collection<Interesse> clIntrsFiltrado;
	private boolean gerarSumColIntrs = true;
	private boolean gerarSumIndIntrs = true;

	public Collection<Interesse> getInteresses() throws ErroModelo {
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

	public String getTituloInteresses() throws ErroModelo {
		return "Interesses (" + clIntrs.size() + ")";
	}

	//// PROJETOS ////

	private static final String OP_PROJETOS = CadastroSumario.OP_PROJETOS;
	private static final String OP_VAL_PROJETOS = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_PROJETOS;
	private static final String OP_VAL_PAR_PROJETOS = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_PROJETOS;
	private static final String OP_INI_PROJETO = "inicio_" + CadastroSumario.OP_PROJETO;
	private static final String OP_PROJETO = CadastroSumario.OP_PROJETO;
	private static final String OP_VAL_PROJETO = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_PROJETO;
	private static final String OP_VAL_PAR_PROJETO = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_PROJETO;
	private Collection<Projeto> clProjs = Collections.emptyList();
	private Collection<Projeto> clProjsFiltrado;
	private boolean gerarSumColProjs = true;
	private boolean gerarSumIndProjs = true;
	private boolean gerarValColProjs = true;
	private boolean gerarValIndProjs = true;

	public Collection<Projeto> getProjetos() throws ErroModelo {
		return clProjs;
	}

	public Collection<Projeto> getProjetosFiltrado() throws ErroModelo {
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

	public String getTituloProjetos() throws ErroModelo {
		return "Projetos (" + clProjs.size() + ")";
	}

	//// AÇÕES ////

	private static final String OP_ACOES = CadastroSumario.OP_ACOES;
	private static final String OP_ACOES_CALEND = CadastroSumario.OP_ACOES_CALENDARIO;
	private static final String OP_VAL_ACOES = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_ACOES;
	private static final String OP_VAL_PAR_ACOES = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_ACOES;
	private Collection<Acao> clAcoes = Collections.emptyList();
	private Collection<Acao> clAcoesFiltrado;
	private boolean gerarSumColAcoes = true;
	private boolean gerarValColAcoes = true;

	public Collection<Acao> getAcoes() throws ErroModelo {
		return clAcoes;
	}

	public Collection<Acao> getAcoesFiltrado() throws ErroModelo {
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

	public String getTituloAcoes() throws ErroModelo {
		return "Ações (" + clAcoes.size() + ")";
	}

	//// REFERÊNCIAS ////

	private static final String OP_REFERENCIAS = CadastroSumario.OP_REFERENCIAS;
	private static final String OP_VAL_REFERENCIAS = CadastroSumario.OP_VALIDACAO + CadastroSumario.OP_REFERENCIAS;
	private static final String OP_VAL_PAR_REFERENCIAS = CadastroSumario.OP_VALIDACAO + "parcial_" + CadastroSumario.OP_REFERENCIAS;
	private Collection<Referencia> clRefs = Collections.emptyList();
	private Collection<Referencia> clRefsFiltrado;
	private boolean gerarSumColRefs = true;
	private boolean gerarValColRefs = true;

	public Collection<Referencia> getReferencias() throws ErroModelo {
		return clRefs;
	}

	public Collection<Referencia> getReferenciasFiltrado() throws ErroModelo {
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

	public String getTituloReferencias() throws ErroModelo {
		return "Referências (" + clRefs.size() + ")";
	}

	//// CONFIGURADOR ////

	private Collection<Nota> clLogs = Collections.emptyList();

	public Collection<Nota> getLogs() throws ErroModelo {
		return clLogs;
	}

	private class ProcessoExclusaoLogs extends ProcessoOperacaoLote {

		ProcessoExclusaoLogs() throws ErroModelo {
			super(false);
			this.info = new InfoOperacaoLote("Excluindo", "Logs", "excluídos com sucesso!", "não foram excluídos com sucesso!",
			        () -> fachada.excluirLogsAntigos(sessao.usuario));
		}
	}

	public void excluirLogs() {
		try (Scope scp = Rastreador.iniciarEscopo("excluirLogs")) {
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
