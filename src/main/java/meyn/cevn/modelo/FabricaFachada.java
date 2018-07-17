package meyn.cevn.modelo;

import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.FabricaFachadaModelo;

public class FabricaFachada extends FabricaFachadaModelo {
	public static Fachada getFachada() throws ErroModelo {
		return (Fachada) getInstanciaEmCache(Fachada.class, Fachada.class.getName());
	}
}
