/* Padronização dos selects da interface web, sem jQuery. */
(() => {
    'use strict';

    const instancias = new WeakMap();

    function iniciar(select) {
        if (instancias.has(select) || typeof SlimSelect !== 'function') return;
        // Algumas ações administrativas existem em <details> fechados.
        // Inicializar somente quando visíveis evita medidas incorretas de largura.
        if (select.closest('details:not([open])')) return;

        const pesquisavel = select.dataset.slimSearch === 'search';
        const rotulo = document.querySelector(`label[for="${select.id}"]`);
        const nomeCampo = (rotulo?.textContent || 'opção').replace(/\s*\*\s*$/, '').trim();
        const primeiraOpcao = select.querySelector('option[value=""]');
        const instancia = new SlimSelect({
            select,
            settings: {
                showSearch: pesquisavel,
                ariaLabel: nomeCampo,
                searchPlaceholder: `Pesquisar ${nomeCampo.toLowerCase()}...`,
                searchText: 'Nenhum resultado encontrado',
                searchHighlight: pesquisavel,
                placeholderText: primeiraOpcao?.textContent?.trim() || 'Selecione uma opção',
                // Não permitir limpar uma escolha obrigatória usando um botão adicional.
                allowDeselect: false
            }
        });
        instancias.set(select, instancia);

        // O <select required> oculto pela biblioteca não consegue receber foco
        // diretamente quando inválido; abrir o componente preserva a validação.
        select.addEventListener('invalid', evento => {
            evento.preventDefault();
            select.closest('.admin-detail-field, .new-request-field, .admin-crud-field')
                ?.classList.add('cr-slim-error');
            if (select.id === 'funcionarioId') {
                const mensagem = document.getElementById('erroFuncionarioAtribuicao');
                if (mensagem) mensagem.hidden = false;
            }
            instancia.open();
        });
        select.addEventListener('change', () => {
            select.closest('.admin-detail-field, .new-request-field, .admin-crud-field')
                ?.classList.remove('cr-slim-error');
            if (select.id === 'funcionarioId') {
                const mensagem = document.getElementById('erroFuncionarioAtribuicao');
                if (mensagem) mensagem.hidden = true;
            }
        });
    }

    // O componente observa mutações das opções; esta atualização explícita
    // também mantém a seleção sincronizada quando o mapa filtra categorias.
    function atualizarOpcoes(select) {
        const instancia = instancias.get(select);
        if (!instancia) return;
        instancia.setData(Array.from(select.options, opcao => ({
            text: opcao.textContent,
            value: opcao.value,
            selected: opcao.selected,
            disabled: opcao.disabled,
            placeholder: opcao.value === ''
        })));
    }

    function definirValor(select, valor) {
        // Atualização nativa síncrona: outros filtros conseguem ler o valor
        // imediatamente, mesmo que o Slim Select renderize em lote.
        select.value = valor;
        const instancia = instancias.get(select);
        if (instancia) instancia.setSelected(valor);
    }

    // API pequena utilizada pelos filtros dinâmicos do mapa de chamados.
    window.CityReportSelects = { atualizarOpcoes, definirValor };

    document.addEventListener('DOMContentLoaded', () => {
        const selects = Array.from(document.querySelectorAll('select.cr-slim-select'));
        if (typeof SlimSelect !== 'function') return; // Fallback: <select> nativo.
        selects.forEach(select => {
            iniciar(select);
            for (let pai = select.parentElement; pai; pai = pai.parentElement) {
                if (pai instanceof HTMLDetailsElement) {
                    pai.addEventListener('toggle', () => iniciar(select));
                }
            }
        });
    });
})();
