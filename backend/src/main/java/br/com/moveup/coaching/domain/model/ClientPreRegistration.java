package br.com.moveup.coaching.domain.model;

import br.com.moveup.coaching.domain.exception.InvalidClientData;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Pré-cadastro do aluno feito pelo profissional (SCREEN-FLOWS 2.2): nome obrigatório; e-mail,
 * WhatsApp e objetivo opcionais. O aluno completa o cadastro dele ao aceitar o convite.
 */
public final class ClientPreRegistration {

  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s]+\\.[^@\\s]{2,}$");
  private static final int MAX_GOAL = 500;

  private final String name;
  private final String email; // nulo = não informado
  private final String phone; // só dígitos com DDI; nulo = não informado
  private final String goal; // nulo = não informado

  private ClientPreRegistration(String name, String email, String phone, String goal) {
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.goal = goal;
  }

  public static ClientPreRegistration of(String name, String email, String phone, String goal) {
    var cleanName = name == null ? "" : name.strip().replaceAll("\\s+", " ");
    if (cleanName.length() < 2 || cleanName.length() > 200) {
      throw new InvalidClientData("name-invalid", "Informe o nome do aluno.");
    }
    var cleanEmail = blankToNull(email);
    if (cleanEmail != null) {
      cleanEmail = cleanEmail.toLowerCase(Locale.ROOT);
      if (cleanEmail.length() > 254 || !EMAIL.matcher(cleanEmail).matches()) {
        throw new InvalidClientData("email-invalid", "E-mail do aluno inválido.");
      }
    }
    var cleanPhone = blankToNull(phone);
    if (cleanPhone != null) {
      cleanPhone = cleanPhone.replaceAll("\\D", "");
      if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
        throw new InvalidClientData("phone-invalid", "WhatsApp do aluno inválido.");
      }
    }
    var cleanGoal = blankToNull(goal);
    if (cleanGoal != null && cleanGoal.length() > MAX_GOAL) {
      throw new InvalidClientData("goal-invalid", "Objetivo com mais de 500 caracteres.");
    }
    return new ClientPreRegistration(cleanName, cleanEmail, cleanPhone, cleanGoal);
  }

  public String name() {
    return name;
  }

  public Optional<String> email() {
    return Optional.ofNullable(email);
  }

  public Optional<String> phone() {
    return Optional.ofNullable(phone);
  }

  public Optional<String> goal() {
    return Optional.ofNullable(goal);
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    var stripped = value.strip();
    return stripped.isEmpty() ? null : stripped;
  }
}
