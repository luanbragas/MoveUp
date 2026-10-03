import { router } from "expo-router";
import { useDeferredValue, useState } from "react";
import { Alert, ScrollView, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { Chip } from "../../../shared/ui/Chip";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { TextField } from "../../../shared/ui/TextField";
import { palette, spacing, typography } from "../../../shared/ui/theme";
import { MUSCLE_CODES, type Exercise, type MuscleCode } from "../domain/exercise";
import { useArchiveExercise, useExerciseSearch } from "../hooks/use-exercises";
import { ExerciseRow } from "./ExerciseRow";
import { strings } from "./strings";

const t = strings.library;

interface Props {
  /** Modo escolha (editor de treino): marca vários e confirma. Sem isto, só consulta. */
  readonly onPick?: (exercises: readonly Exercise[]) => void;
  /** Fecha sem escolher (modo escolha dentro de outra tela). Padrão: voltar a rota. */
  readonly onClose?: () => void;
}

/** Biblioteca de exercícios (design 5.7): busca sem acento, filtro por músculo e "como fazer". */
export function ExerciseLibraryScreen({ onPick, onClose }: Props) {
  const [query, setQuery] = useState("");
  const [muscle, setMuscle] = useState<MuscleCode | null>(null);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [picked, setPicked] = useState<readonly Exercise[]>([]);
  const deferredQuery = useDeferredValue(query);
  const search = useExerciseSearch(deferredQuery, muscle);
  const archive = useArchiveExercise();
  const results = search.data ?? [];

  const toggle = (exercise: Exercise) => {
    setPicked((current) =>
      current.some((e) => e.id === exercise.id)
        ? current.filter((e) => e.id !== exercise.id)
        : [...current, exercise],
    );
  };

  return (
    <Screen
      header={
        <RoundButton
          icon={onPick === undefined ? "back" : "close"}
          label={t.back}
          onPress={() => {
            if (onClose === undefined) {
              router.back();
            } else {
              onClose();
            }
          }}
        />
      }
      title={t.title}
      footer={
        onPick === undefined ? (
          <Button
            label={t.create}
            variant="secondary"
            icon="plus"
            onPress={() => {
              router.push("/exercises/new");
            }}
          />
        ) : (
          <Button
            label={t.add(picked.length)}
            icon="plus"
            disabled={picked.length === 0}
            onPress={() => {
              onPick(picked);
            }}
          />
        )
      }
    >
      <TextField
        label={t.search}
        value={query}
        onChangeText={setQuery}
        placeholder={t.searchPlaceholder}
        autoCapitalize="none"
        autoComplete="off"
      />
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        accessibilityRole="radiogroup"
        accessibilityLabel={t.filter}
        contentContainerStyle={styles.chips}
      >
        <Chip
          label={t.all}
          role="radio"
          selected={muscle === null}
          onPress={() => {
            setMuscle(null);
          }}
        />
        {MUSCLE_CODES.map((code) => (
          <Chip
            key={code}
            label={strings.muscles[code]}
            role="radio"
            selected={muscle === code}
            onPress={() => {
              setMuscle(muscle === code ? null : code);
            }}
          />
        ))}
      </ScrollView>

      {search.isPending ? (
        <>
          <Skeleton width="100%" height={68} rounded={24} />
          <Skeleton width="100%" height={68} rounded={24} />
          <Skeleton width="100%" height={68} rounded={24} />
        </>
      ) : null}
      {search.isError ? (
        <>
          <Message text={t.error} />
          <Button
            label={t.retry}
            variant="secondary"
            icon="refresh"
            onPress={() => {
              void search.refetch();
            }}
          />
        </>
      ) : null}
      {search.isSuccess && results.length === 0 ? (
        <EmptyState icon="search" title={t.empty} text={t.emptyText} />
      ) : null}
      {search.isSuccess && results.length > 0 ? (
        <Text style={[typography.small, { color: palette.muted }]}>{t.count(results.length)}</Text>
      ) : null}
      {archive.isError ? <Message text={errorMessage(toAppError(archive.error))} /> : null}
      <View style={[styles.list, search.isPlaceholderData ? styles.stale : null]}>
        {results.map((exercise) => (
          <ExerciseRow
            key={exercise.id}
            exercise={exercise}
            expanded={expanded === exercise.id}
            onToggle={() => {
              setExpanded(expanded === exercise.id ? null : exercise.id);
            }}
            {...(onPick === undefined
              ? {
                  onArchive: () => {
                    Alert.alert(t.archiveTitle, t.archiveMessage, [
                      { text: t.archiveBack, style: "cancel" },
                      {
                        text: t.archive,
                        style: "destructive",
                        onPress: () => {
                          archive.mutate(exercise.id);
                        },
                      },
                    ]);
                  },
                }
              : {
                  selected: picked.some((e) => e.id === exercise.id),
                  onSelect: () => {
                    toggle(exercise);
                  },
                })}
          />
        ))}
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  chips: { gap: spacing.sm, paddingVertical: 2 },
  list: { gap: spacing.sm },
  stale: { opacity: 0.6 },
});
