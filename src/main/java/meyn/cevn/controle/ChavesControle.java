package meyn.cevn.controle;

public class ChavesControle {
	
	public static final String NOME_PACOTE = ChavesControle.class.getPackage().getName() + ".";
	public static final String USUARIO = NOME_PACOTE + "USUARIO";
	public static final String URL_SERVIDOR = NOME_PACOTE + "URL_SERVIDOR";
	
	private ChavesControle() {}
}
