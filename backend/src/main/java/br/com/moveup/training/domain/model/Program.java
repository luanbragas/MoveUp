package br.com.moveup.training.domain.model;

import br.com.moveup.shared.domain.VersionMismatch;
import br.com.moveup.training.domain.exception.InvalidTrainingData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Programa do aluno (SCREEN-FLOWS 2.3): período, objetivo e agenda dos treinos. Dias fixos (Treino
 * A na segunda e quinta) ou sequência (A → B → C, meta de N treinos por semana). A ordem dos
 * treinos vale nos dois modos (é a ordem em que aparecem).
 */
public final class Program {

  public static final int MAX_NAME = 80;

  public enum ScheduleMode {
    FIXED_DAYS,
    SEQUENCE;

    public String code() {
      return name().toLowerCase(Locale.ROOT);
    }

    public static ScheduleMode fromCode(String code) {
      for (var value : values()) {
        if (value.code().equals(code)) {
          return value;
        }
      }
      throw new InvalidTrainingData("schedule-mode-invalid", "Tipo de agenda inválido.");
    }
  }

  /** Um treino na agenda: posição pela ordem da lista; dias da semana (0 = domingo). */
  public record Slot(UUID workoutId, Set<Integer> weekdays) {

    public Slot {
      weekdays = Set.copyOf(weekdays == null ? Set.of() : weekdays);
      if (weekdays.stream().anyMatch(d -> d < 0 || d > 6)) {
        throw new InvalidTrainingData("weekday-invalid", "Dia da semana inválido.");
      }
    }
  }

  private final UUID id;
  private final UUID linkId;
  private final UUID clientId;
  private String name;
  private String goal;
  private LocalDate startsOn;
  private LocalDate endsOn;
  private ScheduleMode mode;
  private Integer weeklyTarget;
  private List<Slot> slots;
  private int revision;
  private boolean archived;

  public Program(
      UUID id,
      UUID linkId,
      UUID clientId,
      String name,
      String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      ScheduleMode mode,
      Integer weeklyTarget,
      List<Slot> slots,
      int revision,
      boolean archived) {
    this.id = id;
    this.linkId = linkId;
    this.clientId = clientId;
    this.name = name;
    this.goal = goal;
    this.startsOn = startsOn;
    this.endsOn = endsOn;
    this.mode = mode;
    this.weeklyTarget = weeklyTarget;
    this.slots = List.copyOf(slots);
    this.revision = revision;
    this.archived = archived;
  }

  public static Program create(
      UUID id,
      UUID linkId,
      UUID clientId,
      String name,
      String goal,
      LocalDate startsOn,
      LocalDate endsOn,
      ScheduleMode mode,
      Integer weeklyTarget) {
    var program =
        new Program(id, linkId, clientId, null, null, null, null, mode, null, List.of(), 1, false);
    program.apply(name, goal, startsOn, endsOn, mode, weeklyTarget);
    return program;
  }

  /**
   * Salva nome, período e agenda.
   *
   * @param slots todos os treinos do programa, na ordem desejada
   */
  public void update(
      int expectedRevision,
      String newName,
      String newGoal,
      LocalDate newStartsOn,
      LocalDate newEndsOn,
      ScheduleMode newMode,
      Integer newWeeklyTarget,
      List<Slot> newSlots) {
    requireRevision(expectedRevision);
    var current = new HashSet<UUID>();
    slots.forEach(s -> current.add(s.workoutId()));
    var given = new HashSet<UUID>();
    newSlots.forEach(s -> given.add(s.workoutId()));
    if (given.size() != newSlots.size() || !given.equals(current)) {
      throw new InvalidTrainingData(
          "schedule-workouts-invalid", "A agenda precisa ter cada treino do programa uma vez.");
    }
    apply(newName, newGoal, newStartsOn, newEndsOn, newMode, newWeeklyTarget);
    this.slots =
        newMode == ScheduleMode.SEQUENCE
            ? newSlots.stream().map(s -> new Slot(s.workoutId(), Set.of())).toList()
            : List.copyOf(newSlots);
    this.revision++;
  }

  /** Treino novo entra no fim da agenda, sem dias marcados. */
  public void addWorkout(UUID workoutId) {
    var next = new ArrayList<>(slots);
    next.add(new Slot(workoutId, Set.of()));
    this.slots = List.copyOf(next);
    this.revision++;
  }

  public void archive(int expectedRevision) {
    requireRevision(expectedRevision);
    this.archived = true;
    this.revision++;
  }

  private void requireRevision(int expectedRevision) {
    if (expectedRevision != revision) {
      throw VersionMismatch.stale();
    }
  }

  private void apply(
      String newName,
      String newGoal,
      LocalDate newStartsOn,
      LocalDate newEndsOn,
      ScheduleMode newMode,
      Integer newWeeklyTarget) {
    var cleanName = newName == null ? "" : newName.strip().replaceAll("\\s+", " ");
    if (cleanName.isEmpty() || cleanName.length() > MAX_NAME) {
      throw new InvalidTrainingData(
          "program-name-invalid", "O nome do programa precisa ter de 1 a 80 caracteres.");
    }
    var cleanGoal = newGoal == null || newGoal.isBlank() ? null : newGoal.strip();
    if (cleanGoal != null && cleanGoal.length() > 200) {
      throw new InvalidTrainingData("goal-invalid", "O objetivo pode ter até 200 caracteres.");
    }
    if (newStartsOn != null && newEndsOn != null && newEndsOn.isBefore(newStartsOn)) {
      throw new InvalidTrainingData("period-invalid", "O fim não pode ser antes do início.");
    }
    if (newMode == null) {
      throw new InvalidTrainingData("schedule-mode-invalid", "Escolha o tipo de agenda.");
    }
    if (newMode == ScheduleMode.SEQUENCE
        && (newWeeklyTarget == null || newWeeklyTarget < 1 || newWeeklyTarget > 14)) {
      throw new InvalidTrainingData(
          "weekly-target-invalid", "Na sequência, informe a meta de 1 a 14 treinos por semana.");
    }
    this.name = cleanName;
    this.goal = cleanGoal;
    this.startsOn = newStartsOn;
    this.endsOn = newEndsOn;
    this.mode = newMode;
    this.weeklyTarget = newMode == ScheduleMode.SEQUENCE ? newWeeklyTarget : null;
  }

  /** Dias da semana por treino (só no modo dias fixos). */
  public Map<UUID, Set<Integer>> weekdaysByWorkout() {
    var map = new LinkedHashMap<UUID, Set<Integer>>();
    slots.forEach(s -> map.put(s.workoutId(), s.weekdays()));
    return map;
  }

  public UUID id() {
    return id;
  }

  public UUID linkId() {
    return linkId;
  }

  public UUID clientId() {
    return clientId;
  }

  public String name() {
    return name;
  }

  public String goal() {
    return goal;
  }

  public LocalDate startsOn() {
    return startsOn;
  }

  public LocalDate endsOn() {
    return endsOn;
  }

  public ScheduleMode mode() {
    return mode;
  }

  public Integer weeklyTarget() {
    return weeklyTarget;
  }

  public List<Slot> slots() {
    return slots;
  }

  public int revision() {
    return revision;
  }

  public boolean archived() {
    return archived;
  }
}
