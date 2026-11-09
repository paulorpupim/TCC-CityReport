/* Cadastro administrativo: exibir setor somente para funcionário de campo. */
(() => {
    'use strict';

    document.addEventListener('DOMContentLoaded', () => {
        const form = document.querySelector('form.admin-crud-form');
        const radios = form?.querySelectorAll('input[type="radio"][name="perfil"]');
        const campoSetor = document.getElementById('campoSetor');
        const setor = document.getElementById('setorId');

        // A edição mantém o perfil fixo e exibe o setor apenas para funcionários.
        if (!form || !radios?.length || !campoSetor || !setor) return;

        function atualizarCampoSetor() {
            const funcionario = form.querySelector('input[name="perfil"]:checked')?.value === 'FUNCIONARIO';
            const selects = window.CityReportSelects;

            if (funcionario) {
                // Mostrar antes de iniciar o Slim Select evita largura incorreta.
                campoSetor.hidden = false;
                setor.required = true;
                if (selects) {
                    selects.definirHabilitado(setor, true);
                    selects.iniciar(setor);
                } else {
                    setor.disabled = false;
                }
            } else {
                // Um setor antigo não deve ser enviado ao cadastrar atendente.
                if (setor.value) {
                    if (selects) selects.definirValor(setor, '');
                    else setor.value = '';
                }
                setor.required = false;
                if (selects) selects.definirHabilitado(setor, false);
                else setor.disabled = true;
                campoSetor.hidden = true;
            }
        }

        radios.forEach(radio => radio.addEventListener('change', atualizarCampoSetor));
        atualizarCampoSetor();
    });
})();
