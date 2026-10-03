import { router } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { Chevrons } from "../../../shared/ui/Chevrons";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { Steps } from "../../../shared/ui/Steps";
import { TextLink } from "../../../shared/ui/TextLink";
import { Title } from "../../../shared/ui/Title";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import {
  cleaned,
  missing,
  parqPositive,
  questionsOf,
  SECTIONS,
  type Answers,
  type Template,
  withAnswer,
} from "../domain/anamnesis";
import {
  useAnamnesisDraft,
  useAnamnesisTemplate,
  useMyAnamnesis,
  useSaveAnamnesisDraft,
  useSubmitAnamnesis,
} from "../hooks/use-anamnesis";
import { QuestionField } from "./QuestionField";
import { strings } from "./strings";

const t = strings.form;

/**
 * Rota /anamnesis do aluno (SCREEN-FLOWS 1.2): quatro passos curtos, cada resposta já fica no
 * aparelho (terminar depois) e o envio vai uma vez no fim.
 */
export function ClientAnamnesisScreen() {
  const template = useAnamnesisTemplate();
  const mine = useMyAnamnesis();
  const draft = useAnamnesisDraft();
  if (template.data === undefined || mine.isPending || draft.isPending) {
    return (
      <Screen>
        {template.isError ? (
          <Message text={errorMessage(toAppError(template.error))} />
        ) : (
          <>
            <Skeleton width="60%" height={80} />
            <Skeleton width="100%" height={200} rounded={24} />
          </>
        )}
      </Screen>
    );
  }
  // começa do rascunho, ou da última versão enviada (atualizar), ou do zero
  const start = draft.data ?? mine.data?.answers ?? {};
  return <Wizard template={template.data} initial={start} />;
}

function Wizard({ template, initial }: { readonly template: Template; readonly initial: Answers }) {
  const [answers, setAnswers] = useState<Answers>(initial);
  const [step, setStep] = useState(0);
  const [tried, setTried] = useState(false);
  const saveDraft = useSaveAnamnesisDraft();
  const submit = useSubmitAnamnesis();
  const section = SECTIONS[step] ?? "goal";
  const last = step === SECTIONS.length - 1;
  const pending = missing(template, answers, section);

  if (submit.isSuccess) {
    return (
      <Screen
        footer={
          <Button
            label={t.done}
            onPress={() => {
              router.back();
            }}
          />
        }
      >
        <View style={styles.hero}>
          <Chevrons size={88} count={3} />
          <Title size={40}>{t.sent}</Title>
          <Text style={[typography.body, { color: palette.textSoft }]}>{t.sentText}</Text>
          {submit.data.parqPositive ? <Message tone="info" text={t.parqWarning} /> : null}
        </View>
      </Screen>
    );
  }

  const change = (code: string, value: Answers[string] | undefined) => {
    const next = withAnswer(answers, code, value);
    setAnswers(next);
    saveDraft(next);
  };

  return (
    <Screen
      header={
        <>
          <RoundButton
            icon="back"
            label={t.back}
            onPress={() => {
              if (step === 0) {
                router.back();
              } else {
                setStep(step - 1);
                setTried(false);
              }
            }}
          />
          <View style={styles.flex}>
            <Steps current={step + 1} total={SECTIONS.length} />
          </View>
        </>
      }
      title={strings.sections[section].title}
      subtitle={strings.sections[section].hint}
      footer={
        <>
          {tried && pending.length > 0 ? (
            <Text style={[typography.small, styles.missing]}>{t.missing}</Text>
          ) : null}
          <Button
            label={last ? t.send : t.next}
            icon={last ? "check" : "arrow"}
            loading={submit.isPending}
            onPress={() => {
              if (pending.length > 0) {
                setTried(true);
                return;
              }
              setTried(false);
              if (last) {
                submit.mutate(cleaned(answers));
              } else {
                setStep(step + 1);
              }
            }}
          />
          <TextLink
            label={t.later}
            onPress={() => {
              router.back();
            }}
          />
        </>
      }
    >
      <Text style={[typography.small, { color: palette.muted }]}>
        {t.step(step + 1, SECTIONS.length)}
      </Text>
      {section === "parq" && parqPositive(template, answers) ? (
        <Message tone="info" text={t.parqWarning} />
      ) : null}
      {submit.isError ? <Message text={errorMessage(toAppError(submit.error))} /> : null}
      {questionsOf(template, section).map((q) => (
        <QuestionField
          key={q.code}
          question={q}
          value={answers[q.code]}
          onChange={(value) => {
            change(q.code, value);
          }}
        />
      ))}
    </Screen>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  hero: { gap: spacing.md, paddingTop: spacing.xl },
  missing: { color: palette.red, textAlign: "center" },
});
