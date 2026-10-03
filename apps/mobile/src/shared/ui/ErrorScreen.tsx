import { Text, View } from "react-native";
import { Button } from "./Button";
import { Screen } from "./Screen";
import { TextLink } from "./TextLink";
import { palette, spacing, typography } from "./theme";

const t = {
  title: "Essa tela\ntravou.",
  text: "Não foi nada que você fez. Seus dados continuam salvos.",
  support: (code: string) => `Se continuar, mande este código para o suporte: ${code}`,
  retry: "Tentar de novo",
  home: "Voltar ao início",
};

interface Props {
  readonly onRetry: () => void;
  readonly onHome?: () => void;
  /** Código de rastreio do erro (traceId da API), quando existir. */
  readonly code?: string | null;
}

/** Erro inesperado (fronteira de erro das rotas): nunca mostra detalhe técnico. */
export function ErrorScreen({ onRetry, onHome, code = null }: Props) {
  return (
    <Screen
      title={t.title}
      footer={
        <>
          <Button label={t.retry} icon="refresh" onPress={onRetry} />
          {onHome === undefined ? null : <TextLink label={t.home} onPress={onHome} />}
        </>
      }
    >
      <View style={{ gap: spacing.md }}>
        <Text style={[typography.body, { color: palette.textSoft }]}>{t.text}</Text>
        {code === null ? null : (
          <Text selectable style={[typography.small, { color: palette.muted }]}>
            {t.support(code)}
          </Text>
        )}
      </View>
    </Screen>
  );
}
