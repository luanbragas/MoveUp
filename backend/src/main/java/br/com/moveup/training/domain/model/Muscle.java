package br.com.moveup.training.domain.model;

import java.util.Optional;

/** Grupos musculares, com os mesmos códigos do mapa muscular do app. */
public enum Muscle {
  CHEST("chest"),
  DELTS("delts"),
  TRAPS("traps"),
  ABS("abs"),
  LATS("lats"),
  BICEPS("biceps"),
  TRICEPS("triceps"),
  FOREARMS("forearms"),
  QUADS("quads"),
  ADDUCTORS("adductors"),
  ABDUCTORS("abductors"),
  CALVES("calves"),
  LOWERBACK("lowerback"),
  GLUTES("glutes"),
  HAMSTRINGS("hamstrings");

  private final String code;

  Muscle(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public static Optional<Muscle> fromCode(String code) {
    for (var muscle : values()) {
      if (muscle.code.equals(code)) {
        return Optional.of(muscle);
      }
    }
    return Optional.empty();
  }
}
