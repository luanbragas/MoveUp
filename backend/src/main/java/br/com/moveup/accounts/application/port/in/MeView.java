package br.com.moveup.accounts.application.port.in;

import java.util.UUID;

/** Dados da própria conta para o app: unidades preferidas, fuso e se tem perfil de profissional. */
public record MeView(
    UUID id,
    String name,
    String email,
    String locale,
    String timezone,
    String weightUnit,
    String lengthUnit,
    boolean professional) {}
