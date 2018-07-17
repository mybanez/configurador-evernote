package meyn.cevn.modelo;

import com.evernote.edam.type.Tag;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;

public abstract class CadastroEtiqueta<TipoEtq extends Etiqueta> extends CadastroEvn<Tag, TipoEtq> {

	protected class InfoCachePadrao extends CacheEtiquetasConsulta.Info<TipoEtq> {

		public InfoCachePadrao() {
			this(getMoldeOTPadrao());
			getChavesCache().add(getChave());
		}

		public InfoCachePadrao(Class<?> moldeEtq) {
			setMoldeOT(moldeEtq);

			setIniciadorPropsOT((Usuario usu, Tag mtd, TipoEtq etq) -> {
				iniciarPropriedadesOT(usu, mtd, etq);
			});
			setIniciadorPropsRelOT((Usuario usu, Tag mtd, TipoEtq etq) -> {
				iniciarPropriedadesRelacionamentoOT(usu, mtd, etq);
			});
			setValidadorPropsOT((Usuario usu, TipoEtq etq) -> {
				validarPropriedadesOT(usu, etq);
			});

			setNomeRepositorio(CadastroEtiqueta.this.getNomeRepositorio());
		}
	}

	private final InfoCachePadrao infoCacheMoldePadrao;

	protected CadastroEtiqueta(String nomeRepositorio) throws ErroCadastro {
		super(nomeRepositorio, nomeRepositorio);
		infoCacheMoldePadrao = new InfoCachePadrao();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected CacheOTEvn<TipoEtq> getCache(Usuario usu, Class<?> moldeEtq) throws ErroModelo {
		return (CacheEtiquetas<TipoEtq>) CacheEtiquetasConsulta.getCache(usu).get(usu,
				moldeEtq.equals(getMoldeOTPadrao()) ? infoCacheMoldePadrao : new InfoCachePadrao(moldeEtq));
	}

	protected final void iniciarPropriedadesOT(Usuario usu, Tag mtd, TipoEtq etq) throws ErroModelo {
		etq.setMetadado(mtd);
		etq.setId(mtd.getGuid());
		etq.setNome(mtd.getName());
	}
}
