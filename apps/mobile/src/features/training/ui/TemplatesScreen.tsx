import { router } from "expo-router";
import { toAppError } from "../../../shared/lib/http";
import { Button } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { errorMessage } from "../../../shared/ui/error-messages";
import { ListRow } from "../../../shared/ui/ListRow";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { Skeleton } from "../../../shared/ui/Skeleton";
import { useCreateTemplate, useTemplates } from "../hooks/use-training";
import { strings } from "./strings";

const t = strings.templates;

/** Aba Treinos do personal: modelos e a biblioteca de exercícios. */
export function TemplatesScreen() {
  const templates = useTemplates();
  const create = useCreateTemplate();
  const items = templates.data ?? [];

  const createTemplate = () => {
    create.mutate(
      { name: t.newName, goal: null, estimatedMinutes: null, notes: null, blocks: [] },
      {
        onSuccess: (workout) => {
          router.push({ pathname: "/workouts/[id]", params: { id: workout.id } });
        },
      },
    );
  };

  return (
    <Screen
      title={t.title}
      subtitle={t.subtitle}
      refresh={{
        refreshing: templates.isRefetching,
        onRefresh: () => {
          void templates.refetch();
        },
      }}
    >
      <ListRow
        title={t.library}
        subtitle={t.librarySubtitle}
        icon="search"
        last
        onPress={() => {
          router.push("/exercises");
        }}
      />
      {templates.isPending ? (
        <>
          <Skeleton width="100%" height={64} rounded={24} />
          <Skeleton width="100%" height={64} rounded={24} />
        </>
      ) : null}
      {templates.isError ? (
        <>
          <Message text={t.error} />
          <Button
            label={t.retry}
            variant="secondary"
            icon="refresh"
            onPress={() => {
              void templates.refetch();
            }}
          />
        </>
      ) : null}
      {templates.isSuccess && items.length === 0 ? (
        <EmptyState
          icon="dumbbell"
          title={t.emptyTitle}
          text={t.emptyText}
          action={
            <Button
              label={t.create}
              icon="plus"
              loading={create.isPending}
              onPress={createTemplate}
            />
          }
        />
      ) : null}
      {items.map((template, index) => (
        <ListRow
          key={template.id}
          title={template.name}
          subtitle={t.meta(template.exercises, template.estimatedMinutes)}
          icon="dumbbell"
          last={index === items.length - 1}
          onPress={() => {
            router.push({ pathname: "/workouts/[id]", params: { id: template.id } });
          }}
        />
      ))}
      {items.length > 0 ? (
        <Button
          label={t.create}
          variant="secondary"
          icon="plus"
          loading={create.isPending}
          onPress={createTemplate}
        />
      ) : null}
      {create.isError ? <Message text={errorMessage(toAppError(create.error))} /> : null}
    </Screen>
  );
}
