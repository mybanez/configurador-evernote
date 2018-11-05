package meyn.cevn.modelo.acao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evernote.edam.notestore.NoteMetadata;

import meyn.cevn.modelo.CacheNotebooks;
import meyn.cevn.modelo.CacheTags;
import meyn.cevn.modelo.CadastroEtiqueta;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.Usuario;
import meyn.cevn.modelo.interesse.CadastroInteresse;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.util.FusoHorario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.Modelo;

@Modelo(ChavesModelo.ACAO)
public class CadastroAcao extends CadastroNota<Acao> {

	private static final String REPOSITORIO = "3. A��es";
	public static final String REPOSITORIO_FOCO = "3.1. Em Foco";
	public static final String REPOSITORIO_DELEGADA = "3.2. Delegadas";
	public static final String REPOSITORIO_PROXIMA = "3.3. Pr�ximas";

	private static final String GRUPO = "<3. A��o>";

	private static final String ATRIB_COMUNICAR = "Comunicar";
	private static final String ATRIB_LER_REVISAR = "Ler/Revisar";

	private static final String EXP_TITULO_PERIODICA = "(((\\(D\\))|(\\(S\\))|(\\(Q\\))|(\\(M\\))|(\\(BM\\))|(\\(TM\\))|(\\(SM\\))|(\\(A\\)))\\s)?";
	private static final String EXP_TITULO_LEMBRETE = EXP_TITULO_PERIODICA + "\\S.*";
	private static final String EXP_TITULO_DELEGADA = EXP_TITULO_PERIODICA + "[^\\s-]+\\s-\\s[^\\s-][^-]*";
	private static final String EXP_TITULO_COMUNICAR = "(" + EXP_TITULO_PERIODICA + "Reuni�o\\s-\\s)?[^\\s-][^-]*";
	private static final String EXP_TITULO_LER_REVISAR = "Ler\\s-\\s\\S.*";

	@SuppressWarnings("serial")
	private static final Map<String, Integer> HORARIOS_LEMBRETES = new HashMap<String, Integer>() {
		{
			put("(D)", 1);
			put("(S)", 2);
			put("(Q)", 3);
			put("(M)", 4);
			put("(BM)", 4);
			put("(TM)", 4);
			put("(SM)", 5);
			put("(A)", 6);
		}
	};

	private final CadastroEtiqueta<Etiqueta> cadAtrib = new CadastroEtiqueta<Etiqueta>("<Atributo>") {
	};

	public CadastroAcao() throws ErroModelo {
		super(REPOSITORIO, GRUPO, true, true, true);
	}

	@Override
	protected void iniciarPropriedadesEnt(Usuario usu, NoteMetadata mtd, Acao acao) throws ErroModelo {
		super.iniciarPropriedadesEnt(usu, mtd, acao);
		CacheNotebooks cacheNtb = CacheNotebooks.getCache(usu);
		List<String> lsIdsTag = mtd.getTagGuids();

		String nomeRepo = cacheNtb.get(mtd.getNotebookGuid()).getName();
		acao.setFoco(nomeRepo.equals(REPOSITORIO_FOCO));
		acao.setDelegada(nomeRepo.equals(REPOSITORIO_DELEGADA));
		acao.setProxima(nomeRepo.equals(REPOSITORIO_PROXIMA));

		if (lsIdsTag != null) {
			acao.setComunicacao(lsIdsTag.contains(cadAtrib.consultarPorNome(usu, ATRIB_COMUNICAR).getId()));
			acao.setLeituraRevisao(lsIdsTag.contains(cadAtrib.consultarPorNome(usu, ATRIB_LER_REVISAR).getId()));
		} else {
			acao.setComunicacao(false);
			acao.setLeituraRevisao(false);
		}
	}

	@Override
	protected void iniciarPropriedadesRelacionamentoEnt(Usuario usu, NoteMetadata mtd, Acao acao) throws ErroModelo {
		List<String> lsIdsTag = mtd.getTagGuids();
		Collection<Projeto> clProjs = new ArrayList<Projeto>();
		acao.setProjetos(clProjs);
		if (lsIdsTag != null) {
			lsIdsTag = new ArrayList<String>(lsIdsTag);
			for (Etiqueta etq : cadAtrib.consultarTodos(usu)) {
				lsIdsTag.remove(etq.getId());
			}

			// Empregador
			CadastroInteresse cadIntr = getCadastro(ChavesModelo.INTERESSE);
			for (Interesse empr : cadIntr.consultarPorRepositorio(usu, "<Empregador>")) {
				String id = empr.getId();
				if (lsIdsTag.contains(id)) {
					acao.setEmpregador(empr);
					lsIdsTag.remove(id);
					break;
				}
			}
			// Projetos
			CadastroProjeto cadProj = getCadastro(ChavesModelo.PROJETO);
			for (String id : lsIdsTag) {
				clProjs.add(cadProj.consultarPorNome(usu, CacheTags.getCache(usu).get(id).getName()));
			}
		}
	}

	@Override
	public void validarPropriedadesEnt(Usuario usu, Acao acao) {
		super.validarPropriedadesEnt(usu, acao);
		Collection<String> clMsgs = acao.getMensagensValidacao();
		// T�tulo
		String nome = acao.getNome();
		if (acao.isFoco() || acao.isProxima()) {
			if (acao.isComunicacao()) {
				if (!nome.matches(EXP_TITULO_COMUNICAR)) {
					clMsgs.add("A��o de comunica��o com t�tulo inv�lido");
				}
			} else if (acao.isLeituraRevisao()) {
				if (!nome.matches(EXP_TITULO_LER_REVISAR)) {
					clMsgs.add("A��o de leitura com t�tulo inv�lido");
				}
			}
		} else if (acao.isDelegada()) {
			if (!nome.matches(EXP_TITULO_DELEGADA)) {
				clMsgs.add("A��o delegada com t�tulo inv�lido");
			}
		}
		// Lembrete
		if (acao.isLembrete()) {
			// T�tulo e hor�rio
			if (!nome.matches(EXP_TITULO_LEMBRETE)) {
				clMsgs.add("A��o lembrete com t�tulo inv�lido");
			} else {
				String freq = nome.replaceFirst(EXP_TITULO_LEMBRETE, "$2");
				if (!freq.equals("")) {
					Calendar cal = Calendar.getInstance(FusoHorario.FORTALEZA);
					cal.setTime(acao.getDataLembrete());
					int horaRef = HORARIOS_LEMBRETES.get(freq);
					int horaLemb = cal.get(Calendar.HOUR_OF_DAY);
					int minLemb = cal.get(Calendar.MINUTE);
					if (horaLemb > horaRef || (horaLemb == horaRef && minLemb > 2)
							|| (horaLemb == horaRef - 1 && minLemb < 59) || horaLemb < horaRef - 1) {
						clMsgs.add("Hor�rio da a��o lembrete inv�lido: " + horaLemb + ":" + minLemb + " (Ref: " + horaRef
								+ ":00)");
					}
				}
			}
			if (acao.isProxima()) {
				clMsgs.add("A��o pr�xima com lembrete");
			}
		} else {
			if (acao.isFoco()) {
				clMsgs.add("A��o em foco sem ser lembrete");
			} else if (acao.isDelegada()) {
				clMsgs.add("A��o delegada sem ser lembrete");
			}
		}
	}
}
