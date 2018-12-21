package meyn.cevn.modelo;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Grupo<TipoEnt extends EntidadeEvn<?>> {
	private static final String EXP_NOME_GRUPO = "^<?([^<>]+)>?";

	private String nome;
	private TipoEnt ent;
	private SortedSet<Grupo<TipoEnt>> stGrpsFilhos;
	private SortedSet<TipoEnt> stEntsFilho;

	public Grupo(String nome) {
		this(nome, null);
	}

	public Grupo(String nome, TipoEnt ent) {
		setNome(nome);
		this.ent = ent;
		stGrpsFilhos = new TreeSet<Grupo<TipoEnt>>((a, b) -> a.getNome().compareTo(b.getNome()));
		// Viabiliza nomes iguais no conjunto
		stEntsFilho = new TreeSet<TipoEnt>((a, b) -> {
			int comp = a.getNome().compareTo(b.getNome());
			return comp != 0 ? comp : 1;
		});
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome.replaceFirst(EXP_NOME_GRUPO, "$1");
	}

	public TipoEnt getEntidade() {
		return ent;
	}

	public void setEntidade(TipoEnt ent) {
		this.ent = ent;
	}

	public Set<Grupo<TipoEnt>> getGruposFilho() {
		return stGrpsFilhos;
	}

	public SortedSet<TipoEnt> getEntidadesFilho() {
		return stEntsFilho;
	}

	public boolean isVazio() {
		return stGrpsFilhos.isEmpty() && stEntsFilho.isEmpty();
	}
}
