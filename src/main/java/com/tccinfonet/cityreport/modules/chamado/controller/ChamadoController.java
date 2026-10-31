package com.tccinfonet.cityreport.modules.chamado.controller;

import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tccinfonet.cityreport.modules.categoria.entity.Categoria;
import com.tccinfonet.cityreport.modules.categoria.service.CategoriaService;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.enums.TipoFoto;
import com.tccinfonet.cityreport.modules.chamado._foto_chamado.service.FotoChamadoService;
import com.tccinfonet.cityreport.modules.chamado._historico_chamado.service.HistoricoChamadoService;
import com.tccinfonet.cityreport.modules.chamado.dto.CriarChamadoDTO;
import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.Prioridade;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.service.ChamadoAtendenteService;
import com.tccinfonet.cityreport.modules.chamado.service.ChamadoService;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;
import com.tccinfonet.cityreport.modules.usuario.enums.PerfilUsuario;
import com.tccinfonet.cityreport.modules.usuario.service.UsuarioService;

import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final ChamadoAtendenteService chamadoAtendenteService;
    private final CategoriaService categoriaService;
    private final UsuarioService usuarioService;
    private final FotoChamadoService fotoChamadoService;
    private final HistoricoChamadoService historicoChamadoService;

    // ======================= AREA DO CIDADAO =======================
    // Tambem pode ser utilizada na web por atendentes e funcionarios como usuarios comuns.

    @GetMapping("/chamados")
    public String listarMeusChamados(Authentication autenticacao, Model model) {
        model.addAttribute("chamados", chamadoService.listarDoCidadao(autenticacao.getName()));
        return "chamado/lista";
    }

    @GetMapping("/chamados/novo")
    public String novo(Model model) {
        model.addAttribute("chamadoDTO", new CriarChamadoDTO());
        return prepararCadastro(model);
    }

    @PostMapping("/chamados")
    public String cadastrar(@Valid @ModelAttribute("chamadoDTO") CriarChamadoDTO chamadoDTO,
                            BindingResult resultado,
                            Authentication autenticacao,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (resultado.hasErrors()) {
            return prepararCadastro(model);
        }

        try {
            Chamado dados = converterParaChamado(chamadoDTO);
            Chamado chamado = chamadoService.cadastrar(dados, autenticacao.getName());

            redirectAttributes.addFlashAttribute("mensagemSucesso", "Chamado registrado com sucesso.");
            return "redirect:/chamados/" + chamado.getId();
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            resultado.reject("chamado.invalido", e.getMessage());
            return prepararCadastro(model);
        }
    }

    @GetMapping("/chamados/{id}")
    public String detalhesDoCidadao(@PathVariable Long id, Authentication autenticacao, Model model) {
        // A verificacao da autoria ocorre ANTES de consultar fotos e historico.
        Chamado chamado = chamadoService.buscarDoCidadao(id, autenticacao.getName());
        model.addAttribute("chamado", chamado);
        model.addAttribute("statusAcompanhamento", chamadoService.consultarAndamento(id, autenticacao.getName()));
        model.addAttribute("historico", historicoChamadoService.listarPorChamado(id));
        model.addAttribute("atualizacoes", historicoChamadoService.listarAtualizacoesDoCidadao(id));
        // Fotos do problema e somente a solucao efetivamente aprovada.
        model.addAttribute("fotos", fotoChamadoService.listarFotosParaCidadao(chamado));
        return "chamado/detalhes";
    }

    // O envio da foto inicial e opcional e ocorre depois de criar o chamado.
    @PostMapping("/chamados/{id}/fotos")
    public String adicionarFoto(@PathVariable Long id,
                                @RequestParam("arquivo") MultipartFile arquivo,
                                Authentication autenticacao,
                                RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoService.adicionarFotoProblema(id, arquivo, autenticacao.getName()),
                "Foto do problema adicionada.", "/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/chamados/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @RequestParam String justificativa,
                           Authentication autenticacao,
                           RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoService.cancelar(id, justificativa, autenticacao.getName()),
                "Chamado cancelado.", "/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/chamados/{id}/complementar")
    public String complementar(@PathVariable Long id,
                               @RequestParam String resposta,
                               Authentication autenticacao,
                               RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoService.complementarInformacoes(id, resposta, autenticacao.getName()),
                "Informacoes enviadas.", "/chamados/" + id, redirectAttributes);
    }

    // Exibe uma foto SOMENTE depois de verificar o acesso ao chamado.
    // E uma rota compartilhada pelo cidadao, atendente e funcionario responsavel.
    @GetMapping("/chamados/{id}/fotos/{fotoId}/imagem")
    public ResponseEntity<Resource> visualizarFoto(@PathVariable Long id,
                                                   @PathVariable Long fotoId,
                                                   Authentication autenticacao) {
        Usuario usuario = usuarioService.buscarPorEmail(autenticacao.getName());
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new AccessDeniedException("Usuario inativo.");
        }

        Chamado chamado;
        try {
            chamado = chamadoService.buscarPorId(id);
        } catch (EntityNotFoundException erro) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chamado nao encontrado.");
        }

        boolean atendente = usuario.getPerfil() == PerfilUsuario.ATENDENTE;
        boolean autor = chamado.getCidadao() != null
                && Objects.equals(chamado.getCidadao().getId(), usuario.getId());
        boolean funcionarioResponsavel = usuario.getPerfil() == PerfilUsuario.FUNCIONARIO
                && chamado.getFuncionarioResponsavel() != null
                && Objects.equals(chamado.getFuncionarioResponsavel().getId(), usuario.getId());

        if (!atendente && !autor && !funcionarioResponsavel) {
            throw new AccessDeniedException("Voce nao pode visualizar as fotos deste chamado.");
        }

        try {
            FotoChamado foto = fotoChamadoService.buscarPorId(fotoId);
            if (foto.getChamado() == null || !Objects.equals(foto.getChamado().getId(), id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto nao pertence a este chamado.");
            }

            // O cidadao so pode ver a foto da ultima tentativa APROVADA.
            // Fotos de tentativas reprovadas tambem ficam bloqueadas por URL,
            // mesmo quando o chamado ja foi resolvido.
            // Atendente e funcionario responsavel preservam o acesso interno.
            if (foto.getTipo() == TipoFoto.SOLUCAO && autor && !atendente
                    && !funcionarioResponsavel
                    && !fotoChamadoService.ehFotoDeSolucaoAprovada(chamado, foto)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografia nao encontrada.");
            }

            // O formato e definido pelo nome gerado no servidor, nunca por um parametro da URL.
            String nome = foto.getCaminhoArquivo();
            MediaType tipo = nome.endsWith(".png") ? MediaType.IMAGE_PNG
                    : nome.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                    : MediaType.IMAGE_JPEG;

            Resource arquivo = fotoChamadoService.carregarArquivo(foto);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(tipo)
                    .body(arquivo);
        } catch (EntityNotFoundException erro) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografia nao encontrada.");
        }
    }

    // ======================= AREA DO ATENDENTE =======================
    // O Spring Security restringe /atendente/** ao perfil ATENDENTE.

    @GetMapping("/atendente/chamados")
    public String listarTodos(@RequestParam(required = false) String busca,
                              @RequestParam(required = false) StatusChamado status,
                              @RequestParam(required = false) Prioridade prioridade,
                              @RequestParam(required = false) Long categoriaId,
                              Model model) {
        String termo = busca == null ? "" : busca.trim();
        if (termo.length() > 150) {
            termo = termo.substring(0, 150);
        }

        model.addAttribute("chamados", chamadoService.pesquisarChamados(termo, status, prioridade, categoriaId));
        model.addAttribute("busca", termo);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("prioridadeSelecionada", prioridade);
        model.addAttribute("categoriaSelecionada", categoriaId);
        model.addAttribute("statusDisponiveis", StatusChamado.values());
        model.addAttribute("prioridades", Prioridade.values());
        // Inclui categorias inativas para permitir pesquisas em chamados antigos.
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("filtrosAplicados", !termo.isBlank() || status != null
                || prioridade != null || categoriaId != null);
        return "chamado/administracao/lista";
    }

    @GetMapping("/atendente/chamados/{id}")
    public String detalhesDoAtendente(@PathVariable Long id, Model model) {
        Chamado chamado = chamadoService.buscarPorId(id);

        model.addAttribute("chamado", chamado);
        model.addAttribute("historico", historicoChamadoService.listarPorChamado(id));
        model.addAttribute("atualizacoes", historicoChamadoService.listarAtualizacoes(id, false));
        model.addAttribute("fotos", fotoChamadoService.listarPorChamado(id));
        model.addAttribute("categorias", categoriaService.listarAtivas());
        model.addAttribute("prioridades", Prioridade.values());

        // Apenas funcionarios ativos do setor responsavel pela categoria.
        Long setorId = chamado.getCategoria().getSetor().getId();
        model.addAttribute("funcionarios", usuarioService.listarFuncionariosAtivosPorSetor(setorId));
        return "chamado/administracao/detalhes";
    }

    @PostMapping("/atendente/chamados/{id}/aprovar-triagem")
    public String aprovarTriagem(@PathVariable Long id,
                                @RequestParam Long categoriaId,
                                @RequestParam Prioridade prioridadeOficial,
                                Authentication autenticacao,
                                RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.aprovarTriagem(id, categoriaId, prioridadeOficial, autenticacao.getName()),
                "Triagem aprovada.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/prioridade")
    public String alterarPrioridade(@PathVariable Long id,
                                    @RequestParam Prioridade prioridadeOficial,
                                    Authentication autenticacao,
                                    RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.alterarPrioridade(id, prioridadeOficial, autenticacao.getName()),
                "Prioridade atualizada.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/atribuir")
    public String atribuir(@PathVariable Long id,
                           @RequestParam Long funcionarioId,
                           Authentication autenticacao,
                           RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.atribuir(id, funcionarioId, autenticacao.getName()),
                "Funcionario atribuido.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/recusar")
    public String recusar(@PathVariable Long id,
                          @RequestParam String justificativa,
                          Authentication autenticacao,
                          RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.recusar(id, justificativa, autenticacao.getName()),
                "Chamado recusado.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/duplicado")
    public String marcarDuplicado(@PathVariable Long id,
                                  @RequestParam String protocoloOriginal,
                                  Authentication autenticacao,
                                  RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.marcarDuplicado(id, protocoloOriginal, autenticacao.getName()),
                "Chamado vinculado ao original.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/solicitar-informacoes")
    public String solicitarInformacoes(@PathVariable Long id,
                                       @RequestParam String solicitacao,
                                       Authentication autenticacao,
                                       RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.solicitarInformacoes(id, solicitacao, autenticacao.getName()),
                "Informacoes solicitadas ao cidadao.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/retomar")
    public String retomarPendente(@PathVariable Long id,
                                 @RequestParam(required = false) Long funcionarioId,
                                 Authentication autenticacao,
                                 RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.retomarPendente(id, funcionarioId, autenticacao.getName()),
                "Chamado retomado.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/aprovar-conclusao")
    public String aprovarConclusao(@PathVariable Long id,
                                   Authentication autenticacao,
                                   RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.aprovarConclusao(id, autenticacao.getName()),
                "Conclusao aprovada.", "/atendente/chamados/" + id, redirectAttributes);
    }

    @PostMapping("/atendente/chamados/{id}/reprovar-conclusao")
    public String reprovarConclusao(@PathVariable Long id,
                                    @RequestParam String justificativa,
                                    @RequestParam(required = false) Long novoFuncionarioId,
                                    Authentication autenticacao,
                                    RedirectAttributes redirectAttributes) {
        return executarAcao(
                () -> chamadoAtendenteService.reprovarConclusao(id, justificativa, novoFuncionarioId,
                        autenticacao.getName()),
                "Execucao devolvida para nova tentativa.", "/atendente/chamados/" + id, redirectAttributes);
    }

    // ======================= METODOS AUXILIARES =======================

    private String prepararCadastro(Model model) {
        // Um setor inativo nao pode ser utilizado em novos chamados.
        model.addAttribute("categorias", categoriaService.listarAtivas().stream()
                .filter(c -> c.getSetor() != null && Boolean.TRUE.equals(c.getSetor().getAtivo()))
                .toList());
        model.addAttribute("prioridades", Prioridade.values());
        return "chamado/novo";
    }

    // Copia apenas os campos permitidos; autor, status e prioridade oficial vem do Service.
    private Chamado converterParaChamado(CriarChamadoDTO dto) {
        Chamado dados = new Chamado();
        dados.setTitulo(dto.getTitulo());
        dados.setDescricao(dto.getDescricao());
        dados.setEndereco(dto.getEndereco());
        dados.setLatitude(dto.getLatitude());
        dados.setLongitude(dto.getLongitude());
        dados.setUrgenciaPercebida(dto.getUrgenciaPercebida());

        Categoria categoria = new Categoria();
        categoria.setId(dto.getCategoriaId());
        dados.setCategoria(categoria);
        return dados;
    }

    // Evita repetir try/catch e mensagens de retorno em cada acao do atendente/cidadao.
    private String executarAcao(Runnable acao, String mensagemSucesso,
                                String urlRetorno, RedirectAttributes redirectAttributes) {
        try {
            acao.run();
            redirectAttributes.addFlashAttribute("mensagemSucesso", mensagemSucesso);
        } catch (IllegalArgumentException | EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:" + urlRetorno;
    }
}
