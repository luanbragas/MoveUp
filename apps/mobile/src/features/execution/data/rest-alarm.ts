import * as Notifications from "expo-notifications";
import { Platform } from "react-native";
import type { RestAlarm } from "../domain/ports";

// Texto fixo e genérico: notificação aparece na tela bloqueada (nada de treino ou saúde aqui).
const TITLE = "Descanso terminou";
const BODY = "Bora para a próxima série.";
const CHANNEL_ID = "rest";

async function allowed(): Promise<boolean> {
  if (Platform.OS === "android") {
    // Android 13+: o pedido de permissão só aparece depois que existe um canal
    await Notifications.setNotificationChannelAsync(CHANNEL_ID, {
      name: "Descanso",
      importance: Notifications.AndroidImportance.HIGH,
      vibrationPattern: [0, 300, 150, 300],
    });
  }
  const current = await Notifications.getPermissionsAsync();
  if (current.granted) {
    return true;
  }
  if (!current.canAskAgain) {
    return false;
  }
  const asked = await Notifications.requestPermissionsAsync();
  return asked.granted;
}

/** Notificação local agendada pelo horário do fim do descanso. */
export function createRestAlarm(): RestAlarm {
  return {
    async schedule(endsAt) {
      const seconds = Math.round((endsAt - Date.now()) / 1000);
      if (seconds < 1) {
        return null;
      }
      try {
        if (!(await allowed())) {
          return null;
        }
        return await Notifications.scheduleNotificationAsync({
          content: { title: TITLE, body: BODY, sound: true },
          trigger: {
            type: Notifications.SchedulableTriggerInputTypes.TIME_INTERVAL,
            seconds,
            channelId: CHANNEL_ID,
          },
        });
      } catch {
        // aviso é conforto: o timer na tela continua valendo
        return null;
      }
    },
    async cancel(id) {
      await Notifications.cancelScheduledNotificationAsync(id).catch(() => undefined);
    },
  };
}

/** Sem notificação (web e testes). */
export function createNoRestAlarm(): RestAlarm {
  return {
    schedule: () => Promise.resolve(null),
    cancel: () => Promise.resolve(),
  };
}
