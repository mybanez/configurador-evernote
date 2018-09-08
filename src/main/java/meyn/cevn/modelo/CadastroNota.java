package meyn.cevn.modelo;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.thrift.TException;

import meyn.cevn.ClienteEvn;
import meyn.cevn.util.FusoHorario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;

public abstract class CadastroNota<TipoNota extends Nota> extends CadastroEvn<NoteMetadata, TipoNota> {

	private static final String EXP_TITULO_PADRAO = "^\\S.+";
	
	private static final SimpleDateFormat FORMATO_DATA;
	
	static {
		FORMATO_DATA = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
		FORMATO_DATA.setTimeZone(FusoHorario.FORTALEZA);
	}
	
	protected class ConsultaPadrao extends CacheNotasConsultas.InfoConsulta<TipoNota> {

		ConsultaPadrao() {
			setTipoEntidade(CadastroNota.this.getTipoEntidade());
			setEntidadeValidavel(CadastroNota.this.isEntidadeValidavel());
			setIniciadorPropsEnt((Usuario usu, NoteMetadata mtd, TipoNota nota) -> {
				iniciarPropriedadesEnt(usu, mtd, nota);
			});
			setIniciadorPropsRelEnt((Usuario usu, NoteMetadata mtd, TipoNota nota) -> {
				iniciarPropriedadesRelacionamentoEnt(usu, mtd, nota);
			});
			setValidadorPropsEnt((Usuario usu, TipoNota nota) -> {
				validarPropriedadesEnt(usu, nota);
			});
			setLogger(CadastroNota.this.getLogger());
			NotesMetadataResultSpec campos = new NotesMetadataResultSpec();
			campos.setIncludeAttributes(true);
			campos.setIncludeContentLength(true);
			campos.setIncludeCreated(true);
			campos.setIncludeDeleted(true);
			campos.setIncludeNotebookGuid(true);
			campos.setIncludeTagGuids(true);
			campos.setIncludeTitle(true);
			campos.setIncludeUpdated(true);
			setCampos(campos);
			NoteFilter filtro = new NoteFilter();
			filtro.setOrder(NoteSortOrder.TITLE.getValue());
			filtro.setAscending(true);
			setFiltro(filtro);
			setComando(comandoConsulta);
			setGrupoHomonimoDeItemPermitido(grupoHomonimoDeItemPermitido);
			getChavesCache().add(getChaveCache());
		}
	}

	private final ConsultaPadrao consultaPadrao;

	private String comandoConsulta;
	private boolean grupoHomonimoDeItemPermitido;

	protected CadastroNota(String nomeRepositorio, boolean validavel, boolean repositorioPilha) throws ErroCadastro {
		this(nomeRepositorio, "", validavel, true, repositorioPilha);
	}

	protected CadastroNota(String nomeRepositorio, String nomeGrupo, boolean validavel,
			boolean grupoHomonimoDeItemPermitido, boolean repositorioPilha) throws ErroCadastro {
		super(nomeRepositorio, nomeGrupo, validavel);
		this.grupoHomonimoDeItemPermitido = grupoHomonimoDeItemPermitido;
		this.comandoConsulta = (repositorioPilha ? "stack" : "notebook") + ":\"" + nomeRepositorio + "\"";
		consultaPadrao = new ConsultaPadrao();
	}

	protected String getComandoConsulta() {
		return comandoConsulta;
	}

	protected boolean isGrupoHomonimoDeItemPermitido() {
		return grupoHomonimoDeItemPermitido;
	}

	protected Notebook getMetadadoRepositorio(Usuario usu) throws ErroModelo {
		return CacheNotebooks.getCache(usu).get(getNomeRepositorio());
	}

	@SuppressWarnings("unchecked")
	@Override CacheEntidadesEvn<TipoNota> getCache(Usuario usu) throws ErroModelo {
		return (CacheEntidadesEvn<TipoNota>) CacheNotasConsultas.getCache(usu).get(usu, consultaPadrao);
	}

	@Override
	protected void iniciarPropriedadesEnt(Usuario usu, NoteMetadata mtd, TipoNota nota) throws ErroModelo {
		nota.setMetadado(mtd);
		nota.setDataCriacao(mtd.getCreated());
		nota.setDataAlteracao(mtd.getUpdated());
		nota.setDataCriacaoFmt(FORMATO_DATA.format(new Date(mtd.getCreated())));
		nota.setDataAlteracaoFmt(FORMATO_DATA.format(new Date(mtd.getUpdated())));
		nota.setId(mtd.getGuid());
		nota.setNome(mtd.getTitle());
		NoteAttributes noteAtribs = mtd.getAttributes();
		nota.setLembrete(noteAtribs.isSetReminderOrder());
		nota.setDataLembrete(noteAtribs.isSetReminderTime() ? new Date(noteAtribs.getReminderTime()) : null);
		nota.setURL(usu.getPrefixoURL() + mtd.getGuid() + "/" + mtd.getGuid());
	}

	@Override
	protected void validarPropriedadesEnt(Usuario usu, TipoNota nota) {
		Collection<String> clMsgs = nota.getMensagensValidacao();
		String nome = nota.getNome();
		if (!nome.matches(EXP_TITULO_PADRAO)) {
			clMsgs.add("Título vazio ou iniciando com espaços: " + nome);
		}
	}

	protected void iniciarPropriedadesMetadado(Usuario usu, Note mtd, TipoNota nota) {
		mtd.setTitle(nota.getNome());
		NoteAttributes atribs = new NoteAttributes();
		long agora = System.currentTimeMillis();
		if (nota.isLembrete()) {
			atribs.setReminderOrder(agora);
			Date dt = nota.getDataLembrete();
			if (dt != null) {
				atribs.setReminderTime(dt.getTime());
			}
		}
		mtd.setAttributes(atribs);
		mtd.setContent(nota.getConteudo());
		mtd.setUpdated(agora);
	}

	public void carregarConteudo(Usuario usu, TipoNota nota) throws ErroCadastro {
		try {
			NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
			Note mtd;
			synchronized (noteStore) {
				mtd = noteStore.getNote(nota.getId(), true, false, false, false);
			}
			nota.setConteudo(mtd.getContent());
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroCadastro("Erro carregando conteúdo da nota: " + nota.getNome(), e);
		}
	}

	@Override
	public TipoNota incluir(Usuario usu, TipoNota nota) throws ErroCadastro {
		try {
			NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
			Note mtd = new Note();
			iniciarPropriedadesMetadado(usu, mtd, nota);
			mtd.setNotebookGuid(getMetadadoRepositorio(usu).getGuid());
			synchronized (noteStore) {
				mtd = noteStore.createNote(mtd);
			}
			nota.setId(mtd.getGuid());
			nota.setURL(usu.getPrefixoURL() + mtd.getGuid() + "/" + mtd.getGuid());
			getLogger().info("incluído: {}", nota.getNome());
			return nota;
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException | ErroModelo e) {
			throw new ErroCadastro("Erro incluindo nota: " + nota.getNome(), e);
		}
	}

	@Override
	public TipoNota alterar(Usuario usu, TipoNota nota) throws ErroCadastro {
		try {
			NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
			synchronized (noteStore) {
				Note mtd = noteStore.getNote(nota.getId(), false, false, false, false);
				iniciarPropriedadesMetadado(usu, mtd, nota);
				noteStore.updateNote(mtd);
			}
			getLogger().info("alterado: {}", nota.getNome());
			return nota;
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroCadastro("Erro atualizando nota: " + nota.getNome(), e);
		}
	}

	@Override
	public void excluirTodos(Usuario usu) throws ErroCadastro {
		try {
			NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
			for (Nota nota : consultarTodos(usu)) {
				synchronized (noteStore) {
					noteStore.deleteNote(nota.getId());
				}
			}
			getLogger().info("notas excluídas");
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException | ErroModelo e) {
			throw new ErroCadastro("Erro excluindo notas", e);
		}
	}

	@Override
	public void excluir(Usuario usu, TipoNota nota) throws ErroCadastro {
		try {
			NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
			synchronized (noteStore) {
				noteStore.deleteNote(nota.getId());
			}
			getLogger().info("excluído: {}", nota.getNome());
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			throw new ErroCadastro("Erro excluindo nota: " + nota.getNome(), e);
		}
	}
}
