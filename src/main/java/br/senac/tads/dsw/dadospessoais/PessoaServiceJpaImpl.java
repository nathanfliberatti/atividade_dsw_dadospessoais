package br.senac.tads.dsw.dadospessoais;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.senac.tads.dsw.dadospessoais.entidade.ConhecimentoEntity;
import br.senac.tads.dsw.dadospessoais.entidade.PessoaEntity;
import br.senac.tads.dsw.dadospessoais.repositorio.ConhecimentoRepository;
import br.senac.tads.dsw.dadospessoais.repositorio.PessoaRepository;

// @Primary: quando há mais de uma implementação de PessoaService,
// o Spring injeta ESTA por padrão (sem precisar alterar o controller)
@Primary
@Service
public class PessoaServiceJpaImpl implements PessoaService {

    private final PessoaRepository pessoaRepository;
    private final ConhecimentoRepository conhecimentoRepository;

    public PessoaServiceJpaImpl(PessoaRepository pessoaRepository, ConhecimentoRepository conhecimentoRepository) {
        this.pessoaRepository = pessoaRepository;
        this.conhecimentoRepository = conhecimentoRepository;
    }

    @Override
    @Transactional(readOnly = true) // readOnly = true: otimiza leituras (sem necessidade de flush)
    public List<PessoaDto> obterPessoas() {

        List<PessoaDto> resultado = new ArrayList<>();

        for (PessoaEntity entity : pessoaRepository.findAll()) {
            PessoaDto dto = toDto(entity);
            resultado.add(dto);
        }
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PessoaDto> obterPessoa(String username) {

        Optional<PessoaEntity> optEntity = pessoaRepository.findByUsername(username);
        if (optEntity.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(toDto(optEntity.get())); // converte para DTO se encontrou
    }

    @Override
    @Transactional
    public PessoaDto incluirNovo(PessoaDto pessoaDto) {

        PessoaEntity entity = toEntity(pessoaDto); // converte DTO para entidade
        PessoaEntity salva = pessoaRepository.save(entity); // persiste no banco (INSERT)

        return toDto(salva); // retorna DTO com o ID gerado pelo banco
    }

    @Override
    @Transactional
    public PessoaDto alterar(String username, PessoaAlteracaoDto pessoaAlteracao) {

        Optional<PessoaEntity> optEntity = pessoaRepository.findByUsername(username);

        if (optEntity.isEmpty()) {
            throw new NaoEncontradoException("Pessoa " + username + " não encontrada");
        }
        PessoaEntity entity = optEntity.get();

        entity.setNome(pessoaAlteracao.getNome());
        entity.setEmail(pessoaAlteracao.getEmail());
        entity.setDataNascimento(pessoaAlteracao.getDataNascimento());

        // Atualiza os conhecimentos:
        // 1. Limpa a lista atual — remove as entradas da tabela de junção
        // tb_pessoas_conhecimentos
        entity.getConhecimentos().clear();

        // 2. Adiciona os novos conhecimentos (reutilizando entidades já existentes)
        if (pessoaAlteracao.getConhecimentos() != null) {
            for (String nomeConhecimento : pessoaAlteracao.getConhecimentos()) {
                Optional<ConhecimentoEntity> optConh = conhecimentoRepository.findByNomeIgnoreCase(nomeConhecimento);
                if (optConh.isPresent()) {
                    entity.getConhecimentos().add(optConh.get());
                }
            }
        }
        PessoaEntity salva = pessoaRepository.save(entity); // persiste as alterações (UPDATE)

        return toDto(salva);
    }

    @Override
    @Transactional
    public void excluir(String username) {

        Optional<PessoaEntity> optEntity = pessoaRepository.findByUsername(username);

        if (optEntity.isEmpty()) {
            throw new NaoEncontradoException("Pessoa " + username + " não encontrada");

        }
        PessoaEntity entity = optEntity.get();
        pessoaRepository.delete(entity); // DELETE no banco (cascade remove os conhecimentos também)
    }

    @Override
    @Transactional(readOnly = true)
    public List<PessoaDto> buscarPessoas(String termo) {
        // Versão imperativa — veja o Anexo A para a versão equivalente com streams
        List<PessoaDto> resultado = new ArrayList<>();
        for (PessoaEntity entity : pessoaRepository.buscarPorTermo(termo)) {
            resultado.add(toDto(entity));
        }
        return resultado;
    }

    
    // PessoaEntity → PessoaDto
    private PessoaDto toDto(PessoaEntity entity) {

        PessoaDto dto = new PessoaDto();

        dto.setId(entity.getId().intValue()); // Long → Integer (o DTO usa Integer)
        dto.setUsername(entity.getUsername());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setDataNascimento(entity.getDataNascimento());

        List<String> nomes = new ArrayList<>();
        for (ConhecimentoEntity c : entity.getConhecimentos()) {
            nomes.add(c.getNome());
        }
        dto.setConhecimentos(nomes);

        return dto;
    }

    // PessoaDto → PessoaEntity (para inserção — sem ID, o banco gera)
    private PessoaEntity toEntity(PessoaDto dto) {
        PessoaEntity entity = new PessoaEntity();
        entity.setUsername(dto.getUsername());
        entity.setNome(dto.getNome());
        entity.setEmail(dto.getEmail());
        entity.setDataNascimento(dto.getDataNascimento());
        entity.setSenha(dto.getSenha());
        if (dto.getConhecimentos() != null) {
            for (String nomeConhecimento : dto.getConhecimentos()) {
                Optional<ConhecimentoEntity> optConh = conhecimentoRepository.findByNomeIgnoreCase(nomeConhecimento);
                if (optConh.isPresent()) {
                    entity.getConhecimentos().add(optConh.get());
                }
            }
        }
        return entity;
    }
}
