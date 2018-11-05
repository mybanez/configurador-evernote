package meyn.cevn.modelo;

import com.evernote.edam.type.Tag;

import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;

public abstract class CadastroEtiqueta<TipoEtq extends Etiqueta> extends CadastroEvn<Tag, TipoEtq> {

	protected class ConsultaPadrao extends CacheEtiquetasConsultas.InfoConsulta<TipoEtq> {

		ConsultaPadrao() {
			setTipoEntidade(CadastroEtiqueta.this.getTipoEntidade());
			setEntidadeValidavel(CadastroEtiqueta.this.isEntidadeValidavel());
			setIniciadorPropsEnt((Usuario usu, Tag mtd, TipoEtq etq) -> {
				iniciarPropriedadesEnt(usu, mtd, etq);
			});
			setIniciadorPropsRelEnt((Usuario usu, Tag mtd, TipoEtq etq) -> {
				iniciarPropriedadesRelacionamentoEnt(usu, mtd, etq);
			});
			setValidadorPropsEnt((Usuario usu, TipoEtq etq) -> {
				validarPropriedadesEnt(usu, etq);
			});
			setLogger(CadastroEtiqueta.this.getLogger());
			setNomeRepositorio(CadastroEtiqueta.this.getNomeRepositorio());
			getChavesCache().add(getChaveCache());
		}
	}

	private final ConsultaPadrao consultaPadrao;

	protected CadastroEtiqueta(String nomeRepositorio) throws ErroCadastro {
		this(nomeRepositorio, false);
	}
	
	protected CadastroEtiqueta(String nomeRepositorio, boolean validavel) throws ErroCadastro {
		super(nomeRepositorio, nomeRepositorio, validavel);
		consultaPadrao = new ConsultaPadrao();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected CacheEntidadesEvn<TipoEtq> getCache(Usuario usu) throws ErroModelo {
		return (CacheEtiquetas<TipoEtq>) CacheEtiquetasConsultas.getCache(usu).get(usu, consultaPadrao);
	}

	@Override
	protected final void iniciarPropriedadesEnt(Usuario usu, Tag mtd, TipoEtq etq) {
		etq.setMetadado(mtd);
		etq.setId(mtd.getGuid());
		etq.setNome(mtd.getName());
	}
}
