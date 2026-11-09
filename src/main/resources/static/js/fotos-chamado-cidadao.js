// Destaque do envio de fotos e prévia local, sem enviar arquivos antes da confirmação.
(() => {
    'use strict';

    const secaoRecente = document.querySelector('.citizen-detail-photos--destaque');
    if (secaoRecente) {
        const reduzirMovimento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
        window.requestAnimationFrame(() => {
            secaoRecente.scrollIntoView({ behavior: reduzirMovimento ? 'auto' : 'smooth', block: 'start' });
        });
    }

    const formulario = document.getElementById('form-foto-problema');
    const arquivo = document.getElementById('arquivo');
    const previa = document.getElementById('foto-previa');
    const imagem = document.getElementById('foto-previa-imagem');
    const nome = document.getElementById('foto-previa-nome');
    const erro = document.getElementById('foto-erro');
    if (!formulario || !arquivo || !previa || !imagem || !nome || !erro) return;

    const tiposPermitidos = new Set(['image/jpeg', 'image/png', 'image/webp']);
    const tamanhoMaximo = 5 * 1024 * 1024;
    let urlPrevia = null;

    function limparPrevia() {
        if (urlPrevia) {
            URL.revokeObjectURL(urlPrevia);
            urlPrevia = null;
        }
        imagem.removeAttribute('src');
        nome.textContent = '';
        previa.hidden = true;
    }

    function mensagemErro(texto) {
        erro.textContent = texto;
        erro.hidden = !texto;
    }

    arquivo.addEventListener('change', () => {
        limparPrevia();
        mensagemErro('');
        const selecionado = arquivo.files && arquivo.files[0];
        if (!selecionado) return;

        if (!tiposPermitidos.has(selecionado.type)) {
            arquivo.value = '';
            mensagemErro('Escolha uma imagem JPG, PNG ou WebP.');
            return;
        }
        if (selecionado.size > tamanhoMaximo) {
            arquivo.value = '';
            mensagemErro('A fotografia deve ter no máximo 5 MB.');
            return;
        }
        if (selecionado.size === 0) {
            arquivo.value = '';
            mensagemErro('O arquivo selecionado está vazio.');
            return;
        }

        urlPrevia = URL.createObjectURL(selecionado);
        imagem.src = urlPrevia;
        nome.textContent = selecionado.name;
        previa.hidden = false;
    });

    formulario.addEventListener('submit', () => {
        // O navegador e o backend continuam responsáveis pelas validações finais.
        const botao = formulario.querySelector('button[type="submit"]');
        if (botao && arquivo.files && arquivo.files.length > 0) {
            botao.disabled = true;
            botao.textContent = 'Enviando fotografia...';
        }
    });

    window.addEventListener('pagehide', limparPrevia);
})();
