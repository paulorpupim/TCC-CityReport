/* CityReport — visualizador ampliado das fotografias dos chamados.
 * Mantem o href original como alternativa quando o navegador nao suporta <dialog>.
 * Nao realiza uploads nem muda a autorizacao do endpoint de imagens.
 */
(() => {
    'use strict';

    const links = document.querySelectorAll('a[data-cr-photo-lightbox]');
    if (!links.length || typeof HTMLDialogElement === 'undefined' ||
        typeof HTMLDialogElement.prototype.showModal !== 'function') {
        return;
    }

    const dialogo = document.createElement('dialog');
    dialogo.className = 'cr-photo-lightbox';
    dialogo.setAttribute('aria-label', 'Fotografia ampliada');
    dialogo.innerHTML = `
        <div class="cr-photo-lightbox__conteudo">
            <button type="button" class="cr-photo-lightbox__fechar" aria-label="Fechar fotografia">&times;</button>
            <div class="cr-photo-lightbox__visualizacao">
                <img class="cr-photo-lightbox__imagem" alt="" hidden>
                <p class="cr-photo-lightbox__estado" role="status">Carregando fotografia...</p>
            </div>
            <p class="cr-photo-lightbox__legenda"></p>
        </div>`;
    document.body.appendChild(dialogo);

    const botaoFechar = dialogo.querySelector('.cr-photo-lightbox__fechar');
    const imagem = dialogo.querySelector('.cr-photo-lightbox__imagem');
    const estado = dialogo.querySelector('.cr-photo-lightbox__estado');
    const legenda = dialogo.querySelector('.cr-photo-lightbox__legenda');
    let linkAnterior = null;

    botaoFechar.addEventListener('click', () => dialogo.close());
    dialogo.addEventListener('click', (evento) => {
        // Um clique na area escurecida fora da caixa fecha o visualizador.
        if (evento.target === dialogo ||
            evento.target.classList?.contains('cr-photo-lightbox__visualizacao')) {
            dialogo.close();
        }
    });
    dialogo.addEventListener('close', () => {
        imagem.removeAttribute('src');
        imagem.hidden = true;
        if (linkAnterior?.isConnected) linkAnterior.focus();
        linkAnterior = null;
    });

    links.forEach((link) => {
        link.addEventListener('click', (evento) => {
            // Permite os atalhos tradicionais do navegador (Ctrl/Cmd + clique etc.).
            if (evento.defaultPrevented || evento.button !== 0 || evento.ctrlKey ||
                evento.metaKey || evento.shiftKey || evento.altKey) return;

            evento.preventDefault();
            linkAnterior = link;
            const miniatura = link.querySelector('img');
            const descricao = miniatura?.alt || 'Fotografia do chamado';

            imagem.hidden = true;
            imagem.alt = descricao;
            legenda.textContent = descricao;
            estado.textContent = 'Carregando fotografia...';
            estado.hidden = false;
            imagem.onload = () => {
                imagem.hidden = false;
                estado.hidden = true;
            };
            imagem.onerror = () => {
                imagem.hidden = true;
                estado.textContent = 'Não foi possível carregar a fotografia.';
                estado.hidden = false;
            };

            dialogo.showModal();
            imagem.src = link.href;
            botaoFechar.focus();
        });
    });
})();
