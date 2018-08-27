package meyn.cevn.modelo.projeto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import com.evernote.edam.notestore.NoteMetadata;

import meyn.cevn.modelo.CacheTags;
import meyn.cevn.modelo.CadastroEtiqueta;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.Nota;
import meyn.cevn.modelo.Usuario;
import meyn.cevn.modelo.acao.Acao;
import meyn.cevn.modelo.acao.CadastroAcao;
import meyn.cevn.modelo.interesse.CadastroInteresse;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.referencia.CadastroReferencia;
import meyn.cevn.modelo.referencia.Referencia;
import meyn.cevn.modelo.sumario.CadastroSumario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

public class CadastroProjeto extends CadastroNota<Projeto> {

	private static final String REPOSITORIO = "2. Projetos";
	private static final String GRUPO = "<2. Projeto>";
	
	private final CadastroEtiqueta<Etiqueta> cadEtqProj = new CadastroEtiqueta<Etiqueta>("<2. Projeto>") {};

	public CadastroProjeto() throws ErroModelo {
		super(REPOSITORIO, GRUPO, true, false, false);
	}

	@Override
	protected void iniciarPropriedadesEnt(Usuario usu, NoteMetadata mtd, Projeto proj) throws ErroModelo {
		super.iniciarPropriedadesEnt(usu, mtd, proj);
		proj.setEtiqueta(cadEtqProj.consultarPorNome(usu, mtd.getTitle()));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void iniciarPropriedadesRelacionamentoEnt(Usuario usu, NoteMetadata mtd, Projeto proj) throws ErroModelo {
		String idEtq = proj.getEtiqueta().getId();

		CacheTags cacheTag = CacheTags.getCache(usu);
		List<String> lsIdsTag = new ArrayList<String>(proj.getMetadado().getTagGuids());
		lsIdsTag.remove(idEtq);

		Predicate<? extends Nota> ehDoProjeto = (nota) -> {
			List<String> lsIds = nota.getMetadado().getTagGuids();
			return lsIds != null && lsIds.contains(idEtq);
		};		

		// Interesses
		CadastroInteresse cadIntr = getCadastro(ChavesModelo.INTERESSE);
		Collection<Interesse> clIntrs = new ArrayList<Interesse>();
		for (String id : lsIdsTag) {
			clIntrs.add(cadIntr.consultarPorNome(usu, cacheTag.get(id).getName()));
		}
		proj.setInteresses(clIntrs);
		// Ações
		CadastroAcao cadAcao = getCadastro(ChavesModelo.ACAO);
		Predicate<Acao> ehAcaoProjeto = (Predicate<Acao>)ehDoProjeto;
		Predicate<Acao> ehAcaoSemData = (acao) -> acao.getDataLembrete() == null;
		proj.setAcoesEmFoco(cadAcao.consultarPorRepositorio(usu, CadastroAcao.REPOSITORIO_FOCO, ehAcaoProjeto.and(ehAcaoSemData)));
		proj.setAcoesDelegadas(cadAcao.consultarPorRepositorio(usu, CadastroAcao.REPOSITORIO_DELEGADA, ehAcaoProjeto.and(ehAcaoSemData)));
		proj.setAcoesProximas(cadAcao.consultarPorRepositorio(usu, CadastroAcao.REPOSITORIO_PROXIMA, ehAcaoProjeto.and(ehAcaoSemData)));
		List<Acao> lsAcoes = new ArrayList<Acao>(cadAcao.consultarPorFiltro(usu, ehAcaoProjeto.and(ehAcaoSemData.negate())));
		Collections.sort(lsAcoes, (a, b) -> a.getDataLembrete().compareTo(b.getDataLembrete()));
		proj.setAcoesCalendario(lsAcoes);
		// Referências
		CadastroReferencia cadRef = getCadastro(ChavesModelo.REFERENCIA);
		proj.setReferencias(cadRef.consultarPorFiltro(usu, (Predicate<Referencia>) ehDoProjeto));
		// Sumários
		CadastroSumario cadSum = getCadastro(ChavesModelo.SUMARIO);
		try {
			proj.setSumarioValidacao(cadSum.consultarValidacaoPorProjeto(usu, proj));
		} catch (ErroItemNaoEncontrado e) {
			proj.setSumarioValidacao(cadSum.gerarValidacaoProjeto(usu, proj));
		}
		try {
			proj.setSumario(cadSum.consultarPorProjeto(usu, proj));
		} catch (ErroItemNaoEncontrado e) {
			proj.setSumario(cadSum.gerarSumarioInicialProjeto(usu, proj));
		}
	}

	@Override
	public void validarPropriedadesEnt(Usuario usu, Projeto proj) {
		super.validarPropriedadesEnt(usu, proj);
		Collection<String> clMsgs = proj.getMensagensValidacao();
		// Etiqueta
		List<String> lsIdsTag = proj.getMetadado().getTagGuids();
		if (!lsIdsTag.contains(proj.getEtiqueta().getId())) {
			clMsgs.add("Etiqueta não encontrada: " + proj.getNome());
		}
		// Lembrete
		if (proj.isLembrete()) {
			clMsgs.add("Projeto com lembrete: " + proj.getNome());
		}
	}

	@Override
	public Projeto consultarPorChavePrimaria(Usuario usu, String id) throws ErroCadastro {
		try {
			return super.consultarPorChavePrimaria(usu, id);
		} catch (ErroItemNaoEncontrado e) {
			return consultarPorNome(usu, cadEtqProj.consultarPorChavePrimaria(usu, id).getNome());		
		}
	}
}
