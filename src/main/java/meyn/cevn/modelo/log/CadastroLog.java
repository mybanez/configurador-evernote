package meyn.cevn.modelo.log;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import meyn.cevn.ContextoEvn;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.ClienteEvn;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.Modelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.entidade.FabricaEntidade;

@Modelo(ChavesModelo.LOG)
public class CadastroLog extends CadastroNota<Nota> {

	// Somente uma instância de appender pode ser referenciada pelos loggers
	@Plugin(name = "Evernote", category = "Core", elementType = "appender", printObject = true)
	private static final class AppenderEvn extends AbstractAppender {

		private static final Map<String, AppenderEvn.ProcessoAtualizacao> MP_THREADS = new ConcurrentHashMap<String, AppenderEvn.ProcessoAtualizacao>();

		@PluginFactory
		public static AppenderEvn criarAppender(@PluginAttribute("name") String nome,
		        @PluginAttribute("updateInterval") int intervaloAtualizacao, @PluginElement("Filters") final Filter filtro,
		        @PluginElement("Layout") Layout<? extends Serializable> leiaute,
		        @PluginAttribute("ignoreExceptions") boolean ignorarErros) {
			try {
				if (nome == null) {
					LOGGER.error("Nome não fornecido para AppenderEvn");
					return null;
				}
				if (leiaute == null) {
					leiaute = PatternLayout.createDefaultLayout();
				}
				for (ProcessoAtualizacao thr : MP_THREADS.values()) {
					thr.desativar();
				}
				return new AppenderEvn(nome, intervaloAtualizacao, filtro, leiaute, ignorarErros);
			} catch (ErroModelo e) {
				LOGGER.error("Erro criando AppenderEvn: ", e);
				return null;
			}
		}

		class ProcessoAtualizacao extends Thread {

			Usuario usu;
			boolean logAtivo = true;
			boolean logAtualizado = false;
			final StringBuffer conteudoLog = new StringBuffer();

			ProcessoAtualizacao(Usuario usu) {
				this.usu = usu;
				cadLog.gerarCabecalho(conteudoLog);
				MP_THREADS.put(usu.getId(), this);
				start();
			}

			void append(byte[] bytes) {
				// Garante que uma linha seja gerada de forma atômica
				synchronized (conteudoLog) {
					cadLog.gerarInicioLinha(conteudoLog);
					cadLog.gerarTextoMultilinha(conteudoLog, new String(bytes), TEXT_STYLE);
					cadLog.gerarFimLinha(conteudoLog);
					logAtualizado = true;
				}
			}

			@Override
			public void run() {
				ThreadContext.put("usuario", usu.getId());
				try {
					Nota log = usu.getLog();
					// Não faz nada se o log não tiver sido criado
					if (log != null) {
						cadLog.getLogger().debug("Thread do appender iniciado: {}", log.getNome());
						while (logAtivo) {
							if (logAtualizado) {
								try {
									log.setConteudo(cadLog.contentToString(conteudoLog));
									cadLog.alterar(usu, log);
									logAtualizado = false;
								} catch (ErroModelo e) {
									cadLog.getLogger().error("Erro escrevendo no log: ", e);
								}
							}
							try {
								Thread.sleep(intervaloAtualizacao * 1000);
							} catch (InterruptedException e) {
							}
						}
					}
					MP_THREADS.remove(usu.getId());
				} catch (Exception e) {
					cadLog.getLogger().error("Processo de atualização do log abortado. Usuario: {}", usu, e);
				}
			}

			void desativar() {
				logAtivo = false;
			}
		}

		private final CadastroLog cadLog;
		private final int intervaloAtualizacao;

		public AppenderEvn(String nome, int intervaloAtualizacao, Filter filtro, Layout<? extends Serializable> leiaute,
		        boolean ignorarErros) throws ErroModelo {
			super(nome, filtro, leiaute, ignorarErros);
			this.cadLog = getCadastro(ChavesModelo.LOG);
			this.intervaloAtualizacao = intervaloAtualizacao;
		}

		@Override
		public void append(LogEvent event) {
			try {
				String idUsu = event.getContextData().getValue("usuario");
				ProcessoAtualizacao thr;
				LOGGER.trace("Mensagem gerada. Logger: {} Nivel: {} ", event.getLoggerName(), event.getLevel());
				if (idUsu != null) {
					synchronized (MP_THREADS) {
						thr = MP_THREADS.get(idUsu);
						if (thr == null) {
							// Pode gerar erro se houver chamada antes da criação do contexto Evn
							thr = new ProcessoAtualizacao(ContextoEvn.getContexto(idUsu).getUsuario());
						}
					}
					LOGGER.trace("Mensagem apensada. Logger: {} Nivel: {} ", event.getLoggerName(), event.getLevel());
					thr.append(getLayout().toByteArray(event));
				}
			} catch (Exception e) {
				LOGGER.error("Erro escrevendo no log: ", e);
			}
		}
	}

	private static final String REPOSITORIO = "Configurador";

	public static void desativarServico(Usuario usu) {
		if (AppenderEvn.MP_THREADS.containsKey(usu.getId())) {
			AppenderEvn.MP_THREADS.get(usu.getId()).desativar();
		}
	}

	public CadastroLog() throws ErroModelo {
		super(REPOSITORIO, false, false);
	}

	// Gera o log da sessão ativa
	public Nota gerarLogUsuario(Usuario usu) throws ErroCadastro {
		Nota log = usu.getLog();
		try {
			String nomeLog = log == null ? "Log - " + ClienteEvn.getNomeUsuario(usu) + "@" + usu.getId() : log.getNome();
			try {
				log = consultarPorNome(usu, nomeLog);
				getLogger().debug("log recuperado: {}", nomeLog);
			} catch (ErroItemNaoEncontrado e) {
				log = FabricaEntidade.getInstancia(Nota.class);
				log.setNome(nomeLog);
				log.setLembrete(false);
				gerarMensagemInativo(log);
				log = incluir(usu, log);
			}
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro gerando log do usuário", e);
		}
		return log;
	}

	public void excluirLogsAntigos(Usuario usu) throws ErroCadastro {
		Collection<Nota> clLogs = consultarPorFiltro(usu, (log) -> log.getNome().startsWith("Log") && !log.getNome().contains(usu.getId()));
		for (Nota log : clLogs) {
			excluir(usu, log);
		}
	}

	//// GERAÇÃO DE ENML ////

	static final String TEXT_STYLE = "font-family: Courier New; font-size: 10pt;";

	private void gerarMensagemInativo(Nota log) {
		StringBuffer cont = new StringBuffer();
		gerarCabecalho(cont);
		gerarTexto(cont, "Nenhuma informação registrada. Verificar configuração LOG4J!", TEXT_STYLE);
		gerarRodape(cont);
		log.setConteudo(cont.toString());
	}

	private String contentToString(StringBuffer cont) {
		return cont.toString() + "</en-note>";
	}
}
