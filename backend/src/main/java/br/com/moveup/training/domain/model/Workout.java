package br.com.moveup.training.domain.model;

import br.com.moveup.shared.domain.ResourceNotFound;
import br.com.moveup.shared.domain.VersionMismatch;
import br.com.moveup.training.domain.exception.InvalidTrainingData;
import java.util.UUID;

/**
 * Agregado do treino (modelo ou de um programa). O conteúdo vive em versões: se a versão atual já
 * foi usada numa sessão, salvar cria uma versão nova, e a sessão antiga continua comparando com o
 * planejado original; senão, a versão atual é substituída. Toda gravação sobe a revisão (ETag).
 */
public final class Workout {

  public static final int MAX_NAME = 60;

  private final UUID id;
  private final UUID organizationId;
  private final UUID programId; // nulo em modelo
  private final UUID sourceTemplateId;
  private String name;
  private int revision;
  private UUID versionId;
  private int versionNumber;
  private WorkoutContent content;
  private boolean archived;

  public Workout(
      UUID id,
      UUID organizationId,
      UUID programId,
      UUID sourceTemplateId,
      String name,
      int revision,
      UUID versionId,
      int versionNumber,
      WorkoutContent content,
      boolean archived) {
    this.id = id;
    this.organizationId = organizationId;
    this.programId = programId;
    this.sourceTemplateId = sourceTemplateId;
    this.name = name;
    this.revision = revision;
    this.versionId = versionId;
    this.versionNumber = versionNumber;
    this.content = content;
    this.archived = archived;
  }

  /** Modelo novo da organização (biblioteca de treinos do personal). */
  public static Workout newTemplate(
      UUID id, UUID versionId, UUID organizationId, String name, WorkoutContent content) {
    return new Workout(
        id, organizationId, null, null, cleanName(name), 1, versionId, 1, content, false);
  }

  /** Treino de um programa; {@code template} nulo = do zero. */
  public static Workout forProgram(
      UUID id, UUID versionId, UUID organizationId, UUID programId, Workout template, String name) {
    if (template != null && !template.isTemplate()) {
      throw new InvalidTrainingData("not-a-template", "Só dá para copiar de um modelo.");
    }
    var content = template == null ? WorkoutContent.empty() : template.content;
    var finalName =
        name == null || name.isBlank() ? (template == null ? null : template.name) : name;
    return new Workout(
        id,
        organizationId,
        programId,
        template == null ? null : template.id,
        cleanName(finalName),
        1,
        versionId,
        1,
        content,
        false);
  }

  /** Quem edita é da organização dona do treino; senão, 404 (não revela que existe). */
  public void requireOwnedBy(UUID organization) {
    if (!organizationId.equals(organization) || archived) {
      throw new ResourceNotFound();
    }
  }

  /**
   * Salva o editor.
   *
   * @param expectedRevision revisão que o app editou (If-Match)
   * @param currentVersionUsed a versão atual já tem sessão registrada
   * @param newVersionId id da versão nova, se precisar criar
   * @return {@code true} se criou versão nova; {@code false} se substituiu a atual
   */
  public boolean edit(
      String newName,
      WorkoutContent newContent,
      int expectedRevision,
      boolean currentVersionUsed,
      UUID newVersionId) {
    if (expectedRevision != revision) {
      throw VersionMismatch.stale();
    }
    this.name = cleanName(newName);
    this.content = newContent;
    this.revision++;
    if (currentVersionUsed) {
      this.versionId = newVersionId;
      this.versionNumber++;
      return true;
    }
    return false;
  }

  public void archive(int expectedRevision) {
    if (expectedRevision != revision) {
      throw VersionMismatch.stale();
    }
    this.archived = true;
    this.revision++;
  }

  private static String cleanName(String value) {
    var clean = value == null ? "" : value.strip().replaceAll("\\s+", " ");
    if (clean.isEmpty() || clean.length() > MAX_NAME) {
      throw new InvalidTrainingData(
          "workout-name-invalid", "O nome do treino precisa ter de 1 a 60 caracteres.");
    }
    return clean;
  }

  public boolean isTemplate() {
    return programId == null;
  }

  public UUID id() {
    return id;
  }

  public UUID organizationId() {
    return organizationId;
  }

  public UUID programId() {
    return programId;
  }

  public UUID sourceTemplateId() {
    return sourceTemplateId;
  }

  public String name() {
    return name;
  }

  public int revision() {
    return revision;
  }

  public UUID versionId() {
    return versionId;
  }

  public int versionNumber() {
    return versionNumber;
  }

  public WorkoutContent content() {
    return content;
  }

  public boolean archived() {
    return archived;
  }
}
