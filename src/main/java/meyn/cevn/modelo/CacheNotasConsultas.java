package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.List;

import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataResultSpec;

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
			// Para garantir consistência, assume cenário de console única
			if (!cacheNotas.isAtualizado()) {
				cacheNotas.clear();
				List<NoteMetadata> lsMtds = ClienteEvn.consultarNotas(usu, infoConsultaNotas.getFiltro(), infoConsultaNotas.getCampos());
				if (cacheNotas.isValidarEntidades()) {
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
				// Status deve mudar antes de carregar relacionamentos para não gerar
				// atualização recursiva
				cacheNotas.setAtualizado(true);
				if (cacheNotas.isValidarEntidades()) {
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
				cacheNotas.setValidarEntidades(false);
				cacheNotas.getLogger().debug("atualizado");
			}
			return cacheNotas;
		} catch (ErroModelo e) {
			throw new ErroModelo("Erro carregando cache de consultas: " + infoConsultaNotas.getChaveCache(), e);
		}
	}
}
