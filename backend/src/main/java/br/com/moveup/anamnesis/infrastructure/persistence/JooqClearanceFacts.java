package br.com.moveup.anamnesis.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ANAMNESIS;

import br.com.moveup.anamnesis.api.ClearanceFacts;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Situação da liberação médica para o worker (só colunas abertas; nada é decifrado). */
@Repository
public class JooqClearanceFacts implements ClearanceFacts {

  private final DSLContext dsl;

  public JooqClearanceFacts(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public Optional<ClearanceState> latestFor(UUID anamnesisId) {
    return dsl.select(ANAMNESIS.CLIENT_ID)
        .from(ANAMNESIS)
        .where(ANAMNESIS.ID.eq(anamnesisId))
        .fetchOptional(ANAMNESIS.CLIENT_ID)
        .flatMap(
            clientId ->
                dsl.select(ANAMNESIS.COACHING_LINK_ID, ANAMNESIS.MEDICAL_CLEARANCE)
                    .from(ANAMNESIS)
                    .where(ANAMNESIS.CLIENT_ID.eq(clientId))
                    .orderBy(ANAMNESIS.VERSION_NUMBER.desc())
                    .limit(1)
                    .fetchOptional(
                        r ->
                            new ClearanceState(
                                clientId, r.value1(), "pending".equals(r.value2()))));
  }
}
