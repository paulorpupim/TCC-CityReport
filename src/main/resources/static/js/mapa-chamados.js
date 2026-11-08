/* Mapa administrativo do CityReport: filtros locais, sem chamadas adicionais a API. */
(() => {
    'use strict';

    const elMapa = document.getElementById('mapa-chamados');
    const elContagem = document.getElementById('mapa-contagem');
    const elAviso = document.getElementById('mapa-aviso');
    const botaoEnquadrar = document.getElementById('mapa-centralizar');
    if (!elMapa || !elContagem) return;

    const filtros = {
        busca: document.getElementById('filtro-busca'),
        status: document.getElementById('filtro-status'),
        prioridade: document.getElementById('filtro-prioridade'),
        setor: document.getElementById('filtro-setor'),
        categoria: document.getElementById('filtro-categoria')
    };

    const nomesStatus = {
        ENVIADO: 'Enviado', AGUARDANDO_INFORMACOES: 'Aguardando informações',
        ABERTO: 'Aberto', ATRIBUIDO: 'Atribuído', EM_EXECUCAO: 'Em execução',
        PENDENTE: 'Pendente', AGUARDANDO_VALIDACAO: 'Aguardando validação',
        RESOLVIDO: 'Resolvido', RECUSADO: 'Recusado', CANCELADO: 'Cancelado',
        DUPLICADO: 'Duplicado'
    };
    const nomesPrioridade = { CRITICA: 'Crítica', ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };
    const cores = {
        CRITICA: '#c62828', ALTA: '#e36b22', MEDIA: '#efb92b',
        BAIXA: '#2376bb', INDEFINIDA: '#66758a',
        RESOLVIDO: '#13865d', ENCERRADO: '#9ba4b3'
    };
    const encerrados = new Set(['CANCELADO', 'RECUSADO', 'DUPLICADO']);

    const pontos = Array.from(document.querySelectorAll('#mapa-registros .mapa-registro'))
        .map(el => {
            const p = el.dataset;
            return {
                id: Number(p.id), protocolo: p.protocolo || '', titulo: p.titulo || '',
                endereco: p.endereco || '', setorId: p.setorId || '', setor: p.setor || '',
                categoriaId: p.categoriaId || '', categoria: p.categoria || '',
                status: p.status || '', prioridade: p.prioridade || '',
                urgencia: p.urgencia || '', lat: Number(p.latitude), lng: Number(p.longitude)
            };
        })
        .filter(p => Number.isSafeInteger(p.id) && p.id > 0
            && Number.isFinite(p.lat) && Math.abs(p.lat) <= 90
            && Number.isFinite(p.lng) && Math.abs(p.lng) <= 180);

    function criarOpcao(valor, rotulo) {
        const option = document.createElement('option');
        option.value = valor;
        option.textContent = rotulo;
        return option;
    }

    function montarSelects() {
        const setores = new Map();
        const categorias = new Map();
        pontos.forEach(p => {
            setores.set(p.setorId, p.setor);
            categorias.set(p.categoriaId, { nome: p.categoria, setor: p.setor, setorId: p.setorId });
        });
        Array.from(setores.entries()).sort((a, b) => a[1].localeCompare(b[1], 'pt-BR'))
            .forEach(([id, nome]) => filtros.setor.appendChild(criarOpcao(id, nome)));
        return categorias;
    }

    const categorias = montarSelects();
    function atualizarCategorias() {
        const categoriaAtual = filtros.categoria.value;
        filtros.categoria.replaceChildren(criarOpcao('', 'Todas as categorias'));
        Array.from(categorias.entries())
            .filter(([, cat]) => !filtros.setor.value || cat.setorId === filtros.setor.value)
            .sort((a, b) => (a[1].nome + a[1].setor).localeCompare(b[1].nome + b[1].setor, 'pt-BR'))
            .forEach(([id, cat]) => filtros.categoria.appendChild(
                criarOpcao(id, cat.nome + ' · ' + cat.setor)));
        if (Array.from(filtros.categoria.options).some(o => o.value === categoriaAtual)) {
            filtros.categoria.value = categoriaAtual;
        }
    }
    atualizarCategorias();

    if (typeof L === 'undefined') {
        elContagem.textContent = `${pontos.length} chamados com localização cadastrada.`;
        elMapa.textContent = 'Não foi possível carregar o mapa. Confira a conexão com a internet.';
        botaoEnquadrar.disabled = true;
        return;
    }

    const mapa = L.map(elMapa, { scrollWheelZoom: false }).setView([-14.235, -51.925], 4);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors'
    }).addTo(mapa);
    const marcadores = L.featureGroup().addTo(mapa);

    function adicionarTexto(pai, classe, conteudo, tag = 'span') {
        const el = document.createElement(tag);
        el.className = classe;
        el.textContent = conteudo;
        pai.appendChild(el);
        return el;
    }

    function criarPopup(registros) {
        const wrapper = document.createElement('div');
        wrapper.className = 'mapa-popup';
        if (registros.length > 1) {
            adicionarTexto(wrapper, 'mapa-popup-quantidade',
                `${registros.length} chamados nesta localização`, 'p');
        }
        registros.forEach(p => {
            const item = document.createElement('div');
            item.className = 'mapa-popup-item';
            adicionarTexto(item, 'mapa-popup-titulo', `${p.protocolo} · ${p.titulo}`, 'strong');
            adicionarTexto(item, 'mapa-popup-info', `${p.categoria} · ${p.setor}`);
            adicionarTexto(item, 'mapa-popup-info', `Status: ${nomesStatus[p.status] || p.status}`);
            adicionarTexto(item, 'mapa-popup-info',
                `Prioridade oficial: ${nomesPrioridade[p.prioridade] || 'Ainda não definida'}`);
            if (!p.prioridade && p.urgencia) {
                adicionarTexto(item, 'mapa-popup-info',
                    `Urgência informada: ${nomesPrioridade[p.urgencia] || p.urgencia}`);
            }
            if (p.endereco) adicionarTexto(item, 'mapa-popup-info', `Endereço: ${p.endereco}`);
            const link = document.createElement('a');
            link.className = 'mapa-popup-link';
            link.href = `/atendente/chamados/${p.id}`;
            link.textContent = 'Ver detalhes do chamado';
            item.appendChild(link);
            wrapper.appendChild(item);
        });
        return wrapper;
    }

    function corDoGrupo(registros) {
        // Em pontos sobrepostos, nunca esconder uma ocorrencia critica atras
        // de um chamado ja resolvido que tenha sido registrado mais tarde.
        const ativos = registros.filter(p => p.status !== 'RESOLVIDO' && !encerrados.has(p.status));
        if (ativos.length) {
            for (const prioridade of ['CRITICA', 'ALTA', 'MEDIA', 'BAIXA']) {
                if (ativos.some(p => p.prioridade === prioridade)) return cores[prioridade];
            }
            return cores.INDEFINIDA;
        }
        if (registros.some(p => p.status === 'RESOLVIDO')) return cores.RESOLVIDO;
        return cores.ENCERRADO;
    }

    function criarMarcador(registros) {
        const primeiro = registros[0];
        const cor = corDoGrupo(registros);
        const quantidade = registros.length;
        const icone = L.divIcon({
            className: 'mapa-pin-wrapper',
            html: `<span class="mapa-pin${quantidade > 1 ? ' mapa-pin-grupo' : ''}" style="--cor-pin:${cor}">${quantidade > 1 ? quantidade : ''}</span>`,
            iconSize: quantidade > 1 ? [36, 36] : [30, 30],
            iconAnchor: quantidade > 1 ? [18, 18] : [15, 15]
        });
        const marcador = L.marker([primeiro.lat, primeiro.lng], { icon: icone });
        marcador.bindPopup(criarPopup(registros), { minWidth: 240, maxWidth: 330 });
        marcador.addTo(marcadores);
    }

    function normalizar(texto) {
        return texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('pt-BR');
    }

    function corresponde(p) {
        const busca = normalizar(filtros.busca.value.trim());
        return (!busca || normalizar(`${p.protocolo} ${p.titulo}`).includes(busca))
            && (!filtros.status.value || p.status === filtros.status.value)
            && (!filtros.prioridade.value || (filtros.prioridade.value === 'SEM_PRIORIDADE'
                ? !p.prioridade : p.prioridade === filtros.prioridade.value))
            && (!filtros.setor.value || p.setorId === filtros.setor.value)
            && (!filtros.categoria.value || p.categoriaId === filtros.categoria.value);
    }

    function enquadrarMarcadores() {
        if (marcadores.getLayers().length === 0) return;
        mapa.fitBounds(marcadores.getBounds().pad(0.13), { maxZoom: 15 });
    }

    function aplicarFiltros() {
        marcadores.clearLayers();
        const filtrados = pontos.filter(corresponde);
        // Pontos coincidentes aparecem em um unico marcador com lista de chamados.
        const grupos = new Map();
        filtrados.forEach(p => {
            const chave = `${p.lat.toFixed(5)},${p.lng.toFixed(5)}`;
            if (!grupos.has(chave)) grupos.set(chave, []);
            grupos.get(chave).push(p);
        });
        grupos.forEach(criarMarcador);
        const total = pontos.length;
        elContagem.textContent = `Exibindo ${filtrados.length} de ${total} chamados com localização (${grupos.size} pontos no mapa).`;
        botaoEnquadrar.disabled = grupos.size === 0;
        elAviso.hidden = grupos.size !== 0;
        elAviso.textContent = total === 0
            ? 'Nenhum chamado possui coordenadas válidas no momento.'
            : 'Nenhum chamado com localização corresponde aos filtros selecionados.';
        if (grupos.size > 0) enquadrarMarcadores();
    }

    Object.values(filtros).forEach(el => {
        el.addEventListener(el === filtros.busca ? 'input' : 'change', () => {
            if (el === filtros.setor) atualizarCategorias();
            aplicarFiltros();
        });
    });
    document.getElementById('limpar-filtros').addEventListener('click', () => {
        Object.values(filtros).forEach(el => { el.value = ''; });
        atualizarCategorias();
        aplicarFiltros();
        filtros.busca.focus();
    });
    botaoEnquadrar.addEventListener('click', enquadrarMarcadores);
    aplicarFiltros();
})();
