package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.MeView;
import java.util.UUID;

record MeResponse(
    UUID id,
    String name,
    String email,
    String locale,
    String timezone,
    String weightUnit,
    String lengthUnit,
    boolean professional) {

  static MeResponse from(MeView view) {
    return new MeResponse(
        view.id(),
        view.name(),
        view.email(),
        view.locale(),
        view.timezone(),
        view.weightUnit(),
        view.lengthUnit(),
        view.professional());
  }
}
