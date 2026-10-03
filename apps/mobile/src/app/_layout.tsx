import { useFonts } from "expo-font";
import { router, Stack, type ErrorBoundaryProps } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { StatusBar } from "expo-status-bar";
import { useEffect } from "react";
import { AppProviders } from "../providers/AppProviders";
import { toAppError } from "../shared/lib/http";
import { ErrorScreen } from "../shared/ui/ErrorScreen";
import { fontAssets } from "../shared/ui/font-assets";
import { palette } from "../shared/ui/theme";

// A abertura fica na tela até as fontes carregarem: sem piscar com a fonte do sistema.
void SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [loaded, error] = useFonts(fontAssets);

  useEffect(() => {
    if (loaded || error !== null) {
      void SplashScreen.hideAsync();
    }
  }, [loaded, error]);

  // Se uma fonte falhar, o app segue com a fonte do sistema em vez de travar na abertura.
  if (!loaded && error === null) {
    return null;
  }
  return (
    <AppProviders>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: palette.background },
        }}
      />
    </AppProviders>
  );
}

/** Erro inesperado em qualquer rota: tela de erro do sistema visual, sem detalhe técnico. */
export function ErrorBoundary({ error, retry }: ErrorBoundaryProps) {
  const failure = toAppError(error);
  return (
    <ErrorScreen
      code={failure.kind === "problem" ? failure.traceId : null}
      onRetry={() => {
        void retry();
      }}
      onHome={() => {
        router.replace("/");
      }}
    />
  );
}
