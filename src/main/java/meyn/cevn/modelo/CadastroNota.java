package meyn.cevn.modelo;

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

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;

public abstract class CadastroNota<TipoNota extends Nota> extends CadastroEvn<NoteMetadata, TipoNota> {

	private static final String EXP_TITULO_PADRAO = "^\\S.+";

	protected class InfoCachePadrao extends CacheNotasConsulta.Info<TipoNota> {

		public InfoCachePadrao() {
			this(getMoldeOTPadrao());
			getChavesCache().add(getChave());
		}

		public InfoCachePadrao(Class<?> moldeNota) {
			setMoldeOT(moldeNota);

			setIniciadorPropsOT((Usuario usu, NoteMetadata mtd, TipoNota nota) -> {
				iniciarPropriedadesOT(usu, mtd, nota);
			});
			setIniciadorPropsRelOT((Usuario usu, NoteMetadata mtd, TipoNota nota) -> {
				iniciarPropriedadesRelacionamentoOT(usu, mtd, nota);
			});
			setValidadorPropsOT((Usuario usu, TipoNota nota) -> {
				validarPropriedadesOT(usu, nota);
			});

			NotesMetadataResultSpec props = new NotesMetadataResultSpec();
			props.setIncludeAttributes(true);
			props.setIncludeContentLength(true);
			props.setIncludeCreated(true);
			props.setIncludeDeleted(true);
			props.setIncludeNotebookGuid(true);
			props.setIncludeTagGuids(true);
			props.setIncludeTitle(true);
			props.setIncludeUpdated(true);
			setProps(props);

			NoteFilter filtro = new NoteFilter();
			filtro.setOrder(NoteSortOrder.TITLE.getValue());
			filtro.setAscending(true);
			setFiltro(filtro);

			setTexto(textoConsultaRepositorio);

			setGrupoHomonimoDeItemPermitido(grupoHomonimoDeItemPermitido);
		}
	}

	private final InfoCachePadrao infoCacheMoldePadrao;

	private String textoConsultaRepositorio;
	private boolean grupoHomonimoDeItemPermitido;

	protected CadastroNota(String nomeRepositorio) throws ErroCadastro {
		this(nomeRepositorio, "");
	}

	protected CadastroNota(String nomeRepositorio, String nomeGrupo) throws ErroCadastro {
		this(nomeRepositorio, nomeGrupo, true, false, false);
	}

	protected CadastroNota(String nomeRepositorio, String nomeGrupo, boolean grupoHomonimoDeItemPermitido,
			boolean cacheInvalidoAposAtualizacao, boolean repositorioPilha) throws ErroCadastro {
		super(nomeRepositorio, nomeGrupo, cacheInvalidoAposAtualizacao);
		this.grupoHomonimoDeItemPermitido = grupoHomonimoDeItemPermitido;
		this.textoConsultaRepositorio = (repositorioPilha ? "stack" : "notebook") + ":\"" + nomeRepositorio + "\"";
		infoCacheMoldePadrao = new InfoCachePadrao();
	}

	protected String getTextoConsultaRepositorio() {
		return textoConsultaRepositorio;
	}

	protected boolean isGrupoHomonimoDeItemPermitido() {
		return grupoHomonimoDeItemPermitido;
	}

	protected Notebook getMetadadoRepositorio(Usuario usu) throws ErroModelo {
		return CacheNotebooks.getCache(usu).get(getNomeRepositorio());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected CacheOTEvn<TipoNota> getCache(Usuario usu, Class<?> moldeNota) throws ErroModelo {
		return (CacheOTEvn<TipoNota>) CacheNotasConsulta.getCache(usu).get(usu,
				moldeNota.equals(getMoldeOTPadrao()) ? infoCacheMoldePadrao : new InfoCachePadrao(moldeNota));
	}

	@Override
	protected void iniciarPropriedadesOT(Usuario usu, NoteMetadata mtd, TipoNota nota) throws ErroModelo {
		nota.setMetadado(mtd);
		nota.setId(mtd.getGuid());
		nota.setNome(mtd.getTitle());
		NoteAttributes noteAtribs = mtd.getAttributes();
		nota.setLembrete(noteAtribs.isSetReminderOrder());
		nota.setDataLembrete(noteAtribs.isSetReminderTime() ? new Date(noteAtribs.getReminderTime()) : null);
		nota.setURL(usu.getPrefixoURL() + mtd.getGuid() + "/" + mtd.getGuid());
	}

	@Override
	protected void validarPropriedadesOT(Usuario usu, TipoNota nota) {
		Collection<String> clMsgs = nota.getMensagensValidacao();
		String nome = nota.getNome();
		if (!nome.matches(EXP_TITULO_PADRAO)) {
			clMsgs.add("Título vazio ou iniciando com espaços: " + nome);
		}
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
			invalidarCaches(usu);
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
			invalidarCaches(usu);
			getLogger().info("alterado: {}", nota.getNome());
			return nota;
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException | ErroModelo e) {
			throw new ErroCadastro("Erro atualizando nota: " + nota.getNome(), e);
		}
	}

	@Override
	public void excluir(Usuario usu, TipoNota nota) throws ErroCadastro {
		try {
			NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
			synchronized (noteStore) {
				noteStore.deleteNote(nota.getId());
			}
			invalidarCaches(usu);
			getLogger().info("excluído: {}", nota.getNome());
		} catch (EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException | ErroModelo e) {
			throw new ErroCadastro("Erro excluindo nota: " + nota.getNome(), e);
		}
	}

	protected void iniciarPropriedadesMetadado(Usuario usu, Note mtd, TipoNota nota) throws ErroModelo {
		mtd.setTitle(nota.getNome());
		if (nota.isLembrete()) {
			mtd.setAttributes(new NoteAttributes());
			mtd.getAttributes().setReminderOrder(System.currentTimeMillis());
			Date dt = nota.getDataLembrete();
			if (dt != null) {
				mtd.getAttributes().setReminderTime(dt.getTime());
			}
		}
		mtd.setContent(nota.getConteudo());
	}
}
