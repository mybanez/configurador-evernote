package meyn.cevn.modelo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.text.StringEscapeUtils;

import com.evernote.thrift.TBase;

import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.CadastroImpl;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

public abstract class CadastroEvn<TipoMtd extends TBase<?>, TipoEnt extends EntidadeEvn<TipoMtd>> extends CadastroImpl<Usuario, TipoEnt> {

	protected static void desatualizarCaches(Usuario usu) throws ErroModelo {
		CacheNotasConsultas.getCache(usu).desatualizarCaches();
		CacheEtiquetasConsultas.getCache(usu).desatualizarCaches();
	}

	protected static void desatualizarCachesParaValidacao(Usuario usu) throws ErroModelo {
		CacheNotasConsultas.getCache(usu).desatualizarCachesParaValidacao();
		CacheEtiquetasConsultas.getCache(usu).desatualizarCachesParaValidacao();
	}

	private String nomeRepositorio;
	private String nomeGrupo;
	private Set<String> chavesCache = new HashSet<String>();
	private boolean entidadeValidavel;

	protected CadastroEvn(String nomeRepositorio, boolean entidadeValidavel) throws ErroCadastro {
		this(nomeRepositorio, "", entidadeValidavel);
	}

	protected CadastroEvn(String nomeRepositorio, String nomeGrupo, boolean entidadeValidavel) throws ErroCadastro {
		this.nomeRepositorio = nomeRepositorio;
		this.nomeGrupo = nomeGrupo;
		this.entidadeValidavel = entidadeValidavel;
	}

	protected String getNomeRepositorio() {
		return nomeRepositorio;
	}

	protected String getNomeGrupo() {
		return nomeGrupo;
	}

	protected Set<String> getChavesCache() {
		return chavesCache;
	}

	public boolean isEntidadeValidavel() {
		return entidadeValidavel;
	}

	protected abstract CacheEntidadesEvn<TipoEnt> getCache(Usuario usu) throws ErroModelo;

	protected abstract void iniciarPropriedadesEnt(Usuario usu, TipoMtd mtd, TipoEnt ent) throws ErroModelo;

	protected void iniciarPropriedadesRelacionamentoEnt(Usuario usu, TipoMtd mtd, TipoEnt ent) throws ErroModelo {
	}

	protected void validarPropriedadesEnt(Usuario usu, TipoEnt ent) throws ErroModelo {
	}

	public void desatualizarCache(Usuario usu) throws ErroModelo {
		CacheNotasConsultas.getCache(usu).desatualizarCaches(chavesCache);
		CacheEtiquetasConsultas.getCache(usu).desatualizarCaches(chavesCache);
	}

	@Override
	public Collection<TipoEnt> consultarTodos(Usuario usu) throws ErroCadastro {
		try {
			return getCache(usu).consultarTodos();
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens.", e);
		}
	}

	public Grupo<TipoEnt> consultarTodosPorGrupos(Usuario usu) throws ErroCadastro {
		try {
			return getCache(usu).consultarPorGrupo(nomeGrupo);
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens.", e);
		}
	}

	public Collection<TipoEnt> consultarPorRepositorio(Usuario usu, String nomeRepositorio, Predicate<TipoEnt> filtro) throws ErroCadastro {
		Collection<TipoEnt> clEnts = consultarPorRepositorio(usu, nomeRepositorio);
		clEnts.removeIf(filtro.negate());
		return clEnts;
	}

	public Collection<TipoEnt> consultarPorRepositorio(Usuario usu, String nomeRepositorio) throws ErroCadastro {
		try {
			return getCache(usu).consultarPorRepositorio(nomeRepositorio);
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens por repositório: " + nomeRepositorio, e);
		}
	}

	public Collection<TipoEnt> consultarPorFiltro(Usuario usu, Predicate<TipoEnt> filtro) throws ErroCadastro {
		try {
			return getCache(usu).consultarPorFiltro(filtro);
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens por filtro: " + filtro.getClass(), e);
		}
	}

	public Collection<TipoEnt> validarTodos(Usuario usu) throws ErroCadastro {
		try {
			desatualizarCachesParaValidacao(usu);
			Collection<TipoEnt> clEnts = consultarTodos(usu);
			return clEnts;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro validando itens: " + e, e);
		}
	}

	public TipoEnt validarPorChavePrimaria(Usuario usu, TipoEnt chave) throws ErroCadastro {
		return consultarPorChavePrimaria(usu, chave.getId());
	}

	public TipoEnt validarPorChavePrimaria(Usuario usu, String id) throws ErroCadastro {
		try {
			desatualizarCachesParaValidacao(usu);
			TipoEnt ent = consultarPorChavePrimaria(usu, id);
			return ent;
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro validando item: " + e, e);
		}
	}

	public TipoEnt consultarPorChavePrimaria(Usuario usu, TipoEnt chave) throws ErroCadastro {
		return consultarPorChavePrimaria(usu, chave.getId());
	}

	public TipoEnt consultarPorChavePrimaria(Usuario usu, String id) throws ErroCadastro {
		try {
			return getCache(usu).consultarPorChavePrimaria(id);
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando item: " + id, e);
		}
	}

	public TipoEnt consultarPorNome(Usuario usu, String nome) throws ErroCadastro {
		try {
			return getCache(usu).consultarPorNome(nome);
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando item: " + nome, e);
		}
	}

	//// GERAÇÃO DE ENML ////

	private static final String EXP_QUEBRA_LINHA = "(?m)^(.*)$";

	@FunctionalInterface
	protected interface GeradorENML<T> {
		void executar(StringBuffer cont, T item);
	}

	protected void gerarCabecalho(StringBuffer cont) {
		cont.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		cont.append("<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">");
		cont.append("<en-note>");
	}

	protected void gerarRodape(StringBuffer cont) {
		cont.append("</en-note>");
	}

	protected <TipoTitulo, TipoItem> void gerarListaItens(StringBuffer cont, TipoTitulo titulo, GeradorENML<TipoTitulo> grTitulo,
	        Collection<TipoItem> clItens, GeradorENML<TipoItem> grItem) {
		if (!clItens.isEmpty()) {
			grTitulo.executar(cont, titulo);
			gerarInicioLista(cont);
			for (TipoItem item : clItens) {
				grItem.executar(cont, item);
			}
			gerarFimLista(cont);
		}
	}

	protected void gerarItemURL(StringBuffer cont, String texto, String estiloTexto, String url) {
		gerarInicioItem(cont);
		gerarTextoURL(cont, texto, estiloTexto, url);
		gerarFimItem(cont);
	}

	protected void gerarItem(StringBuffer cont, String texto, String estiloTexto) {
		gerarInicioItem(cont);
		gerarTexto(cont, texto, estiloTexto);
		gerarFimItem(cont);
	}

	protected void gerarTextoURL(StringBuffer cont, String texto, String estiloTexto, String url) {
		cont.append("<a style=\"").append(estiloTexto).append("\" href=\"").append(url).append("\">")
		        .append(StringEscapeUtils.escapeXml11(texto)).append("</a>");
	}

	protected void gerarTextoMultilinha(StringBuffer cont, String texto, String estiloTexto) {
		cont.append("<span style=\"").append(estiloTexto).append("\">")
		        .append(StringEscapeUtils.escapeXml11(texto).replaceAll(EXP_QUEBRA_LINHA, "$1<br/>")).append("</span>");
	}

	protected void gerarTexto(StringBuffer cont, String texto, String estiloTexto) {
		cont.append("<span style=\"").append(estiloTexto).append("\">").append(StringEscapeUtils.escapeXml11(texto)).append("</span>");
	}

	protected void gerarInicioLinha(StringBuffer cont, String estiloTexto) {
		cont.append("<div style=\"").append(estiloTexto).append("\">");
	}

	protected void gerarInicioLinha(StringBuffer cont) {
		cont.append("<div>");
	}

	protected void gerarFimLinha(StringBuffer cont) {
		cont.append("</div>");
	}

	protected void gerarInicioLista(StringBuffer cont) {
		cont.append("<ul>");
	}

	protected void gerarFimLista(StringBuffer cont) {
		cont.append("</ul>");
	}

	protected void gerarInicioItem(StringBuffer cont) {
		cont.append("<li>");
	}

	protected void gerarFimItem(StringBuffer cont) {
		cont.append("</li>");
	}

	protected void gerarQuebraSecao(StringBuffer cont) {
		cont.append("<hr/>");
	}

	protected void gerarQuebraLinha(StringBuffer cont) {
		cont.append("<br/>");
	}
}
