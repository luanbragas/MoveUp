package br.com.moveup.accounts.domain.model;

import br.com.moveup.accounts.domain.exception.InvalidAccountData;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Conta nova no momento do cadastro. Nasce pelas fábricas de cada papel, que garantem as regras:
 * aluno informa a data de nascimento (decide se precisa do responsável); profissional, se informar,
 * precisa ser maior de idade.
 */
public final class Account {

  private final UUID id;
  private final LoginIdentity identity;
  private final PersonName name;
  private final Email email;
  private final AccountRole role;
  private final BirthDate birthDate; // nulo: profissional que não informou
  private final ProfessionalSetup professional; // nulo: aluno

  private Account(
      UUID id,
      LoginIdentity identity,
      PersonName name,
      Email email,
      AccountRole role,
      BirthDate birthDate,
      ProfessionalSetup professional) {
    this.id = id;
    this.identity = identity;
    this.name = name;
    this.email = email;
    this.role = role;
    this.birthDate = birthDate;
    this.professional = professional;
  }

  public static Account registerClient(
      UUID id, LoginIdentity identity, PersonName name, Email email, BirthDate birthDate) {
    return new Account(id, identity, name, email, AccountRole.CLIENT, birthDate, null);
  }

  public static Account registerProfessional(
      UUID id, LoginIdentity identity, PersonName name, Email email, ProfessionalSetup setup) {
    return new Account(id, identity, name, email, AccountRole.PROFESSIONAL, null, setup);
  }

  public static Account registerProfessional(
      UUID id,
      LoginIdentity identity,
      PersonName name,
      Email email,
      ProfessionalSetup setup,
      BirthDate birthDate,
      LocalDate today) {
    if (birthDate.isMinorOn(today)) {
      throw new InvalidAccountData(
          "professional-must-be-adult", "O perfil de profissional exige maioridade.");
    }
    return new Account(id, identity, name, email, AccountRole.PROFESSIONAL, birthDate, setup);
  }

  public UUID id() {
    return id;
  }

  public LoginIdentity identity() {
    return identity;
  }

  public PersonName name() {
    return name;
  }

  public Email email() {
    return email;
  }

  public AccountRole role() {
    return role;
  }

  public Optional<BirthDate> birthDate() {
    return Optional.ofNullable(birthDate);
  }

  public Optional<ProfessionalSetup> professional() {
    return Optional.ofNullable(professional);
  }
}
