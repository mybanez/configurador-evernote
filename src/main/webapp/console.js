function testarResultadoGeracaoSumariosParametro() {
	var res = document.getElementById('resultadoParam');
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

function atualizarTituloPainel(nome) {
	var tabs = document.querySelectorAll("a[href='#paineis:tab-"+nome+"']");
	var qtd = document.getElementById('paineis:'+nome+':titulo');
	tabs[0].innerHTML = qtd.value;
}

function ligarAtualizadorTela() {
	window.atualizadorAtivo = true;
	desativarBotoes(true);
	mostrarImagemProcessando(true);
	PF('atualizadorTela').start();
}

function desligarAtualizadorTela() {
	if (window.atualizadorAtivo == true) {
		var processando = document.getElementById('paineis:console:processando');
		var saida = document.getElementById('paineis:console:saida:padrao');
		if (processando.value == 'false') {
			PF('atualizadorTela').stop();
			mostrarImagemProcessando(false);
			desativarBotoes(false);
			var resultado = document.getElementById('paineis:console:resultado');
			var msgSumario, msgDetalhe, msgSeveridade;
			if (resultado.value == 'ok') {
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
			window.atualizadorAtivo = false;
		}
	}
}

function atualizarPosicaoJanela() {
	var element = document.getElementById('paineis:console:saida:janela');
	element.scrollTop = element.scrollHeight;
}