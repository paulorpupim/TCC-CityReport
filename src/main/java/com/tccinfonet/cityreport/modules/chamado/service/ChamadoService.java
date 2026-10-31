package com.tccinfonet.cityreport.modules.chamado.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.categoria.service.CategoriaService;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.enums.TipoFoto;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.service.FotoChamadoService;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.enums.TipoAtualizacao;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado._protocolo_contador.service.ProtocoloService;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.repository.ChamadoRepository;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final ProtocoloService protocoloService;
    private final UsuarioService usuarioService;
    private final CategoriaService categoriaService;
    private final HistoricoChamadoService historicoChamadoService;
    private final FotoChamadoService fotoChamadoService;

    // Consultas administrativas: lista todos os chamados e busca um pelo ID.
    public List<Chamado> listarTodos() {
        return chamadoRepository.findAllByOrderByDataCriacaoDesc();
    }

    // Pesquisa administrativa: filtros opcionais combinados diretamente no banco.
    // Os chamados mais recentes aparecem primeiro, mesmo quando nao ha filtros.
    public List<Chamado> pesquisarChamados(String busca, StatusChamado status,
                                          Prioridade prioridade, Long categoriaId) {
        String termo = busca == null ? "" : busca.trim();
        if (termo.length() > 150) {
            throw new IllegalArgumentException("A pesquisa deve ter no maximo 150 caracteres.");
        }

        Specification<Chamado> filtros = (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();

            if (!termo.isEmpty()) {
                String padrao = "%" + termo.toLowerCase(Locale.ROOT) + "%";
                condicoes.add(cb.or(
                        cb.like(cb.lower(root.get("protocolo")), padrao),
                        cb.like(cb.lower(root.get("titulo")), padrao)
                ));
            }
            if (status != null) {
                condicoes.add(cb.equal(root.get("status"), status));
            }
            if (prioridade != null) {
                condicoes.add(cb.equal(root.get("prioridadeOficial"), prioridade));
            }
            if (categoriaId != null) {
                condicoes.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }

            return cb.and(condicoes.toArray(new Predicate[0]));
        };

        return chamadoRepository.findAll(filtros,
                Sort.by(Sort.Direction.DESC, "dataCriacao", "id"));
    }

    public Chamado buscarPorId(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chamado nao encontrado."));
    }

    // O cidadao so pode consultar os chamados registrados por sua propria conta.
    public List<Chamado> listarDoCidadao(String emailAutenticado) {
        Usuario usuario = usuarioService.buscarPorEmail(emailAutenticado);
        return chamadoRepository.findByCidadaoIdOrderByDataCriacaoDesc(usuario.getId());
    }

    public Chamado buscarDoCidadao(Long chamadoId, String emailAutenticado) {
        Usuario usuario = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarPorId(chamadoId);

        if (!Objects.equals(chamado.getCidadao().getId(), usuario.getId())) {
            throw new IllegalArgumentException("Voce nao pode consultar este chamado.");
        }

        return chamado;
    }

    // Cadastro: nunca aceita do formulario o cidadao, o status ou a prioridade oficial.
    @Transactional
    public Chamado cadastrar(Chamado dados, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        if (!Boolean.TRUE.equals(cidadao.getAtivo())) {
            throw new IllegalArgumentException("O usuario esta inativo.");
        }

        String titulo = validarTexto(dados.getTitulo(), "O titulo", 150);
        String descricao = validarTexto(dados.getDescricao(), "A descricao", 2000);

        if (dados.getUrgenciaPercebida() == null) {
            throw new IllegalArgumentException("Informe a urgencia percebida.");
        }

        if (dados.getCategoria() == null || dados.getCategoria().getId() == null) {
            throw new IllegalArgumentException("Selecione uma categoria.");
        }

        Categoria categoria = categoriaService.buscarPorId(dados.getCategoria().getId());
        validarCategoriaAtiva(categoria);

        String endereco = dados.getEndereco() == null ? "" : dados.getEndereco().trim();
        validarLocalizacao(endereco, dados.getLatitude(), dados.getLongitude());

        Chamado chamado = new Chamado();
        chamado.setProtocolo(protocoloService.gerarProtocolo());
        chamado.setTitulo(titulo);
        chamado.setDescricao(descricao);
        chamado.setEndereco(endereco.isEmpty() ? null : endereco);
        chamado.setLatitude(dados.getLatitude());
        chamado.setLongitude(dados.getLongitude());
        chamado.setUrgenciaPercebida(dados.getUrgenciaPercebida());
        chamado.setCategoria(categoria);
        chamado.setCidadao(cidadao);
        chamado.setStatus(StatusChamado.ENVIADO);

        Chamado salvo = chamadoRepository.save(chamado);
        historicoChamadoService.registrar(
                salvo, cidadao, "Chamado registrado.", null, StatusChamado.ENVIADO);

        return salvo;
    }

    // O envio da foto inicial e opcional. Somente o autor pode anexar uma foto do problema.
    @Transactional
    public FotoChamado adicionarFotoProblema(Long chamadoId, MultipartFile arquivo, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);

        FotoChamado foto = fotoChamadoService.salvarFoto(chamado, cidadao, TipoFoto.PROBLEMA, arquivo);
        historicoChamadoService.registrar(chamado, cidadao, "Foto do problema adicionada.", null, null);
        return foto;
    }

    // O autor pode cancelar enquanto o servico nunca tiver sido iniciado.
    @Transactional
    public Chamado cancelar(Long chamadoId, String justificativa, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);

        if (chamado.getPrimeiroInicioExecucao() != null) {
            throw new IllegalArgumentException("O chamado nao pode ser cancelado depois do inicio da execucao.");
        }
        if (chamado.getStatus() != StatusChamado.ENVIADO
                && chamado.getStatus() != StatusChamado.AGUARDANDO_INFORMACOES
                && chamado.getStatus() != StatusChamado.ABERTO
                && chamado.getStatus() != StatusChamado.ATRIBUIDO) {
            throw new IllegalArgumentException("Nao e possivel cancelar este chamado.");
        }

        // O cancelamento do original deixaria os cidadaos dos duplicados sem atendimento.
        if (chamadoRepository.existsByChamadoOriginalId(chamadoId)) {
            throw new IllegalArgumentException(
                    "Este chamado possui duplicados vinculados e nao pode ser cancelado.");
        }

        String motivo = validarTexto(justificativa, "A justificativa", 1700);
        StatusChamado anterior = chamado.getStatus();
        chamado.setStatus(StatusChamado.CANCELADO);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, cidadao,
                "Chamado cancelado. Justificativa: " + motivo,
                anterior, StatusChamado.CANCELADO, TipoAtualizacao.CANCELAMENTO);
        return chamado;
    }

    // Acompanhamento do cidadao: duplicados exibem o andamento do original.
    public StatusChamado consultarAndamento(Long chamadoId, String emailAutenticado) {
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);
        if (chamado.getStatus() == StatusChamado.DUPLICADO) {
            return chamado.getChamadoOriginal().getStatus();
        }
        return chamado.getStatus();
    }

    // O autor responde e o chamado volta para a fila de triagem.
    @Transactional
    public Chamado complementarInformacoes(Long chamadoId, String resposta, String emailAutenticado) {
        Usuario cidadao = usuarioService.buscarPorEmail(emailAutenticado);
        Chamado chamado = buscarDoCidadao(chamadoId, emailAutenticado);
        exigirStatus(chamado, StatusChamado.AGUARDANDO_INFORMACOES);

        String mensagem = validarTexto(resposta, "A resposta", 1700);
        chamado.setStatus(StatusChamado.ENVIADO);
        chamadoRepository.save(chamado);
        historicoChamadoService.registrar(chamado, cidadao,
                "Informacoes complementares: " + mensagem,
                StatusChamado.AGUARDANDO_INFORMACOES, StatusChamado.ENVIADO,
                TipoAtualizacao.RESPOSTA_INFORMACOES);
        return chamado;
    }

    private void validarCategoriaAtiva(Categoria categoria) {
        if (!Boolean.TRUE.equals(categoria.getAtivo())
                || categoria.getSetor() == null
                || !Boolean.TRUE.equals(categoria.getSetor().getAtivo())) {
            throw new IllegalArgumentException("A categoria ou seu setor esta inativo.");
        }
    }

    private void exigirStatus(Chamado chamado, StatusChamado esperado) {
        if (chamado.getStatus() != esperado) {
            throw new IllegalArgumentException("Operacao permitida somente para chamados com status " + esperado + ".");
        }
    }

    private String validarTexto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " e obrigatorio.");
        }

        String texto = valor.trim();
        if (texto.length() > limite) {
            throw new IllegalArgumentException(campo + " deve ter no maximo " + limite + " caracteres.");
        }

        return texto;
    }

    private void validarLocalizacao(String endereco, Double latitude, Double longitude) {
        if (endereco.length() > 255) {
            throw new IllegalArgumentException("O endereco deve ter no maximo 255 caracteres.");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Informe latitude e longitude juntas.");
        }
        if (endereco.isBlank() && latitude == null) {
            throw new IllegalArgumentException("Informe um endereco ou selecione o local no mapa.");
        }
        if (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new IllegalArgumentException("As coordenadas informadas sao invalidas.");
        }
    }
}
