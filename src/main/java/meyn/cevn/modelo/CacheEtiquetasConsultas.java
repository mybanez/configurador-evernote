package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.List;

import com.evernote.edam.type.Tag;

import meyn.util.contexto.ContextoEmMemoria;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.entidade.FabricaEntidade;

@SuppressWarnings("serial")
public class CacheEtiquetasConsultas extends CacheEntidadesConsultas {

	protected static class InfoConsulta<TipoEtq extends Etiqueta> extends CacheEntidadesConsultas.InfoConsulta<Tag, TipoEtq> {

		private String nomeRepositorio;

		public String getNomeRepositorio() {
			return nomeRepositorio;
		}

		public void setNomeRepositorio(String nomeRepositorio) {
			this.nomeRepositorio = nomeRepositorio;
		}

		public String getChaveCache() {
			return getNomeRepositorio();
		}

		public CacheEtiquetas<TipoEtq> criarCache(ContextoEmMemoria contexto) {
			return new CacheEtiquetas<TipoEtq>() {
				{
					setContexto(contexto);
					setLogger(InfoConsulta.this.getLogger());
					setChave(getChaveCache());
					setEntidadeValidavel(InfoConsulta.this.isEntidadeValidavel());
				}
			};
		}
	}

	public static CacheEtiquetasConsultas getCache(Usuario usu) throws ErroModelo {
		return (CacheEtiquetasConsultas) getCache(usu, CacheEtiquetasConsultas.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CacheEntidadesEvn<?> get(Usuario usu, CacheEntidadesConsultas.InfoConsulta<?, ?> infoConsulta) throws ErroModelo {
		InfoConsulta<Etiqueta> infoConsultaEtqs = (InfoConsulta<Etiqueta>) infoConsulta;
		try {
			String chave = infoConsultaEtqs.getChaveCache();
			if (!containsKey(chave)) {
				put(chave, infoConsultaEtqs.criarCache(getContexto()));
			}
			CacheEtiquetas<Etiqueta> cacheEtqs = (CacheEtiquetas<Etiqueta>) get(chave);
			boolean emValidacao = cacheEtqs.isValidarEntidades();
			if (!cacheEtqs.isAtualizado() || emValidacao) {
				cacheEtqs.clear();
				cacheEtqs.setAtualizado(true);
				cacheEtqs.setValidarEntidades(false);
				CacheTags cacheTag = CacheTags.getCache(usu);
				List<Tag> lsTags = cacheTag.consultarPorRepositorio(infoConsultaEtqs.getNomeRepositorio());
				if (emValidacao) {
					for (Tag tag : lsTags) {
						Etiqueta etq = FabricaEntidade.getInstancia(infoConsultaEtqs.getTipoEntidade());
						etq.setMensagensValidacao(new ArrayList<String>());
						try {
							infoConsultaEtqs.getIniciadorPropsEnt().executar(usu, tag, etq);
							infoConsultaEtqs.getValidadorPropsEnt().executar(usu, etq);
						} catch (Exception e) {
							etq.getMensagensValidacao().add(e.toString());
						}
						cacheEtqs.put(tag.getGuid(), etq);
					}
				} else {
					for (Tag tag : lsTags) {
						Etiqueta etq = FabricaEntidade.getInstancia(infoConsultaEtqs.getTipoEntidade());
						infoConsultaEtqs.getIniciadorPropsEnt().executar(usu, tag, etq);
						cacheEtqs.put(tag.getGuid(), etq);
					}
				}
				if (emValidacao) {
					for (Etiqueta etq : cacheEtqs.values()) {
						try {
							infoConsultaEtqs.getIniciadorPropsRelEnt().executar(usu, etq.getMetadado(), etq);
						} catch (Exception e) {
							etq.getMensagensValidacao().add(e.toString());
						}
					}
				} else {
					for (Etiqueta etq : cacheEtqs.values()) {
						infoConsultaEtqs.getIniciadorPropsRelEnt().executar(usu, etq.getMetadado(), etq);
					}
				}
				cacheEtqs.getLogger().debug("atualizado");
			}
			return cacheEtqs;
		} catch (ErroModelo e) {
			throw new ErroModelo("Erro carregando cache de consultas: " + infoConsultaEtqs.getChaveCache(), e);
		}
	}
}
