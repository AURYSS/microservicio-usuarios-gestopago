package com.gestopago.microusuarios.service;

import com.gestopago.microusuarios.dto.*;

import java.util.List;

public interface UsuarioService {
    UsuarioResponse crearUsuario(UsuarioCreateRequest request);
    UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request);
    void bajaLogicaUsuario(UsuarioDeleteRequest request);
    List<UsuarioResponse> buscarUsuarios(UsuarioFiltroRequest filtro);
    UsuarioResponse obtenerMiPerfil(String correoAutenticado);
}
