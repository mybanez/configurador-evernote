package meyn.cevn.modelo.referencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import com.evernote.edam.notestore.NoteMetadata;

import meyn.cevn.modelo.CadastroEtiqueta;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.Usuario;
import meyn.cevn.modelo.interesse.CadastroInteresse;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.Modelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

@Modelo(ChavesModelo.REFERENCIA)
public class CadastroReferencia extends CadastroNota<Referencia> {

	private static final String REPOSITORIO = "4. Referências";
	private static final String GRUPO = "<4. Referência>";

	private final CadastroEtiqueta<Etiqueta> cadForm = new CadastroEtiqueta<Etiqueta>("<Formato>") {
	};
	private final CadastroEtiqueta<Etiqueta> cadTema = new CadastroEtiqueta<Etiqueta>("<Tema>") {
	};

	public CadastroReferencia() throws ErroModelo {
		super(REPOSITORIO, GRUPO, true, true, true);
	}

	@Override
	protected void iniciarPropriedadesEnt(Usuario usu, NoteMetadata mtd, Referencia ref) throws ErroModelo {
		super.iniciarPropriedadesEnt(usu, mtd, ref);
		List<String> lsIdsTag = mtd.getTagGuids();
		if (lsIdsTag == null) {
			lsIdsTag = Collections.emptyList();
		}
		Collection<Etiqueta> clFormatos = new ArrayList<Etiqueta>();
		Collection<Etiqueta> clTemas = new ArrayList<Etiqueta>();
		for (String id : lsIdsTag) {
			try {
				clFormatos.add(cadForm.consultarPorChavePrimaria(usu, id));
				continue;
			} catch (ErroItemNaoEncontrado e) {
			}
			try {
				clTemas.add(cadTema.consultarPorChavePrimaria(usu, id));
			} catch (ErroItemNaoEncontrado e) {
			}
		}
		ref.setFormatos(clFormatos);
		ref.setTemas(clTemas);
	}

	@Override
	protected void iniciarPropriedadesRelacionamentoEnt(Usuario usu, NoteMetadata mtd, Referencia ref) throws ErroCadastro {
		List<String> lsIdsTag = mtd.getTagGuids();
		lsIdsTag = lsIdsTag == null ? Collections.emptyList() : new ArrayList<String>(lsIdsTag);
		for (Etiqueta etq : ref.getFormatos()) {
			lsIdsTag.remove(etq.getId());
		}
		for (Etiqueta etq : ref.getTemas()) {
			lsIdsTag.remove(etq.getId());
		}

		// Interesses
		CadastroInteresse cadIntr = getCadastro(ChavesModelo.INTERESSE);
		Collection<Interesse> clIntrs = new ArrayList<Interesse>();
		ListIterator<String> iter = lsIdsTag.listIterator();
		while (iter.hasNext()) {
			try {
				clIntrs.add(cadIntr.consultarPorChavePrimaria(usu, iter.next()));
				iter.remove();
			} catch (ErroItemNaoEncontrado e) {
			}
		}
		ref.setInteresses(clIntrs);

		// Projetos
		CadastroProjeto cadProj = getCadastro(ChavesModelo.PROJETO);
		Collection<Projeto> clProjs = new ArrayList<Projeto>();
		for (String id : lsIdsTag) {
			clProjs.add(cadProj.consultarPorChavePrimaria(usu, id));
		}
		ref.setProjetos(clProjs);
	}

	@Override
	protected void validarPropriedadesEnt(Usuario usu, Referencia ref) {
		super.validarPropriedadesEnt(usu, ref);
		Collection<String> clMsgs = ref.getMensagensValidacao();
		// Formato
		if (ref.getFormatos().isEmpty()) {
			clMsgs.add("Referência sem formato definido");
		}
		// Lembrete
		if (ref.isLembrete()) {
			clMsgs.add("Referência com lembrete");
		}
	}

	@Override
	public void desatualizarCache(Usuario usu) throws ErroModelo {
		super.desatualizarCache(usu);
		cadForm.desatualizarCache(usu);
		cadTema.desatualizarCache(usu);
	}
}
