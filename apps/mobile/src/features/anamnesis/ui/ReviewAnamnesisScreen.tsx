import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Segmented } from "../../../shared/ui/Segmented";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { palette, radius, spacing, typography } from "../../../shared/ui/theme";
import {
  cleaned,
  questionsOf,
  SECTIONS,
  type AnamnesisRecord,
  type Answers,
  type AnswerValue,
  type Clearance,
  type Question,
  type Template,
  withAnswer,
} from "../domain/anamnesis";
import {
  useAnamnesisTemplate,
  useClientAnamnesis,
  useReviewAnamnesis,
} from "../hooks/use-anamnesis";
import { QuestionField } from "./QuestionField";
import { RestrictionsSection } from "./RestrictionsSection";
import { strings } from "./strings";

const t = strings.review;
const dateFormat = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
  year: "numeric",
});

function display(question: Question, value: AnswerValue | undefined): string {
  if (value === undefined) {
    return t.unanswered;
  }
  if (typeof value === "boolean") {
    return value ? strings.form.yes : strings.form.no;
  }
  const label = (v: string) => question.options.find((o) => o.value === v)?.label ?? v;
  if (Array.isArray(value)) {
    return value.map(label).join(", ");
  }
  return typeof value === "string" ? label(value) : String(value);
}

/** Rota /anamnesis/[linkId] do personal: revisar, complementar, liberar e as restrições. */
export function ReviewAnamnesisScreen() {
  const { linkId, name } = useLocalSearchParams<{ linkId: string; name?: string }>();
  const template = useAnamnesisTemplate();
  const anamnesis = useClientAnamnesis(linkId);
  const back = (
    <RoundButton
      icon="back"
      label={t.back}
      onPress={() => {
        router.back();
      }}
    />
  );
  if (template.data === undefined || anamnesis.data === undefined) {
    return (
      <Screen header={back} title={t.title}>
        {anamnesis.isError || template.isError ? (
          <Message text={errorMessage(toAppError(anamnesis.error ?? template.error))} />
        ) : (
          <Skeleton width="100%" height={220} rounded={24} />
        )}
      </Screen>
    );
  }
  const latest = anamnesis.data.latest;
  return (
    <Screen header={back} title={t.title} {...(name ? { subtitle: name } : {})}>
      {latest === null ? (
        <EmptyState icon="doc" title={t.empty} text={t.emptyText} />
      ) : (
        <Review
          key={`${String(latest.versionNumber)}-${String(latest.reviewed)}`}
          linkId={linkId}
          template={template.data}
          latest={latest}
        />
      )}
      <RestrictionsSection linkId={linkId} />
      {anamnesis.data.versions.length > 1 ? (
        <View style={styles.versions}>
          <Text style={[typography.label, { color: palette.muted }]}>{t.versions}</Text>
          {anamnesis.data.versions.map((v) => (
            <Text key={v.versionNumber} style={[typography.small, { color: palette.textSoft }]}>
              {t.version(v.versionNumber)} ·{" "}
              {v.reviewedAt === null ? t.toReview : t.reviewed(dateFormat.format(v.reviewedAt))}
            </Text>
          ))}
        </View>
      ) : null}
    </Screen>
  );
}

function Review({
  linkId,
  template,
  latest,
}: {
  readonly linkId: string;
  readonly template: Template;
  readonly latest: AnamnesisRecord;
}) {
  const [answers, setAnswers] = useState<Answers>(latest.answers);
  const [editing, setEditing] = useState(false);
  const [clearance, setClearance] = useState<Clearance>(latest.clearance);
  const [clearanceDate, setClearanceDate] = useState(latest.clearanceDate ?? "");
  const review = useReviewAnamnesis(linkId);
  const dirty =
    !latest.reviewed ||
    clearance !== latest.clearance ||
    (clearanceDate || null) !== latest.clearanceDate ||
    JSON.stringify(answers) !== JSON.stringify(latest.answers);

  return (
    <View style={styles.wrap}>
      <View style={styles.head}>
        <Text style={[typography.headline, { color: palette.text }]}>
          {t.version(latest.versionNumber)}
        </Text>
        <Text style={[typography.small, { color: latest.reviewed ? palette.muted : palette.lime }]}>
          {latest.reviewed && latest.reviewedAt !== null
            ? t.reviewed(dateFormat.format(latest.reviewedAt))
            : t.toReview}
        </Text>
      </View>
      {latest.parqPositive ? <Message text={t.parq} /> : null}

      <View style={styles.card}>
        <Text style={[typography.label, { color: palette.text }]}>{t.clearance}</Text>
        <Segmented
          label={t.clearance}
          value={clearance}
          onChange={setClearance}
          options={(["not_required", "pending", "cleared"] as const).map((value) => ({
            value,
            label: t.clearanceOptions[value],
          }))}
        />
        {clearance === "cleared" ? (
          <TextField
            label={t.clearanceDate}
            value={clearanceDate}
            placeholder="2026-10-01"
            keyboardType="numbers-and-punctuation"
            maxLength={10}
            onChangeText={setClearanceDate}
          />
        ) : null}
      </View>

      <TextLink
        label={editing ? t.stopEdit : t.edit}
        onPress={() => {
          setEditing(!editing);
        }}
      />
      {SECTIONS.map((section) => (
        <View key={section} style={styles.card}>
          <Text style={[typography.label, { color: palette.muted }]}>
            {strings.sections[section].title.replace("\n", " ")}
          </Text>
          {questionsOf(template, section).map((q) =>
            editing ? (
              <QuestionField
                key={q.code}
                question={q}
                value={answers[q.code]}
                onChange={(value) => {
                  setAnswers(withAnswer(answers, q.code, value));
                }}
              />
            ) : (
              <View key={q.code} style={{ gap: 2 }}>
                <Text style={[typography.small, { color: palette.muted }]}>{q.label}</Text>
                <Text
                  style={[
                    typography.body,
                    {
                      color:
                        section === "parq" && answers[q.code] === true ? palette.red : palette.text,
                    },
                  ]}
                >
                  {display(q, answers[q.code])}
                </Text>
              </View>
            ),
          )}
        </View>
      ))}

      {review.isError ? <Message text={errorMessage(toAppError(review.error))} /> : null}
      {review.isSuccess && !dirty ? <Message tone="info" text={t.saved} /> : null}
      <Text style={[typography.small, { color: palette.muted }]}>{t.lockedHint}</Text>
      <Button
        label={latest.reviewed ? t.saveNew : t.save}
        icon="check"
        disabled={!dirty}
        loading={review.isPending}
        onPress={() => {
          review.mutate({
            answers: cleaned(answers),
            clearance,
            clearanceDate: clearance === "cleared" ? clearanceDate.trim() || null : null,
          });
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.md },
  head: { gap: 2 },
  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.sm + 2,
  },
  versions: { gap: spacing.xs },
});
