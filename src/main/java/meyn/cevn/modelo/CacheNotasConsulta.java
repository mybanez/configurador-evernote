package meyn.cevn.modelo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.limits.Constants;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.thrift.TException;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.contexto.Contexto;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.ot.FabricaOT;

@SuppressWarnings("serial")
public class CacheNotasConsulta extends CacheResultadosConsulta {

	public static class Info<TipoNota extends Nota> extends CacheResultadosConsulta.Info<NoteMetadata, TipoNota> {

		private NotesMetadataResultSpec props;
		private NoteFilter filtro;
		private boolean grupoHomonimoDeItemPermitido;

		public String getTexto() {
			return filtro.getWords();
		}

		public void setTexto(String texto) {
			filtro.setWords(texto);
		}

		public NotesMetadataResultSpec getProps() {
			return props;
		}

		public void setProps(NotesMetadataResultSpec props) {
			this.props = props;
		}

		public NoteFilter getFiltro() {
			return filtro;
		}

		public void setFiltro(NoteFilter filtro) {
			this.filtro = filtro;
		}

		public boolean isGrupoHomonimoDeItemPermitido() {
			return grupoHomonimoDeItemPermitido;
		}

		public void setGrupoHomonimoDeItemPermitido(boolean grupoHomonimoDeItemPermitido) {
			this.grupoHomonimoDeItemPermitido = grupoHomonimoDeItemPermitido;
		}

		public String getChave() {
			return getTexto() + "-" + getMoldeOT().getName();
		}

		public CacheNotas<TipoNota> getInstancia(Contexto ctx) {
			return new CacheNotas<TipoNota>() {
				{
					setGrupoHomonimoDeItemPermitido(grupoHomonimoDeItemPermitido);
					setContexto(ctx);
				}
			};
		}
	}

	public static CacheNotasConsulta getCache(Usuario usu) throws ErroModelo {
		return (CacheNotasConsulta) getCache(usu, CacheNotasConsulta.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CacheOTEvn<?> get(Usuario usu, CacheResultadosConsulta.Info<?, ?> infoCache) throws ErroModelo {
		Info<Nota> infoCacheNotas = (Info<Nota>) infoCache;
		try {
			String chave = infoCacheNotas.getChave();
			CacheOTEvn<?> cache = get(chave);
			if (cache == null || !cache.isAtualizado()) {
				CacheNotas<Nota> cacheNotas = infoCacheNotas.getInstancia(getContexto());
				cacheNotas.setAtualizado(true);
				put(chave, cacheNotas);
				NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
				int desloc = 0;
				NotesMetadataList lsMtdsPag;
				List<NoteMetadata> lsMtds = new ArrayList<NoteMetadata>();
				do {
					synchronized (noteStore) {
						lsMtdsPag = noteStore.findNotesMetadata(infoCacheNotas.getFiltro(), desloc,
								Constants.EDAM_USER_NOTES_MAX, infoCacheNotas.getProps());
					}
					lsMtds.addAll(lsMtdsPag.getNotes());
					desloc += lsMtdsPag.getNotesSize();
				} while (lsMtdsPag.getTotalNotes() > desloc);
				if (isEmValidacao()) {
					for (NoteMetadata mtd : lsMtds) {
						Nota nota = FabricaOT.getInstancia(infoCacheNotas.getMoldeOT());
						nota.setMensagensValidacao(new ArrayList<String>());
						try {
							infoCacheNotas.getIniciadorPropsOT().executar(usu, mtd, nota);
							infoCacheNotas.getValidadorPropsOT().executar(usu, nota);
						} catch (Throwable t) {
							StringWriter sw = new StringWriter();
							PrintWriter pw = new PrintWriter(sw);
							t.printStackTrace(pw);
							nota.getMensagensValidacao().add(sw.toString());
						}
						cacheNotas.put(mtd.getGuid(), nota);
					}
					for (Nota nota : cacheNotas.values()) {
						try {
							infoCacheNotas.getIniciadorPropsRelOT().executar(usu, nota.getMetadado(), nota);
						} catch (Throwable t) {
							StringWriter sw = new StringWriter();
							PrintWriter pw = new PrintWriter(sw);
							t.printStackTrace(pw);
							nota.getMensagensValidacao().add(sw.toString());
						}
					}
				} else {
					for (NoteMetadata mtd : lsMtds) {
						Nota nota = FabricaOT.getInstancia(infoCacheNotas.getMoldeOT());
						infoCacheNotas.getIniciadorPropsOT().executar(usu, mtd, nota);
						cacheNotas.put(mtd.getGuid(), nota);
					}
					for (Nota nota : cacheNotas.values()) {
						infoCacheNotas.getIniciadorPropsRelOT().executar(usu, nota.getMetadado(), nota);
					}
				}
				cache = cacheNotas;
				cache.getLogger().debug("atualizado - "+chave);
			}
			return cache;
		} catch (EDAMUserException | EDAMSystemException | TException | EDAMNotFoundException e) {
			throw new ErroModelo("Erro atualizando cache: " + infoCacheNotas.getChave(), e);
		}
	}

}
