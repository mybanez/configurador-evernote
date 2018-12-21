package meyn.cevn.modelo;

import meyn.cevn.ContextoEvn;
import meyn.util.Cache;
import meyn.util.modelo.ErroModelo;

/* Todo cache de metadados do Evn deve ser carregado de uma vez, de forma sincronizada. Apenas 
 * acessos de leitura podem eventualmente ser concorrentes, caso múltiplos threads sejam criados 
 * para o atendimento de requisições do usuário. Isto deve ser permitido se a falha na leitura 
 * de um item que ainda não foi carregado não gerar efeitos colaterais indesejados.
 */

@SuppressWarnings("serial")
class CacheEvn<TipoChave, TipoValor> extends Cache<TipoChave, TipoValor> {

	protected static <TipoChave, TipoValor> CacheEvn<TipoChave, TipoValor> getCache(Usuario usu,
	        Class<? extends CacheEvn<TipoChave, TipoValor>> tipo) throws ErroModelo {
		return (CacheEvn<TipoChave, TipoValor>) getCache(ContextoEvn.getContexto(usu), tipo);
	}

	protected Usuario getUsuario() {
		return ((ContextoEvn) getContexto()).getUsuario();
	}
}
