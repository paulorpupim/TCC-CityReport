// Pesquisa apenas nos chamados já listados para o cidadão autenticado.
// O HTML mantém a lista completa caso o navegador esteja com JavaScript desativado.
(() => {
    'use strict';

    function normalizar(texto) {
        return String(texto || '')
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .toLocaleLowerCase('pt-BR')
            .trim();
    }

    document.addEventListener('DOMContentLoaded', () => {
        const caixa = document.getElementById('pesquisa-chamados-cidadao');
        const entrada = document.getElementById('busca-meus-chamados');
        const limpar = document.getElementById('limpar-busca-chamados');
        const contagem = document.getElementById('resultado-busca-chamados');
        const semResultados = document.getElementById('nenhum-chamado-encontrado');
        const cartoes = Array.from(document.querySelectorAll('.citizen-request-card'));

        if (!caixa || !entrada || !limpar || !contagem || !semResultados || cartoes.length === 0) {
            return;
        }

        // Evita ler cada cartão em todos os pressionamentos de tecla.
        const chamados = cartoes.map(cartao => ({
            elemento: cartao,
            texto: normalizar([
                cartao.querySelector('.citizen-request-protocol')?.textContent,
                cartao.querySelector('.citizen-request-title')?.textContent
            ].join(' '))
        }));

        function pesquisar() {
            const termo = normalizar(entrada.value);
            let visiveis = 0;

            chamados.forEach(({ elemento, texto }) => {
                const corresponde = termo === '' || texto.includes(termo);
                elemento.hidden = !corresponde;
                if (corresponde) visiveis++;
            });

            limpar.hidden = termo === '';
            semResultados.hidden = visiveis !== 0;
            contagem.textContent = termo
                ? `${visiveis} de ${chamados.length} chamado(s) encontrado(s)`
                : 'Pesquise por protocolo ou título para localizar um chamado.';
        }

        entrada.addEventListener('input', pesquisar);
        limpar.addEventListener('click', () => {
            entrada.value = '';
            pesquisar();
            entrada.focus();
        });

        caixa.hidden = false;
        pesquisar();
    });
})();
