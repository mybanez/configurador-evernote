package meyn.cevn.modelo;

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

import meyn.cevn.ClienteEvn;
import meyn.util.Erro;
import meyn.util.contexto.ContextoEmMemoria;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
public class CacheNotasConsultas extends CacheEntidadesConsultas {

	protected static class InfoConsulta<TipoNota extends Nota> extends CacheEntidadesConsultas.InfoConsulta<NoteMetadata, TipoNota> {

		private NotesMetadataResultSpec campos;
		private NoteFilter filtro;
		private boolean grupoHomonimoDeItemPermitido;

		String getComando() {
			return filtro.getWords();
		}

		void setComando(String texto) {
			filtro.setWords(texto);
		}

		NotesMetadataResultSpec getCampos() {
			return campos;
		}

		void setCampos(NotesMetadataResultSpec campos) {
			this.campos = campos;
		}

		NoteFilter getFiltro() {
			return filtro;
		}

		void setFiltro(NoteFilter filtro) {
			this.filtro = filtro;
		}

		boolean isGrupoHomonimoDeItemPermitido() {
			return grupoHomonimoDeItemPermitido;
		}

		void setGrupoHomonimoDeItemPermitido(boolean grupoHomonimoDeItemPermitido) {
			this.grupoHomonimoDeItemPermitido = grupoHomonimoDeItemPermitido;
		}

		String getChaveCache() {
			return getComando();
		}

		CacheNotas<TipoNota> criarCache(ContextoEmMemoria contexto) {
			return new CacheNotas<TipoNota>() {
				{
					setContexto(contexto);
					setLogger(InfoConsulta.this.getLogger());
					setChave(getChaveCache());
					setEntidadeValidavel(InfoConsulta.this.isEntidadeValidavel());
					setGrupoHomonimoDeItemPermitido(grupoHomonimoDeItemPermitido);
				}
			};
		}
	}

	protected static CacheEntidadesConsultas getCache(Usuario usu) throws ErroModelo {
		return (CacheEntidadesConsultas) getCache(usu, CacheNotasConsultas.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected CacheEntidadesEvn<?> get(Usuario usu, CacheEntidadesConsultas.InfoConsulta<?, ?> infoConsulta) throws ErroModelo {
		InfoConsulta<Nota> infoConsultaNotas = (InfoConsulta<Nota>) infoConsulta;
		try {
			String chave = infoConsultaNotas.getChaveCache();
			if (!containsKey(chave)) {
				put(chave, infoConsultaNotas.criarCache(getContexto()));
			}
			CacheNotas<Nota> cacheNotas = (CacheNotas<Nota>) get(chave);
			boolean emValidacao = cacheNotas.isValidarEntidades();
			if (!cacheNotas.isAtualizado() || emValidacao) {
				cacheNotas.clear();
				cacheNotas.setAtualizado(true);
				cacheNotas.setValidarEntidades(false);
				NoteStoreClient noteStore = ClienteEvn.getNoteStore(usu);
				int desloc = 0;
				NotesMetadataList lsMtdsPag;
				List<NoteMetadata> lsMtds = new ArrayList<NoteMetadata>();
				do {
					synchronized (noteStore) {
						lsMtdsPag = noteStore.findNotesMetadata(infoConsultaNotas.getFiltro(), desloc,
								Constants.EDAM_USER_NOTES_MAX, infoConsultaNotas.getCampos());
					}
					lsMtds.addAll(lsMtdsPag.getNotes());
					desloc += lsMtdsPag.getNotesSize();
				} while (lsMtdsPag.getTotalNotes() > desloc);
				if (emValidacao) {
					for (NoteMetadata mtd : lsMtds) {
						Nota nota = FabricaEntidade.getInstancia(infoConsultaNotas.getTipoEntidade());
						nota.setMensagensValidacao(new ArrayList<String>());
						try {
							infoConsultaNotas.getIniciadorPropsEnt().executar(usu, mtd, nota);
							infoConsultaNotas.getValidadorPropsEnt().executar(usu, nota);
						} catch (Exception e) {
							nota.getMensagensValidacao().add(Erro.toString(e));
						}
						cacheNotas.put(mtd.getGuid(), nota);
					}
				} else {
					for (NoteMetadata mtd : lsMtds) {
						Nota nota = FabricaEntidade.getInstancia(infoConsultaNotas.getTipoEntidade());
						infoConsultaNotas.getIniciadorPropsEnt().executar(usu, mtd, nota);
						cacheNotas.put(mtd.getGuid(), nota);
					}
				}
				if (emValidacao) {
					for (Nota nota : cacheNotas.values()) {
						try {
							infoConsultaNotas.getIniciadorPropsRelEnt().executar(usu, nota.getMetadado(), nota);
						} catch (Exception e) {
							nota.getMensagensValidacao().add(Erro.toString(e));
						}
					}
				} else {
					for (Nota nota : cacheNotas.values()) {
						infoConsultaNotas.getIniciadorPropsRelEnt().executar(usu, nota.getMetadado(), nota);
					}
				}
				cacheNotas.getLogger().debug("atualizado");
			}
			return cacheNotas;
		} catch (EDAMUserException | EDAMSystemException | TException | EDAMNotFoundException e) {
			throw new ErroModelo("Erro atualizando cache: " + infoConsultaNotas.getChaveCache(), e);
		}
	}
}
