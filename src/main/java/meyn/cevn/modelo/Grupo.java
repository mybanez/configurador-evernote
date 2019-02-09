package meyn.cevn.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

public class Grupo<TipoEnt extends EntidadeEvn<?>> {
	private static final String EXP_NOME_GRUPO = "^<?([^<>]+)>?";

	@SuppressWarnings("serial")
	private class SortedArrayList extends ArrayList<TipoEnt> {

		boolean atualizado = true;
		
		@Override
		public boolean add(TipoEnt ent) {
			atualizado = false;
			return super.add(ent);
		}

		@Override
		public boolean remove(Object o) {
			atualizado = false;
			return super.remove(o);
		}
		
		SortedArrayList ordenar() {
			if (!atualizado) {
				Collections.sort(this, (a, b) -> a.getNome().compareTo(b.getNome()));
				atualizado = true;
			}
			return this;
		}
	}
	
	private String nome;
	private TipoEnt ent;
	private SortedSet<Grupo<TipoEnt>> stGrpsFilho = new TreeSet<Grupo<TipoEnt>>((a, b) -> a.nome.compareTo(b.nome));
	private SortedArrayList lsEntsFilho = new SortedArrayList();
	
	public Grupo(String nome) {
		this(nome, null);
	}

	public Grupo(String nome, TipoEnt ent) {
		setNome(nome);
		this.ent = ent;
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
		return stGrpsFilho;
	}

	public List<TipoEnt> getEntidadesFilho() {
		return lsEntsFilho.ordenar();
	}

	public boolean isVazio() {
		return stGrpsFilho.isEmpty() && lsEntsFilho.isEmpty();
	}
	
	public Grupo<TipoEnt> filtrar(Predicate<TipoEnt> filtro) {
		Grupo<TipoEnt> grpEnts = new Grupo<TipoEnt>(nome, ent);
		grpEnts.lsEntsFilho.addAll(lsEntsFilho);
		grpEnts.lsEntsFilho.removeIf(filtro.negate());
		for (Grupo<TipoEnt> grpFilho: stGrpsFilho) {
			grpFilho = grpFilho.filtrar(filtro);
			if (!grpFilho.isVazio()) {
				grpEnts.stGrpsFilho.add(grpFilho);
			}
		}
		return grpEnts;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Nome: ").append(nome).append("\n");
		if (ent != null) {
			sb.append("Entidade:\n").append(ent);
		}
		if (!stGrpsFilho.isEmpty()) {
			sb.append("Grupos filhos:\n");
			for (Grupo<TipoEnt> grp : stGrpsFilho) {
				sb.append('-').append(grp.nome).append('\n');
			}
		}
		if (!lsEntsFilho.isEmpty()) {
			sb.append("Entidades filhas:\n");
			for (TipoEnt ent : lsEntsFilho) {
				sb.append('-').append(ent.getNome()).append('\n');
			}
		}
		return sb.toString();
	}
}
