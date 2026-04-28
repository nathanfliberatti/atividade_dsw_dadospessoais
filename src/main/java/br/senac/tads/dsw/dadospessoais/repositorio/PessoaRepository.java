package br.senac.tads.dsw.dadospessoais.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.senac.tads.dsw.dadospessoais.entidade.PessoaEntity;

@Repository
public interface PessoaRepository extends JpaRepository<PessoaEntity, Long> {

    // Spring Data JPA gera a query automaticamente a partir do nome do método:
    // SELECT * FROM tb_pessoas WHERE username = :username
    Optional<PessoaEntity> findByUsername(String username);

    // Gera: SELECT COUNT(*) > 0 FROM tb_pessoas WHERE username = :username
    boolean existsByUsername(String username);

}
