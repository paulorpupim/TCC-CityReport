package com.tccinfonet.cityreport.modules.chamado.foto_chamado.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tccinfonet.cityreport.modules.chamado.entity.Chamado;
import com.tccinfonet.cityreport.modules.chamado.enums.StatusChamado;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.entity.FotoChamado;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.enums.TipoFoto;
import com.tccinfonet.cityreport.modules.chamado.foto_chamado.repository.FotoChamadoRepository;
import com.tccinfonet.cityreport.modules.usuario.entity.Usuario;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FotoChamadoService {

    private static final long TAMANHO_MAXIMO = 5 * 1024 * 1024; // 5 MB

    private final FotoChamadoRepository fotoChamadoRepository;

    // Fora de src/main/resources/static: imagens nao devem ser publicas por URL direta.
    @Value("${cityreport.upload.dir:uploads/chamados}")
    private String diretorioUpload;

    // Lista metadados das fotos. Quem chama deve verificar antes o acesso ao chamado.
    public List<FotoChamado> listarPorChamado(Long chamadoId) {
        return fotoChamadoRepository.findByChamadoIdOrderByDataEnvioAscIdAsc(chamadoId);
    }

    // Lista somente fotos do problema ou somente fotos da solucao.
    public List<FotoChamado> listarPorChamadoETipo(Long chamadoId, TipoFoto tipo) {
        return fotoChamadoRepository.findByChamadoIdAndTipoOrderByDataEnvioAscIdAsc(chamadoId, tipo);
    }

    // Salva uma imagem em disco e registra os dados dela no PostgreSQL.
    // Deve ser chamado pelo fluxo de ChamadoService, com entidades vindas do banco.
    @Transactional
    public FotoChamado salvarFoto(Chamado chamado, Usuario usuario, TipoFoto tipo,
                                 MultipartFile arquivo) {
        if (chamado == null || chamado.getId() == null) {
            throw new IllegalArgumentException("O chamado precisa existir antes de receber fotos.");
        }
        if (usuario == null || usuario.getId() == null || !Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Usuario invalido ou inativo.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Informe o tipo da fotografia.");
        }
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma fotografia.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A fotografia deve ter no maximo 5 MB.");
        }

        validarResponsavel(chamado, usuario, tipo);

        Path destino = null;
        try {
            String extensao = identificarExtensao(arquivo);
            String nomeArquivo = UUID.randomUUID() + extensao;
            Path diretorio = Paths.get(diretorioUpload).toAbsolutePath().normalize();
            destino = diretorio.resolve(nomeArquivo);

            Files.createDirectories(diretorio);

            // O nome original enviado pelo usuario nunca e usado como caminho.
            try (InputStream entrada = arquivo.getInputStream()) {
                Files.copy(entrada, destino);
            }

            FotoChamado foto = new FotoChamado();
            foto.setCaminhoArquivo(nomeArquivo);
            foto.setTipo(tipo);
            foto.setChamado(chamado);
            foto.setEnviadaPor(usuario);

            return fotoChamadoRepository.saveAndFlush(foto);
        } catch (IOException erro) {
            removerArquivoSeNecessario(destino, erro);
            throw new IllegalStateException("Nao foi possivel armazenar a fotografia.", erro);
        } catch (RuntimeException erro) {
            removerArquivoSeNecessario(destino, erro);
            throw erro;
        }
    }

    // Limpa arquivos parciais se o upload ou a gravacao no banco falhar neste metodo.
    private void removerArquivoSeNecessario(Path caminho, Exception erroOriginal) {
        if (caminho != null) {
            try {
                Files.deleteIfExists(caminho);
            } catch (IOException erroExclusao) {
                erroOriginal.addSuppressed(erroExclusao);
            }
        }
    }

    // Fotos do problema: cidadao autor. Fotos da solucao: funcionario responsavel.
    private void validarResponsavel(Chamado chamado, Usuario usuario, TipoFoto tipo) {
        if (tipo == TipoFoto.PROBLEMA) {
            if (chamado.getCidadao() == null
                    || !Objects.equals(chamado.getCidadao().getId(), usuario.getId())) {
                throw new IllegalArgumentException("Somente o autor pode enviar fotos do problema.");
            }
            if (chamado.getStatus() != StatusChamado.ENVIADO
                    && chamado.getStatus() != StatusChamado.AGUARDANDO_INFORMACOES) {
                throw new IllegalArgumentException("Nao e possivel adicionar fotos do problema neste status.");
            }
        } else if (tipo == TipoFoto.SOLUCAO) {
            if (chamado.getFuncionarioResponsavel() == null
                    || !Objects.equals(chamado.getFuncionarioResponsavel().getId(), usuario.getId())) {
                throw new IllegalArgumentException("Somente o funcionario atribuido pode enviar fotos da solucao.");
            }
            if (chamado.getStatus() != StatusChamado.EM_EXECUCAO) {
                throw new IllegalArgumentException("A foto da solucao exige um chamado em execucao.");
            }
        }
    }

    // Confere o tipo informado e a assinatura do arquivo; nao confia na extensao original.
    private String identificarExtensao(MultipartFile arquivo) throws IOException {
        byte[] inicio;
        try (InputStream entrada = arquivo.getInputStream()) {
            inicio = entrada.readNBytes(12);
        }

        String tipo = arquivo.getContentType();

        boolean jpeg = inicio.length >= 3
                && (inicio[0] & 0xFF) == 0xFF
                && (inicio[1] & 0xFF) == 0xD8
                && (inicio[2] & 0xFF) == 0xFF;
        if (jpeg && "image/jpeg".equalsIgnoreCase(tipo)) {
            return ".jpg";
        }

        boolean png = inicio.length >= 8
                && (inicio[0] & 0xFF) == 0x89
                && inicio[1] == 'P' && inicio[2] == 'N' && inicio[3] == 'G'
                && (inicio[4] & 0xFF) == 0x0D && (inicio[5] & 0xFF) == 0x0A
                && (inicio[6] & 0xFF) == 0x1A && (inicio[7] & 0xFF) == 0x0A;
        if (png && "image/png".equalsIgnoreCase(tipo)) {
            return ".png";
        }

        boolean webp = inicio.length >= 12
                && "RIFF".equals(new String(inicio, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(inicio, 8, 4, StandardCharsets.US_ASCII));
        if (webp && "image/webp".equalsIgnoreCase(tipo)) {
            return ".webp";
        }

        throw new IllegalArgumentException("Envie uma imagem JPG, PNG ou WebP valida.");
    }
}
