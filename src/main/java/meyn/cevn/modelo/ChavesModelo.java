package meyn.cevn.modelo;

public interface ChavesModelo {
    String NOME_PACOTE = ChavesModelo.class.getPackage().getName()+".";
    String USUARIO = NOME_PACOTE+"USUARIO";
    String PROJETO = NOME_PACOTE+"PROJETO";
    String INTERESSE = NOME_PACOTE+"INTERESSE";
    String ACAO = NOME_PACOTE+"ACAO";
	String REFERENCIA = NOME_PACOTE+"REFERENCIA";
	String SUMARIO = NOME_PACOTE+"SUMARIO";
	String LOG = NOME_PACOTE+"LOG";
}
