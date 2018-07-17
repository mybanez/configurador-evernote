package meyn.cevn.modelo.log;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.thrift.TException;

import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.ClienteEvn;
import meyn.cevn.modelo.ContextoEvn;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;
import meyn.util.modelo.ot.FabricaOT;

public class CadastroLog extends CadastroNota<Nota> {

	// Somente uma instância de appender pode ser referenciada pelos loggers
	@Plugin(name = "Evernote", category = "Core", elementType = "appender", printObject = true)
	private static final class AppenderEvn extends AbstractAppender {

		static final String TEXT_STYLE = "font-family: Courier New; font-size: 10pt;";

		class UpdateThread extends Thread {

			Usuario usu;
			boolean active = true;
			boolean logUpdated = false;
			boolean logCreation = false;
			final StringBuffer logContent = new StringBuffer();

			UpdateThread(Usuario usu) {
				this.usu = usu;
				cadLog.gerarCabecalho(logContent);
				start();
			}

			void append(byte[] bytes) {
				//Garante que uma linha seja gerada de forma atômica
				synchronized (logContent) {
					cadLog.gerarInicioLinha(logContent);
					cadLog.gerarTexto(logContent, new String(bytes), TEXT_STYLE);
					cadLog.gerarFimLinha(logContent);
					logUpdated = true;
				}
			}

			@Override
			public void run() {
				String userName;
				try {
					userName = ClienteEvn.getUserStore(usu).getUser().getUsername();
					String logName = "Log - " + userName + "@" + usu.getId();
					while (active) {
						if (logUpdated) {
							Nota log;
							try {
								try {
									log = cadLog.consultarPorNome(usu, logName);
									log.setConteudo(cadLog.contentToString(logContent));
									cadLog.alterar(usu, log);
								} catch (ErroItemNaoEncontrado e) {
									log = FabricaOT.getInstancia(Nota.class);
									log.setNome(logName);
									log.setLembrete(false);
									log.setConteudo(cadLog.contentToString(logContent));
									logCreation = true;
									try {
										cadLog.incluir(usu, log);
									} finally {
										logCreation = false;
									}
								}
								logUpdated = false;
							} catch (ErroCadastro e) {
								LogManager.getRootLogger().error("Erro atualizando log:", e);
							}
						}
						try {
							Thread.sleep(updateInterval * 1000);
						} catch (InterruptedException e) {
						}
					}
				} catch (EDAMUserException | EDAMSystemException | TException e) {
					LogManager.getRootLogger().error("Erro iniciando log:", e);
				}
			}

			void shutdown() {
				active = false;
			}
		}

		private final CadastroLog cadLog;
		private final int updateInterval;
		private static final Map<String, AppenderEvn.UpdateThread> MP_THREADS = new ConcurrentHashMap<String, AppenderEvn.UpdateThread>();

		public AppenderEvn(String name, int updateInterval, int maxSize, Filter filter,
				Layout<? extends Serializable> layout, boolean ignoreExceptions) throws ErroCadastro {
			super(name, filter, layout, ignoreExceptions);
			this.cadLog = getCadastro(ChavesModelo.LOG);
			this.updateInterval = updateInterval;
		}

		@Override
		public void append(LogEvent event) {
			try {
				String idUsu = event.getContextData().getValue("usuario");
				UpdateThread thr;
				if (idUsu != null) {
					thr = MP_THREADS.get(idUsu);
					if (thr == null) {
						Usuario usu = ContextoEvn.getContexto(idUsu).getUsuario();
						thr = new UpdateThread(usu);
						MP_THREADS.put(idUsu, thr);
					}
					thr.append(getLayout().toByteArray(event));
				}
			} catch (Exception ex) {
				if (!ignoreExceptions()) {
					throw new AppenderLoggingException(ex);
				}
			}
		}

		@PluginFactory
		public static AppenderEvn createAppender(@PluginAttribute("name") String name,
				@PluginAttribute("updateInterval") int updateInterval, @PluginAttribute("maximumSize") int maxSize,
				@PluginElement("Filters") final Filter filter,
				@PluginElement("Layout") Layout<? extends Serializable> layout,
				@PluginAttribute("ignoreExceptions") boolean ignoreExceptions) {
			if (name == null) {
				LOGGER.error("Nome não fornecido para AppenderEvn");
				return null;
			}
			if (layout == null) {
				layout = PatternLayout.createDefaultLayout();
			}
			try {
				for (UpdateThread thr : MP_THREADS.values()) {
					thr.shutdown();
				}
				return new AppenderEvn(name, updateInterval, maxSize, filter, layout, ignoreExceptions);
			} catch (ErroCadastro e) {
				LOGGER.error("Erro criando AppenderEvn: {}", e.getMessage());
				return null;
			}
		}
	}

	private static final String REPOSITORIO = "Configurador";

	public static void desativarServico(Usuario usu) {
		if (AppenderEvn.MP_THREADS.containsKey(usu.getId())) {
			AppenderEvn.MP_THREADS.get(usu.getId()).shutdown();
		}
	}

	public CadastroLog() throws ErroCadastro {
		super(REPOSITORIO);
		setCacheInvalidoAposAtualizacao(true);
	}

	@Override
	protected void invalidarCaches(Usuario usu) throws ErroModelo {
		if (((AppenderEvn.UpdateThread) Thread.currentThread()).logCreation) {
			super.invalidarCaches(usu);
		}
	}

	//// GERAÇÃO DE ENML ////

	public String contentToString(StringBuffer cont) {
		return cont.toString() + "</en-note>";
	}
}
