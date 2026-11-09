// Filtra as categorias pelo setor escolhido, inclusive quando Slim Select está ativo.
(() => {
    'use strict';
    document.addEventListener('DOMContentLoaded', () => {
        const setor = document.getElementById('rel-setor');
        const categoria = document.getElementById('rel-categoria');
        const inicial = document.getElementById('rel-data-inicial');
        const final = document.getElementById('rel-data-final');
        if (!setor || !categoria) return;

        const opcoesOriginais = Array.from(categoria.options, opcao => opcao.cloneNode(true));
        function atualizarCategorias() {
            const idSetor = setor.value;
            const categoriaAtual = categoria.value;
            const opcoesVisiveis = opcoesOriginais
                .filter(opcao => !opcao.value || !idSetor || opcao.dataset.setorId === idSetor)
                .map(opcao => opcao.cloneNode(true));
            categoria.replaceChildren(...opcoesVisiveis);
            categoria.value = opcoesVisiveis.some(opcao => opcao.value === categoriaAtual)
                ? categoriaAtual : '';
            window.CityReportSelects?.atualizarOpcoes(categoria);
        }
        setor.addEventListener('change', atualizarCategorias);
        atualizarCategorias();

        // Feedback imediato sem impedir a validação definitiva no Spring Boot.
        inicial?.addEventListener('change', () => {
            if (final) final.min = inicial.value;
        });
        if (inicial && final) final.min = inicial.value;
    });
})();
