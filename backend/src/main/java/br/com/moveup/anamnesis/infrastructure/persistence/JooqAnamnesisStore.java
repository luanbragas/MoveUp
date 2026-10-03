package br.com.moveup.anamnesis.infrastructure.persistence;

import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ANAMNESIS;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.ANAMNESIS_TEMPLATE;
import static br.com.moveup.shared.infrastructure.persistence.jooq.Tables.OUTBOX_EVENT;

import br.com.moveup.anamnesis.application.port.out.AnamnesisStore;
import br.com.moveup.anamnesis.domain.model.Anamnesis;
import br.com.moveup.anamnesis.domain.model.AnamnesisTemplate;
import br.com.moveup.anamnesis.domain.model.Answers;
import br.com.moveup.anamnesis.domain.model.Question;
import br.com.moveup.shared.application.crypto.FieldCipher;
import br.com.moveup.shared.domain.ResourceNotFound;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Anamneses. As respostas vão cifradas com a chave do aluno ({@code answers} guarda só o texto
 * cifrado, como string JSON); as colunas de ação (objetivo, nível, PAR-Q, liberação) ficam abertas.
 */
@Repository
public class JooqAnamnesisStore implements AnamnesisStore {

  static final String ANSWERS_FIELD = "anamnesis.answers";
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() {};

  private final DSLContext dsl;
  private final FieldCipher cipher;
  private final Map<UUID, AnamnesisTemplate> templates = new ConcurrentHashMap<>();

  public JooqAnamnesisStore(DSLContext dsl, FieldCipher cipher) {
    this.dsl = dsl;
    this.cipher = cipher;
  }

  @Override
  public AnamnesisTemplate currentTemplate() {
    var id =
        dsl.select(ANAMNESIS_TEMPLATE.ID)
            .from(ANAMNESIS_TEMPLATE)
            .where(ANAMNESIS_TEMPLATE.ORGANIZATION_ID.isNull())
            .and(ANAMNESIS_TEMPLATE.IS_ACTIVE.isTrue())
            .orderBy(ANAMNESIS_TEMPLATE.VERSION.desc())
            .limit(1)
            .fetchOptional(ANAMNESIS_TEMPLATE.ID)
            .orElseThrow(ResourceNotFound::new);
    return template(id);
  }

  private AnamnesisTemplate template(UUID id) {
    return templates.computeIfAbsent(
        id,
        key -> {
          var row =
              dsl.select(ANAMNESIS_TEMPLATE.VERSION, ANAMNESIS_TEMPLATE.QUESTIONS)
                  .from(ANAMNESIS_TEMPLATE)
                  .where(ANAMNESIS_TEMPLATE.ID.eq(key))
                  .fetchOne();
          if (row == null) {
            throw new ResourceNotFound();
          }
          return new AnamnesisTemplate(key, row.value1(), questions(row.value2().data()));
        });
  }

  static List<Question> questions(String json) {
    var out = new ArrayList<Question>();
    for (JsonNode q : JSON.readTree(json)) {
      var options = new ArrayList<Question.Option>();
      for (JsonNode o : q.path("options")) {
        options.add(new Question.Option(o.path("value").asString(), o.path("label").asString()));
      }
      out.add(
          new Question(
              q.path("code").asString(),
              q.path("section").asString(),
              q.path("label").asString(),
              Question.Type.fromCode(q.path("type").asString()),
              q.path("required").asBoolean(false),
              options,
              q.has("min") ? q.path("min").asInt() : null,
              q.has("max") ? q.path("max").asInt() : null));
    }
    return out;
  }

  @Override
  public Optional<Anamnesis> latest(UUID clientId) {
    return dsl.selectFrom(ANAMNESIS)
        .where(ANAMNESIS.CLIENT_ID.eq(clientId))
        .orderBy(ANAMNESIS.VERSION_NUMBER.desc())
        .limit(1)
        .fetchOptional(this::toDomain);
  }

  @Override
  public Optional<Anamnesis> version(UUID clientId, int versionNumber) {
    return dsl.selectFrom(ANAMNESIS)
        .where(ANAMNESIS.CLIENT_ID.eq(clientId))
        .and(ANAMNESIS.VERSION_NUMBER.eq(versionNumber))
        .fetchOptional(this::toDomain);
  }

  @Override
  public List<VersionSummary> versions(UUID clientId) {
    return dsl.select(
            ANAMNESIS.VERSION_NUMBER,
            ANAMNESIS.CREATED_AT,
            ANAMNESIS.REVIEWED_AT,
            ANAMNESIS.MEDICAL_CLEARANCE,
            ANAMNESIS.PARQ_POSITIVE)
        .from(ANAMNESIS)
        .where(ANAMNESIS.CLIENT_ID.eq(clientId))
        .orderBy(ANAMNESIS.VERSION_NUMBER.desc())
        .fetch(
            r ->
                new VersionSummary(
                    r.value1(),
                    r.value2().toInstant(),
                    r.value3() == null ? null : r.value3().toInstant(),
                    r.value4(),
                    r.value5()));
  }

  @Override
  public void insert(Anamnesis a) {
    dsl.insertInto(ANAMNESIS)
        .set(ANAMNESIS.ID, a.id())
        .set(ANAMNESIS.CLIENT_ID, a.clientId())
        .set(ANAMNESIS.COACHING_LINK_ID, a.linkId())
        .set(ANAMNESIS.TEMPLATE_ID, a.templateId())
        .set(ANAMNESIS.VERSION_NUMBER, a.versionNumber())
        .set(ANAMNESIS.FILLED_BY, a.filledBy())
        .set(ANAMNESIS.CREATED_AT, a.createdAt().atOffset(ZoneOffset.UTC))
        .set(ANAMNESIS.ANSWERS, encrypted(a))
        .set(ANAMNESIS.GOAL, a.answers().text("goal"))
        .set(ANAMNESIS.ACTIVITY_LEVEL, a.answers().text("activity_level"))
        .set(ANAMNESIS.WEEKLY_DAYS, shortOf(a.answers().integer("weekly_days")))
        .set(ANAMNESIS.SESSION_MINUTES, shortOf(a.answers().integer("session_minutes")))
        .set(ANAMNESIS.PARQ_POSITIVE, a.answers().parqPositive())
        .set(ANAMNESIS.MEDICAL_CLEARANCE, a.clearance().status().code())
        .set(ANAMNESIS.CLEARANCE_DATE, a.clearance().date())
        .set(ANAMNESIS.REVIEWED_BY, a.reviewedBy())
        .set(
            ANAMNESIS.REVIEWED_AT,
            a.reviewedAt() == null ? null : a.reviewedAt().atOffset(ZoneOffset.UTC))
        .execute();
  }

  @Override
  public void update(Anamnesis a) {
    dsl.update(ANAMNESIS)
        .set(ANAMNESIS.ANSWERS, encrypted(a))
        .set(ANAMNESIS.FILLED_BY, a.filledBy())
        .set(ANAMNESIS.GOAL, a.answers().text("goal"))
        .set(ANAMNESIS.ACTIVITY_LEVEL, a.answers().text("activity_level"))
        .set(ANAMNESIS.WEEKLY_DAYS, shortOf(a.answers().integer("weekly_days")))
        .set(ANAMNESIS.SESSION_MINUTES, shortOf(a.answers().integer("session_minutes")))
        .set(ANAMNESIS.PARQ_POSITIVE, a.answers().parqPositive())
        .set(ANAMNESIS.MEDICAL_CLEARANCE, a.clearance().status().code())
        .set(ANAMNESIS.CLEARANCE_DATE, a.clearance().date())
        .set(ANAMNESIS.REVIEWED_BY, a.reviewedBy())
        .set(
            ANAMNESIS.REVIEWED_AT,
            a.reviewedAt() == null ? null : a.reviewedAt().atOffset(ZoneOffset.UTC))
        .where(ANAMNESIS.ID.eq(a.id()))
        .and(ANAMNESIS.REVIEWED_AT.isNull())
        .execute();
  }

  @Override
  public void announce(UUID anamnesisId, UUID clientId, String eventType) {
    // só ids: dado de saúde nunca vai para evento (CLAUDE.md, regra 7)
    dsl.insertInto(OUTBOX_EVENT)
        .set(OUTBOX_EVENT.AGGREGATE_TYPE, "anamnesis")
        .set(OUTBOX_EVENT.AGGREGATE_ID, anamnesisId)
        .set(OUTBOX_EVENT.TYPE, eventType)
        .set(
            OUTBOX_EVENT.PAYLOAD,
            JSONB.valueOf(
                "{\"anamnesisId\": \"" + anamnesisId + "\", \"clientId\": \"" + clientId + "\"}"))
        .execute();
  }

  private JSONB encrypted(Anamnesis a) {
    var plain = JSON.writeValueAsString(a.answers().values());
    return JSONB.valueOf(
        JSON.writeValueAsString(cipher.encrypt(a.clientId(), ANSWERS_FIELD, plain)));
  }

  private Anamnesis toDomain(Record record) {
    var r = record.into(ANAMNESIS);
    var template = template(r.getTemplateId());
    var stored = JSON.readTree(r.getAnswers().data());
    // string = cifrado; objeto = gravado antes da criptografia de campo
    var plain =
        stored.isString()
            ? cipher.decrypt(r.getClientId(), ANSWERS_FIELD, stored.asString())
            : stored.toString();
    Map<String, Object> values = JSON.readValue(plain, MAP);
    var parq = template.questions().stream().filter(Question::isParq).map(Question::code).toList();
    return new Anamnesis(
        r.getId(),
        r.getClientId(),
        r.getCoachingLinkId(),
        r.getVersionNumber(),
        r.getTemplateId(),
        new Answers(values, parq),
        new Anamnesis.Clearance(
            Anamnesis.ClearanceStatus.fromCode(r.getMedicalClearance()), r.getClearanceDate()),
        r.getFilledBy(),
        r.getReviewedBy(),
        r.getReviewedAt() == null ? null : r.getReviewedAt().toInstant(),
        r.getCreatedAt().toInstant());
  }

  private static Short shortOf(Integer value) {
    return value == null ? null : value.shortValue();
  }
}
