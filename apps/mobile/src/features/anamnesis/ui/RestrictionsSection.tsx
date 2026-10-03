import { useState } from "react";
import { Alert, Pressable, StyleSheet, Text, View } from "react-native";
import { toAppError } from "../../../shared/lib/http";
import { BottomSheet } from "../../../shared/ui/BottomSheet";
import { Button } from "../../../shared/ui/Button";
import { Chip } from "../../../shared/ui/Chip";
import { errorMessage } from "../../../shared/ui/error-messages";
import { Icon } from "../../../shared/ui/Icon";
import { Message } from "../../../shared/ui/Message";
import { TextField } from "../../../shared/ui/TextField";
import { TextLink } from "../../../shared/ui/TextLink";
import { Toggle } from "../../../shared/ui/Toggle";
import { MIN_TOUCH, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import type { Restriction, RestrictionInput, RestrictionKind } from "../domain/anamnesis";
import { useDeleteRestriction, useRestrictions, useSaveRestriction } from "../hooks/use-anamnesis";
import { strings } from "./strings";

const t = strings.restrictions;
const KINDS: readonly RestrictionKind[] = ["injury", "surgery", "pain", "condition"];
const REGIONS = Object.keys(strings.regions);

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function restrictionTitle(r: Pick<Restriction, "kind" | "bodyRegion">): string {
  const region = r.bodyRegion === null ? null : (strings.regions[r.bodyRegion] ?? null);
  return region === null ? t.kinds[r.kind] : `${t.kinds[r.kind]} · ${region}`;
}

/** Lista de restrições do aluno com criar, editar, resolver e excluir (folha inferior). */
export function RestrictionsSection({ linkId }: { readonly linkId: string }) {
  const restrictions = useRestrictions(linkId);
  const [editing, setEditing] = useState<Restriction | "new" | null>(null);
  const all = restrictions.data ?? [];
  const activeOnes = all.filter((r) => r.resolvedOn === null);
  const resolved = all.filter((r) => r.resolvedOn !== null);

  return (
    <View style={styles.section}>
      <Text accessibilityRole="header" style={[typography.headline, { color: palette.text }]}>
        {t.title}
      </Text>
      {restrictions.isSuccess && activeOnes.length === 0 ? (
        <Text style={[typography.small, { color: palette.muted }]}>{t.empty}</Text>
      ) : null}
      {activeOnes.map((r) => (
        <Row
          key={r.id}
          restriction={r}
          onPress={() => {
            setEditing(r);
          }}
        />
      ))}
      <TextLink
        label={t.add}
        onPress={() => {
          setEditing("new");
        }}
      />
      {resolved.length > 0 ? (
        <>
          <Text style={[typography.label, { color: palette.muted, marginTop: spacing.sm }]}>
            {t.resolved}
          </Text>
          {resolved.map((r) => (
            <Row
              key={r.id}
              restriction={r}
              onPress={() => {
                setEditing(r);
              }}
            />
          ))}
        </>
      ) : null}
      {editing === null ? null : (
        <RestrictionSheet
          key={editing === "new" ? "new" : editing.id}
          linkId={linkId}
          restriction={editing === "new" ? null : editing}
          onClose={() => {
            setEditing(null);
          }}
        />
      )}
    </View>
  );
}

function Row({
  restriction,
  onPress,
}: {
  readonly restriction: Restriction;
  readonly onPress: () => void;
}) {
  const resolved = restriction.resolvedOn !== null;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${restrictionTitle(restriction)}: ${restriction.description}. ${t.edit}`}
      onPress={onPress}
      style={[styles.row, resolved ? null : styles.rowActive]}
    >
      <Icon name="alert" size={18} color={resolved ? palette.muted : palette.red} />
      <View style={{ flex: 1, gap: 2 }}>
        <Text style={[typography.label, { color: resolved ? palette.muted : palette.text }]}>
          {restrictionTitle(restriction)}
        </Text>
        <Text style={[typography.small, { color: palette.textSoft }]}>
          {restriction.description}
        </Text>
      </View>
      <Icon name="chevron" size={18} color={palette.muted} />
    </Pressable>
  );
}

function RestrictionSheet({
  linkId,
  restriction,
  onClose,
}: {
  readonly linkId: string;
  readonly restriction: Restriction | null;
  readonly onClose: () => void;
}) {
  const [input, setInput] = useState<RestrictionInput>(
    restriction ?? {
      kind: "injury",
      bodyRegion: null,
      description: "",
      severity: null,
      resolvedOn: null,
    },
  );
  const save = useSaveRestriction(linkId);
  const remove = useDeleteRestriction(linkId);
  const set = (patch: Partial<RestrictionInput>) => {
    setInput((current) => ({ ...current, ...patch }));
  };

  return (
    <BottomSheet
      visible
      title={restriction === null ? t.sheetNew : t.sheetEdit}
      onClose={onClose}
      footer={
        <>
          {save.isError ? <Message text={errorMessage(toAppError(save.error))} /> : null}
          <Button
            label={t.save}
            icon="check"
            disabled={input.description.trim() === ""}
            loading={save.isPending}
            onPress={() => {
              save.mutate(
                {
                  id: restriction?.id ?? null,
                  input: { ...input, description: input.description.trim() },
                },
                { onSuccess: onClose },
              );
            }}
          />
          {restriction === null ? null : (
            <TextLink
              label={t.remove}
              onPress={() => {
                Alert.alert(t.removeTitle, t.removeText, [
                  { text: t.cancel, style: "cancel" },
                  {
                    text: t.remove,
                    style: "destructive",
                    onPress: () => {
                      remove.mutate(restriction.id, { onSuccess: onClose });
                    },
                  },
                ]);
              }}
            />
          )}
        </>
      }
    >
      <Text style={[typography.label, styles.label]}>{t.kind}</Text>
      <View accessibilityRole="radiogroup" accessibilityLabel={t.kind} style={styles.chips}>
        {KINDS.map((kind) => (
          <Chip
            key={kind}
            role="radio"
            label={t.kinds[kind]}
            selected={input.kind === kind}
            onPress={() => {
              set({ kind });
            }}
          />
        ))}
      </View>
      <Text style={[typography.label, styles.label]}>{t.region}</Text>
      <View accessibilityRole="radiogroup" accessibilityLabel={t.region} style={styles.chips}>
        <Chip
          role="radio"
          label={t.noRegion}
          selected={input.bodyRegion === null}
          onPress={() => {
            set({ bodyRegion: null });
          }}
        />
        {REGIONS.map((region) => (
          <Chip
            key={region}
            role="radio"
            label={strings.regions[region] ?? region}
            selected={input.bodyRegion === region}
            onPress={() => {
              set({ bodyRegion: region });
            }}
          />
        ))}
      </View>
      <TextField
        label={t.description}
        value={input.description}
        multiline
        maxLength={500}
        onChangeText={(description) => {
          set({ description });
        }}
      />
      <Text style={[typography.label, styles.label]}>{t.severity}</Text>
      <View accessibilityRole="radiogroup" accessibilityLabel={t.severity} style={styles.chips}>
        {t.severities.map((label, index) => (
          <Chip
            key={label}
            role="radio"
            label={label}
            selected={input.severity === index + 1}
            onPress={() => {
              set({ severity: input.severity === index + 1 ? null : index + 1 });
            }}
          />
        ))}
      </View>
      <Toggle
        label={t.resolvedToggle}
        value={input.resolvedOn !== null}
        onChange={(on) => {
          set({ resolvedOn: on ? (restriction?.resolvedOn ?? today()) : null });
        }}
      />
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  section: { gap: spacing.sm },
  row: {
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.sm + 2,
    minHeight: MIN_TOUCH + 8,
    padding: spacing.md,
    borderRadius: radius.md,
    backgroundColor: palette.surface,
  },
  rowActive: { backgroundColor: palette.redSoft },
  label: { color: palette.text },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: spacing.sm },
});
