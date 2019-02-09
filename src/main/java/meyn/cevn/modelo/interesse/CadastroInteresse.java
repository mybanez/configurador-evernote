package meyn.cevn.modelo.interesse;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.evernote.edam.type.Tag;

import meyn.cevn.modelo.CadastroEtiqueta;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.Usuario;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.acao.CadastroAcao;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.referencia.CadastroReferencia;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.CadastroSumario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.Modelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

@Modelo(ChavesModelo.INTERESSE)
public class CadastroInteresse extends CadastroEtiqueta<Interesse> {
	private static final String REPOSITORIO = "<1. Interesse>";

	public CadastroInteresse() throws ErroModelo {
		super(REPOSITORIO);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void iniciarPropriedadesRelacionamentoEnt(Usuario usu, Tag mtd, Interesse intr) throws ErroCadastro {
		Predicate<? extends Nota> ehDoInteresse = (nota) -> {
			List<String> lsIds = nota.getMetadado().getTagGuids();
			return lsIds != null && lsIds.contains(intr.getId());
		};
		// Projetos
		CadastroProjeto cadProj = getCadastro(ChavesModelo.PROJETO);
		intr.setProjetos(cadProj.consultarPorFiltro(usu, (Predicate<Projeto>) ehDoInteresse));
		// Ações
		if (consultarPorRepositorio(usu, "<Empregador>").contains(intr)) {
			CadastroAcao cadAcao = getCadastro(ChavesModelo.ACAO);
			intr.setAcoes(cadAcao.consultarPorFiltro(usu, (Predicate<Acao>) ehDoInteresse));
		} else {
			intr.setAcoes(Collections.<Acao>emptyList());
		}
		// Referências
		CadastroReferencia cadRef = getCadastro(ChavesModelo.REFERENCIA);
		intr.setReferencias(cadRef.consultarPorFiltro(usu, (Predicate<Referencia>) ehDoInteresse));
		intr.setReferenciasPorFormato(cadRef.consultarPorGrupo(usu, "<Formato>").filtrar((Predicate<Referencia>) ehDoInteresse));
		// Sumário
		CadastroSumario cadSum = getCadastro(ChavesModelo.SUMARIO);
		try {
			intr.setSumario(cadSum.consultarPorInteresse(usu, intr));
		} catch (ErroItemNaoEncontrado e) {
			getLogger().warn("Sumário não encontrado: {}", intr.getNome());
		}
	}
}
