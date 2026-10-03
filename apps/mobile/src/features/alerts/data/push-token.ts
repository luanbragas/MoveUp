import Constants from "expo-constants";
import * as Notifications from "expo-notifications";
import { Platform } from "react-native";
import type { PushTokenSource } from "../domain/ports";

/** Projeto EAS (app.json → extra.eas.projectId): o Expo Push exige para gerar o token. */
function projectId(): string | null {
  const extra: unknown = Constants.expoConfig?.extra;
  if (typeof extra !== "object" || extra === null || !("eas" in extra)) {
    return null;
  }
  const eas: unknown = extra.eas;
  if (typeof eas !== "object" || eas === null || !("projectId" in eas)) {
    return null;
  }
  return typeof eas.projectId === "string" ? eas.projectId : null;
}

export function createExpoPushTokenSource(): PushTokenSource {
  return {
    async current(ask) {
      if (Platform.OS !== "ios" && Platform.OS !== "android") {
        return null;
      }
      const id = projectId();
      if (id === null) {
        return null; // sem projeto EAS ainda: push fica para o primeiro build
      }
      try {
        let permission = await Notifications.getPermissionsAsync();
        if (!permission.granted && ask && permission.canAskAgain) {
          permission = await Notifications.requestPermissionsAsync();
        }
        if (!permission.granted) {
          return null;
        }
        const token = await Notifications.getExpoPushTokenAsync({ projectId: id });
        return { token: token.data, platform: Platform.OS };
      } catch {
        // Expo Go no Android não gera token de push remoto: segue sem push
        return null;
      }
    },
  };
}

/** Sem push (web e testes). */
export function createNoPushTokenSource(): PushTokenSource {
  return { current: () => Promise.resolve(null) };
}
