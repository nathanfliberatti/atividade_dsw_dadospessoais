package br.senac.tads.dsw.dadospessoais;

import java.util.List;
import java.util.Optional;

public interface PessoaService {

    List<PessoaDto> obterPessoas();

    Optional<PessoaDto> obterPessoa(String username);

    PessoaDto incluirNovo(PessoaDto pessoaDto);

    PessoaDto alterar(String username, PessoaAlteracaoDto pessoaAlteracao);

    void excluir(String username);

    List<PessoaDto> buscarPessoas(String termo);
}
