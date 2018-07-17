package meyn.cevn.modelo;

import java.util.Collection;

import com.evernote.thrift.TBase;

import meyn.util.modelo.ot.OT;

public interface OTEvn<TipoMtd extends TBase<?>> extends OT {
	TipoMtd getMetadado();
	void setMetadado(TipoMtd mtd);
	String getId();
	void setId(String id);
	String getNome();
	void setNome(String nome);
	Collection<String> getMensagensValidacao();
	void setMensagensValidacao(Collection<String> clMsgs);
}
