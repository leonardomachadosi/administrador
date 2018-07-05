package br.ufma.lsdi.administrador.service;

import java.util.List;

import br.ufma.lsdi.administrador.domain.model.Usuario;
import br.ufma.lsdi.administrador.domain.model.UsuarioLocalizacao;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UsuarioLocalizacaoService {


    @POST("listarTrajetoPorUsuario")
    Call<List<UsuarioLocalizacao>> getTrajeto(@Body Usuario usuario);

    @POST("salvarTrajeto")
    Call<UsuarioLocalizacao> salvarTrajeto(@Body UsuarioLocalizacao UsuarioLocalizacao);

}
