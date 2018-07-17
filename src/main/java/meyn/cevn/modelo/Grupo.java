package meyn.cevn.modelo;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Grupo<TipoOT extends OTEvn<?>> {
	private static final String EXP_NOME_GRUPO = "^<?([^<>]+)>?";

	private String nome;
	private TipoOT ot;
	private SortedSet<Grupo<TipoOT>> stGrpsFilhos;
	private SortedSet<TipoOT> stOtsFilho;

	public Grupo(String nome) {
		this(nome, null);
	}

	public Grupo(String nome, TipoOT ot) {
		setNome(nome);
		this.ot = ot;
		stGrpsFilhos = new TreeSet<Grupo<TipoOT>>((a, b) -> a.getNome().compareTo(b.getNome()));
		//Viabiliza nomes iguais no conjunto
		stOtsFilho = new TreeSet<TipoOT>((a, b) -> {
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

	public TipoOT getOT() {
		return ot;
	}

	public void setOT(TipoOT ot) {
		this.ot = ot;
	}

	public Set<Grupo<TipoOT>> getGruposFilho() {
		return stGrpsFilhos;
	}

	public SortedSet<TipoOT> getOtsFilho() {
		return stOtsFilho;
	}

	public boolean isVazio() {
		return stGrpsFilhos.isEmpty() && stOtsFilho.isEmpty();
	}
}
