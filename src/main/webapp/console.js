function testarResultadoOperacao() {
	var res = document.getElementById('resultado');
	if (res != null && res.value == 'ok') {
		alert('Sumário(s) gerado(s) com sucesso!');
		close();
	}
}

function trocarCheckBoxes(fonte, chave) {
	var chks = document.getElementById('console').getElementsByTagName('input');
	if (chave == null) {
		chave = fonte.id;
	}
	for (var i = 0; i < chks.length; i++) {
		if (chks[i].id.indexOf(chave) != -1) {
			chks[i].checked = fonte.checked;
		}
	}
}

function desativarBotoes(desativado) {
	var botoes = document.getElementsByTagName("button");
	for (var i = 0; i < botoes.length; i++) {
		botoes[i].disabled = desativado;
	}
}

function mostrarImagemProcessando(mostrar) {
	var painel = document.getElementById('painelImgProc');
	painel.style.display = (mostrar ? 'block' : 'none');
}

function ligarAtualizadorTela() {
	desativarBotoes(true);
	mostrarImagemProcessando(true);
	PF('atualizadorTela').start();
}

function desligarAtualizadorTela() {
	var processos = document.getElementById('paineis:console:processos');
	var saida = document.getElementById('paineis:console:saida:padrao');
	if (processos.value == 0 && saida.innerHTML.indexOf('PROCESSO FINALIZADO') == -1) {
		PF('atualizadorTela').stop();
		mostrarImagemProcessando(false);
		desativarBotoes(false);
		var status = document.getElementById('paineis:console:status');
		var msgSumario, msgDetalhe, msgSeveridade;
		if (status.value == 'ok') {
			msgSumario = 'Sucesso!';
			msgDetalhe = 'Processamento finalizado';
			msgSeveridade = 'info';
		} else {
			msgSumario = 'Erro!';
			msgDetalhe = 'Processamento finalizado: consultar console/log';
			msgSeveridade = 'error';
		}
		saida.innerHTML += '\n' + msgDetalhe.toUpperCase();
		PF('mensagem').renderMessage({
			'summary' : msgSumario,
			'detail' : msgDetalhe,
			'severity' : msgSeveridade
		});
	}
}

function atualizarPosicaoJanela() {
	var element = document.getElementById('paineis:console:saida:janela');
	element.scrollTop = element.scrollHeight;
}