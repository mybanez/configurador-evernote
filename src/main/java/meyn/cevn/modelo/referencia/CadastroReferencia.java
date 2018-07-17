package meyn.cevn.modelo.referencia;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.evernote.edam.notestore.NoteMetadata;

import meyn.cevn.modelo.CadastroEtiqueta;
import meyn.cevn.modelo.CadastroNota;
import meyn.cevn.modelo.ChavesModelo;
import meyn.cevn.modelo.Etiqueta;
import meyn.cevn.modelo.interesse.CadastroInteresse;
import meyn.cevn.modelo.interesse.Interesse;
import meyn.cevn.modelo.projeto.CadastroProjeto;
import meyn.cevn.modelo.projeto.Projeto;
import meyn.cevn.modelo.usuario.Usuario;
import meyn.util.modelo.ErroModelo;
import meyn.util.modelo.cadastro.ErroCadastro;
import meyn.util.modelo.cadastro.ErroItemNaoEncontrado;

public class CadastroReferencia extends CadastroNota<Referencia> {
	private static final String REPOSITORIO = "4. Referências";
	private static final String GRUPO = "<4. Referência>";

	private final CadastroEtiqueta<Etiqueta> cadForm = new CadastroEtiqueta<Etiqueta>("<Formato>") {};
	private final CadastroEtiqueta<Etiqueta> cadTema = new CadastroEtiqueta<Etiqueta>("<Tema>") {};

	public CadastroReferencia() throws ErroCadastro {
		super(REPOSITORIO, GRUPO, true, false, true);	
	}

	@Override
	protected void iniciarPropriedadesOT(Usuario usu, NoteMetadata mtd, Referencia ref) throws ErroModelo {
		super.iniciarPropriedadesOT(usu, mtd, ref);
		List<String> lsIdsTag = new ArrayList<String>(mtd.getTagGuids());
		Collection<Etiqueta> clFormatos = new ArrayList<Etiqueta>();
		Collection<Etiqueta> clTemas = new ArrayList<Etiqueta>();
		for (String id : lsIdsTag) {
			try {
				clFormatos.add(cadForm.consultarPorChavePrimaria(usu, id));
				continue;
			} catch (ErroItemNaoEncontrado e) {	}
			try {
				clTemas.add(cadTema.consultarPorChavePrimaria(usu, id));
			} catch (ErroItemNaoEncontrado e) {	}
		}
		ref.setFormatos(clFormatos);
		ref.setTemas(clTemas);
	}

	@Override
	protected void iniciarPropriedadesRelacionamentoOT(Usuario usu, NoteMetadata mtd, Referencia ref)
			throws ErroModelo {
		List<String> lsIdsTag = new ArrayList<String>(mtd.getTagGuids());
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
	public void validarPropriedadesOT(Usuario usu, Referencia ref) {
		super.validarPropriedadesOT(usu, ref);
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
}
