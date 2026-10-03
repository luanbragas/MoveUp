package br.com.moveup.anamnesis.domain.model;

import br.com.moveup.anamnesis.domain.exception.InvalidAnamnesisData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Pergunta do modelo de anamnese: o tipo decide o formato aceito na resposta. */
public record Question(
    String code,
    String section,
    String label,
    Type type,
    boolean required,
    List<Option> options,
    Integer min,
    Integer max) {

  static final int MAX_TEXT = 2000;

  public enum Type {
    SINGLE,
    MULTI,
    YES_NO,
    INTEGER,
    TEXT;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static Type fromCode(String code) {
      return valueOf(code.toUpperCase(Locale.ROOT));
    }
  }

  public record Option(String value, String label) {}

  public Question {
    options = options == null ? List.of() : List.copyOf(options);
  }

  public boolean isParq() {
    return "parq".equals(section);
  }

  /**
   * Valor normalizado (String, Boolean, Integer ou lista de String), ou nulo se em branco.
   *
   * @throws InvalidAnamnesisData formato errado (a mensagem cita só o código da pergunta)
   */
  Object normalize(Object raw) {
    if (raw == null || (raw instanceof String s && s.isBlank())) {
      return null;
    }
    return switch (type) {
      case SINGLE -> {
        if (raw instanceof String s && hasOption(s)) {
          yield s;
        }
        throw invalid();
      }
      case MULTI -> {
        if (!(raw instanceof List<?> list)) {
          throw invalid();
        }
        var picked = new LinkedHashSet<String>();
        for (var item : list) {
          if (!(item instanceof String s) || !hasOption(s)) {
            throw invalid();
          }
          picked.add(s);
        }
        yield picked.isEmpty() ? null : List.copyOf(new ArrayList<>(picked));
      }
      case YES_NO -> {
        if (raw instanceof Boolean b) {
          yield b;
        }
        throw invalid();
      }
      case INTEGER -> {
        if (raw instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
          var value = n.intValue();
          if ((min == null || value >= min) && (max == null || value <= max)) {
            yield value;
          }
        }
        throw invalid();
      }
      case TEXT -> {
        if (raw instanceof String s && s.strip().length() <= MAX_TEXT) {
          yield s.strip();
        }
        throw invalid();
      }
    };
  }

  private boolean hasOption(String value) {
    return options.stream().anyMatch(o -> o.value().equals(value));
  }

  private InvalidAnamnesisData invalid() {
    return new InvalidAnamnesisData("answer-invalid", "Resposta inválida em " + code + ".");
  }
}
