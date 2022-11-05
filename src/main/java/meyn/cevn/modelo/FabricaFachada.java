package meyn.cevn.modelo;

import meyn.util.modelo.ErroModelo;

public class FabricaFachada extends FabricaFachadaLocal {
	
	static {
		setClasseFabrica(FabricaFachada.class.getName());
	}

	//Força a inicialização estática da classe
	public static FachadaEvn getFachada() throws ErroModelo {
		return FabricaFachadaLocal.getFachada();
	}	
}
