package br.com.moveup.accounts.application.usecase;

import br.com.moveup.accounts.application.port.in.GetMe;
import br.com.moveup.accounts.application.port.in.MeView;
import br.com.moveup.accounts.application.port.out.AccountReader;
import br.com.moveup.accounts.domain.exception.AccountNotRegistered;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class GetMeUseCase implements GetMe {

  private final AccountReader accounts;

  public GetMeUseCase(AccountReader accounts) {
    this.accounts = accounts;
  }

  @Override
  @Transactional(readOnly = true)
  public MeView handle(UUID userId) {
    return accounts.findMe(userId).orElseThrow(AccountNotRegistered::new);
  }
}
