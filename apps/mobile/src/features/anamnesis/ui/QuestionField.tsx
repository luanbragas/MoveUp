import { Pressable, StyleSheet, Text, View } from "react-native";
import { Chip } from "../../../shared/ui/Chip";
import { Icon } from "../../../shared/ui/Icon";
import { TextField } from "../../../shared/ui/TextField";
import { fonts, MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { toggleOption, type AnswerValue, type Question } from "../domain/anamnesis";
import { strings } from "./strings";

const t = strings.form;

interface Props {
  readonly question: Question;
  readonly value: AnswerValue | undefined;
  readonly onChange: (value: AnswerValue | undefined) => void;
}

/** Uma pergunta do modelo, no controle certo para o tipo (escolha, sim/não, número, texto). */
export function QuestionField({ question, value, onChange }: Props) {
  const label = question.required ? question.label : question.label;
  return (
    <View style={styles.wrap}>
      {question.type === "text" ? null : (
        <Text style={[typography.label, styles.label]}>{label}</Text>
      )}
      {question.type === "single" ? (
        <View accessibilityRole="radiogroup" accessibilityLabel={label} style={styles.chips}>
          {question.options.map((o) => (
            <Chip
              key={o.value}
              role="radio"
              label={o.label}
              selected={value === o.value}
              onPress={() => {
                onChange(o.value);
              }}
            />
          ))}
        </View>
      ) : null}
      {question.type === "multi" ? (
        <View style={styles.chips}>
          {question.options.map((o) => (
            <Chip
              key={o.value}
              label={o.label}
              selected={Array.isArray(value) && value.includes(o.value)}
              onPress={() => {
                onChange(toggleOption(value, o.value));
              }}
            />
          ))}
        </View>
      ) : null}
      {question.type === "yes_no" ? (
        <View accessibilityRole="radiogroup" accessibilityLabel={label} style={styles.chips}>
          <Chip
            role="radio"
            label={t.yes}
            selected={value === true}
            onPress={() => {
              onChange(true);
            }}
          />
          <Chip
            role="radio"
            label={t.no}
            selected={value === false}
            onPress={() => {
              onChange(false);
            }}
          />
        </View>
      ) : null}
      {question.type === "integer" ? (
        <Stepper
          question={question}
          value={typeof value === "number" ? value : null}
          onChange={onChange}
        />
      ) : null}
      {question.type === "text" ? (
        <TextField
          label={label}
          value={typeof value === "string" ? value : ""}
          multiline
          onChangeText={(text) => {
            onChange(text === "" ? undefined : text);
          }}
        />
      ) : null}
    </View>
  );
}

function Stepper({
  question,
  value,
  onChange,
}: {
  readonly question: Question;
  readonly value: number | null;
  readonly onChange: (value: number) => void;
}) {
  const min = question.min ?? 0;
  const max = question.max ?? 999;
  // minutos andam de 5 em 5; dias, de 1 em 1
  const step = max > 30 ? 5 : 1;
  const current = value ?? min;
  return (
    <View style={styles.stepper}>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${t.less}: ${question.label}`}
        disabled={value !== null && current <= min}
        onPress={() => {
          onChange(Math.max(min, value === null ? min : current - step));
        }}
        style={styles.stepButton}
      >
        <Icon name="down" size={18} color={palette.text} />
      </Pressable>
      <Text
        accessibilityLabel={`${question.label}: ${value === null ? "sem resposta" : String(value)}`}
        style={styles.value}
      >
        {value ?? "–"}
      </Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`${t.more}: ${question.label}`}
        disabled={current >= max}
        onPress={() => {
          onChange(Math.min(max, value === null ? min : current + step));
        }}
        style={styles.stepButton}
      >
        <Icon name="up" size={18} color={palette.text} />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: spacing.sm },
  label: { color: palette.text, fontSize: 16 },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
  stepper: { flexDirection: "row", alignItems: "center", gap: spacing.md },
  stepButton: {
    width: MIN_TOUCH,
    height: MIN_TOUCH,
    borderRadius: radius.pill,
    backgroundColor: palette.surface2,
    alignItems: "center",
    justifyContent: "center",
  },
  value: {
    fontFamily: fonts.number,
    fontSize: 32,
    color: palette.text,
    minWidth: 64,
    textAlign: "center",
  },
});
