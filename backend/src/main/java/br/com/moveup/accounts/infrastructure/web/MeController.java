package br.com.moveup.accounts.infrastructure.web;

import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import br.com.moveup.shared.infrastructure.security.CurrentAppUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
class MeController {

  private final GetMe getMe;
  private final CurrentAppUser currentAppUser;

  MeController(GetMe getMe, CurrentAppUser currentAppUser) {
    this.getMe = getMe;
    this.currentAppUser = currentAppUser;
  }

  @GetMapping
  MeResponse me() {
    var userId = currentAppUser.id().orElseThrow(AccountNotRegistered::new);
    return MeResponse.from(getMe.handle(userId));
  }
}
