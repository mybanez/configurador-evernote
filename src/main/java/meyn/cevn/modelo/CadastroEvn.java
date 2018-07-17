package meyn.cevn.modelo;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.text.StringEscapeUtils;

import com.evernote.thrift.TBase;

import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.CadastroImpl;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

public abstract class CadastroEvn<TipoMtd extends TBase<?>, TipoOT extends OTEvn<TipoMtd>>
		extends CadastroImpl<Usuario, TipoOT> {

	private static void ativarValidacaoOT(Usuario usu) throws ErroModelo {
		CacheNotasConsulta.getCache(usu).ativarValidacaoOT();
		CacheEtiquetasConsulta.getCache(usu).ativarValidacaoOT();
	}

	private static void desativarValidacaoOT(Usuario usu) throws ErroModelo {
		CacheNotasConsulta.getCache(usu).desativarValidacaoOT();
		CacheEtiquetasConsulta.getCache(usu).desativarValidacaoOT();
	}

	private String nomeRepositorio;
	private String nomeGrupo;
	private boolean cacheInvalidoAposAtualizacao;
	private Set<String> chavesCache = new HashSet<String>();

	protected CadastroEvn(String nomeRepositorio) throws ErroCadastro {
		this(nomeRepositorio, "");
	}

	protected CadastroEvn(String nomeRepositorio, String nomeGrupoPadrao) throws ErroCadastro {
		this(nomeRepositorio, nomeGrupoPadrao, false);
	}

	protected CadastroEvn(String nomeRepositorio, boolean cacheInvalidoAposAtualizacao) throws ErroCadastro {
		this(nomeRepositorio, "", cacheInvalidoAposAtualizacao);
	}

	protected CadastroEvn(String nomeRepositorio, String nomeGrupoPadrao, boolean cacheInvalidoAposAtualizacao) throws ErroCadastro {
		this.nomeRepositorio = nomeRepositorio;
		this.nomeGrupo = nomeGrupoPadrao;
		this.cacheInvalidoAposAtualizacao = cacheInvalidoAposAtualizacao;
	}

	protected final String getNomeRepositorio() {
		return nomeRepositorio;
	}

	protected final void setNomeRepositorio(String nomeRepositorio) {
		this.nomeRepositorio = nomeRepositorio;
	}

	public String getNomeGrupo() {
		return nomeGrupo;
	}

	public void setNomeGrupo(String nomeGrupoPadrao) {
		this.nomeGrupo = nomeGrupoPadrao;
	}

	public boolean isCacheInvalidoAposAtualizacao() {
		return cacheInvalidoAposAtualizacao;
	}

	public void setCacheInvalidoAposAtualizacao(boolean cacheInvalidoAposAtualizacao) {
		this.cacheInvalidoAposAtualizacao = cacheInvalidoAposAtualizacao;
	}

	protected Set<String> getChavesCache() {
		return chavesCache;
	}

	protected void setChavesCache(Set<String> chavesCache) {
		this.chavesCache = chavesCache;
	}

	protected void invalidarCaches(Usuario usu) throws ErroModelo {
		if (isCacheInvalidoAposAtualizacao()) {
			List<CacheResultadosConsulta> lsCaches = Arrays.asList(CacheNotasConsulta.getCache(usu),
					CacheEtiquetasConsulta.getCache(usu));
			chaves: for (String chave : chavesCache) {
				for (CacheResultadosConsulta cacheResultados : lsCaches) {
					if (cacheResultados.containsKey(chave)) {
						cacheResultados.get(chave).setAtualizado(false);
						continue chaves;
					}
				}
			}
		}
	}

	protected abstract CacheOTEvn<TipoOT> getCache(Usuario usu, Class<?> moldeOT) throws ErroModelo;

	protected abstract void iniciarPropriedadesOT(Usuario usu, TipoMtd mtd, TipoOT ot) throws ErroModelo;

	protected void iniciarPropriedadesRelacionamentoOT(Usuario usu, TipoMtd mtd, TipoOT ot) throws ErroModelo {
	}

	protected void validarPropriedadesOT(Usuario usu, TipoOT ot) throws ErroModelo {
	}

	@Override
	public Collection<TipoOT> consultarTodos(Usuario usu, Class<?> moldeOT) throws ErroCadastro {
		try {
			return getCache(usu, moldeOT).consultarTodos();
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens.", e);
		}
	}
	
	public Grupo<TipoOT> consultarTodosPorGrupos(Usuario usu) throws ErroCadastro {
		return consultarTodosPorGrupos(usu, getMoldeOTPadrao());
	}	
		
	public Grupo<TipoOT> consultarTodosPorGrupos(Usuario usu, Class<?> moldeOT) throws ErroCadastro {
		try {
			return getCache(usu, moldeOT).consultarPorGrupo(nomeGrupo);
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens.", e);
		}
	}
	
	public Collection<TipoOT> consultarPorRepositorio(Usuario usu, String nomeRepositorio) throws ErroCadastro {
		return consultarPorRepositorio(usu, nomeRepositorio, getMoldeOTPadrao());
	}

	public Collection<TipoOT> consultarPorRepositorio(Usuario usu, String nomeRepositorio, Predicate<TipoOT> filtro)
			throws ErroCadastro {
		return consultarPorRepositorio(usu, nomeRepositorio, filtro, getMoldeOTPadrao());
	}

	public Collection<TipoOT> consultarPorRepositorio(Usuario usu, String nomeRepositorio, Predicate<TipoOT> filtro,
			Class<?> moldeOT) throws ErroCadastro {
		Collection<TipoOT> clOTs = consultarPorRepositorio(usu, nomeRepositorio, moldeOT);
		clOTs.removeIf(filtro.negate());
		return clOTs;
	}

	public Collection<TipoOT> consultarPorRepositorio(Usuario usu, String nomeRepositorio, Class<?> moldeOT)
			throws ErroCadastro {
		try {
			return getCache(usu, moldeOT).consultarPorRepositorio(nomeRepositorio);
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens por repositório: " + nomeRepositorio, e);
		}
	}

	public Collection<TipoOT> consultarPorFiltro(Usuario usu, Predicate<TipoOT> filtro) throws ErroCadastro {
		return consultarPorFiltro(usu, filtro, getMoldeOTPadrao());
	}

	public Collection<TipoOT> consultarPorFiltro(Usuario usu, Predicate<TipoOT> filtro, Class<?> moldeOT)
			throws ErroCadastro {
		try {
			return getCache(usu, moldeOT).consultarPorFiltro(filtro);
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando itens por filtro: " + filtro.getClass(), e);
		}
	}

	public Collection<TipoOT> validarTodos(Usuario usu) throws ErroCadastro {
		try {
			ativarValidacaoOT(usu);
			Collection<TipoOT> clOTs = consultarTodos(usu);
			desativarValidacaoOT(usu);
			return clOTs;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro validando itens: " + e, e);
		}
	}
	
	public TipoOT validarPorChavePrimaria(Usuario usu, TipoOT chave) throws ErroCadastro {
		return consultarPorChavePrimaria(usu, chave.getId());
	}

	public TipoOT validarPorChavePrimaria(Usuario usu, String id) throws ErroCadastro {
		try {
			ativarValidacaoOT(usu);
			TipoOT OT = consultarPorChavePrimaria(usu, id);
			desativarValidacaoOT(usu);
			return OT;
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro validando item: " + e, e);
		}
	}

	@Override
	public TipoOT consultarPorChavePrimaria(Usuario usu, TipoOT chave, Class<?> moldeOT) throws ErroCadastro {
		return consultarPorChavePrimaria(usu, chave.getId(), moldeOT);
	}

	public TipoOT consultarPorChavePrimaria(Usuario usu, String id) throws ErroCadastro {
		return consultarPorChavePrimaria(usu, id, getMoldeOTPadrao());
	}

	public TipoOT consultarPorChavePrimaria(Usuario usu, String id, Class<?> moldeOT) throws ErroCadastro {
		try {
			return getCache(usu, moldeOT).consultarPorChavePrimaria(id);
		} catch (ErroItemNaoEncontrado e) {
			throw e;
		} catch (ErroModelo e) {
			throw new ErroCadastro("Erro consultando item: " + id, e);
		}
	}

	public TipoOT consultarPorNome(Usuario usu, String nome) throws ErroCadastro {
		return consultarPorNome(usu, nome, getMoldeOTPadrao());
	}

	public TipoOT consultarPorNome(Usuario usu, String nome, Class<?> moldeOT) throws ErroCadastro {
		try {
			return getCache(usu, moldeOT).consultarPorNome(nome);
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
	
	protected <TipoTitulo, TipoItem> void gerarListaItens(StringBuffer cont, TipoTitulo titulo,
			GeradorENML<TipoTitulo> grTitulo, Collection<TipoItem> clItens, GeradorENML<TipoItem> grItem) {
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
		cont.append("<span style=\"").append(estiloTexto).append("\">").append(StringEscapeUtils.escapeXml11(texto))
				.append("</span>");
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
