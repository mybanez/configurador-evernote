package meyn.cevn.controle;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.faces.bean.ManagedBean;

import meyn.cevn.modelo.FabricaFachada;

@ManagedBean(name = "gerador")
public class GeradorSumarios extends ManagedBeanImpl {
	private static final long serialVersionUID = 8123802055362514845L;
	private String resultado;
	
	private String formatarErro(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}

	public String getResultadoSumarioInteresses() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioInteresses(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}	

	public String getResultadoSumarioInteresse() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioInteresse(getUsuario(), getParametroURL("id"));
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}
	
	public String getResultadoSumarioProjetos() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioProjetos(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}	
	
	public String getResultadoSumarioProjeto() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioProjeto(getUsuario(), getParametroURL("id"));
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}

	public String getResultadoValidacaoProjetos() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarValidacaoProjetos(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}

	public String getResultadoValidacaoProjeto() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarValidacaoProjeto(getUsuario(), getParametroURL("id"));
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}
	
	public String getResultadoSumarioAcoes() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioAcoes(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}
	
	public String getResultadoSumarioAcoesCalendario() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioAcoesCalendario(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}
	
	public String getResultadoValidacaoAcoes() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarValidacaoAcoes(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}
	
	public String getResultadoSumarioReferencias() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarSumarioReferencias(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}

	public String getResultadoValidacaoReferencias() {
		try {
			if (resultado == null) {
				FabricaFachada.getFachada().gerarValidacaoReferencias(getUsuario());
				resultado = "ok";
			}
		} catch (Throwable t) {
			resultado = formatarErro(t);
		}
		return resultado;
	}
}
