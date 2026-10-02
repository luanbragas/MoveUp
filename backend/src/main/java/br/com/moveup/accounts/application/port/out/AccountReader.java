package br.com.moveup.accounts.application.port.out;

import br.com.moveup.accounts.application.port.in.MeView;
import java.util.Optional;
import java.util.UUID;

/** Leitura de contas ativas (não anonimizadas). */
public interface AccountReader {

  Optional<MeView> findMe(UUID userId);
}
