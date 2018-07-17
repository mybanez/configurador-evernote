package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.List;

import com.evernote.edam.type.Tag;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.contexto.Contexto;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.ot.FabricaOT;

@SuppressWarnings("serial")
public class CacheEtiquetasConsulta extends CacheResultadosConsulta {

	public static class Info<TipoEtq extends Etiqueta>
			extends CacheResultadosConsulta.Info<Tag, TipoEtq> {

		private String nomeRepositorio;

		public String getNomeRepositorio() {
			return nomeRepositorio;
		}

		public void setNomeRepositorio(String nomeRepositorio) {
			this.nomeRepositorio = nomeRepositorio;
		}

		public String getChave() {
			return getNomeRepositorio() + "-" + getMoldeOT().getName();
		}

		public CacheEtiquetas<TipoEtq> getInstancia(Contexto ctx) {
			return new CacheEtiquetas<TipoEtq>() {
				{
					setContexto(ctx);
				}
			};
		}
	}

	public static CacheEtiquetasConsulta getCache(Usuario usu) throws ErroModelo {
		return (CacheEtiquetasConsulta) getCache(usu, CacheEtiquetasConsulta.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CacheOTEvn<?> get(Usuario usu, CacheResultadosConsulta.Info<?, ?> infoCache) throws ErroModelo {
		Info<Etiqueta> infoCacheEtqs = (Info<Etiqueta>) infoCache;
		try {
			String chave = infoCacheEtqs.getChave();
			CacheOTEvn<?> cache = get(chave);
			if (cache == null || !cache.isAtualizado()) {
				CacheEtiquetas<Etiqueta> cacheEtqs = infoCacheEtqs.getInstancia(getContexto());
				cacheEtqs.setAtualizado(true);
				put(chave, cacheEtqs);
				CacheTags cacheTag = CacheTags.getCache(usu);
				List<String> lsIdsTags = cacheTag.consultarPorRepositorio(infoCacheEtqs.getNomeRepositorio());
				if (isEmValidacao()) {
					for (String id : lsIdsTags) {
						Tag tag = cacheTag.get(id);
						Etiqueta etq = FabricaOT.getInstancia(infoCacheEtqs.getMoldeOT());
						etq.setMensagensValidacao(new ArrayList<String>());
						try {
							infoCacheEtqs.getIniciadorPropsOT().executar(usu, tag, etq);
							infoCacheEtqs.getValidadorPropsOT().executar(usu, etq);
						} catch (Throwable t) {
							etq.getMensagensValidacao().add(t.toString());
						}
						cacheEtqs.put(id, etq);
					}
					for (Etiqueta etq : cacheEtqs.values()) {
						try {
							infoCacheEtqs.getIniciadorPropsRelOT().executar(usu, etq.getMetadado(), etq);
						} catch (Throwable t) {
							etq.getMensagensValidacao().add(t.toString());
						}
					}
				} else {
					for (String id : lsIdsTags) {
						Tag tag = cacheTag.get(id);
						Etiqueta etq = FabricaOT.getInstancia(infoCacheEtqs.getMoldeOT());
						infoCacheEtqs.getIniciadorPropsOT().executar(usu, tag, etq);
						cacheEtqs.put(id, etq);
					}
					for (Etiqueta etq : cacheEtqs.values()) {
						infoCacheEtqs.getIniciadorPropsRelOT().executar(usu, etq.getMetadado(), etq);
					}
				}
				cache = cacheEtqs;
				cache.getLogger().debug("atualizado - "+chave);
			}
			return cache;
		} catch (ErroModelo e) {
			throw new ErroModelo("Erro atualizando cache: " + infoCacheEtqs.getChave(), e);
		}
	}

}
