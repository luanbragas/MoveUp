import type { AlertPage, AlertSetting, AlertStatus } from "./alert";

/** Central de atenção e push (backend: módulo alerts). */
export interface AlertsRepository {
  list(status: AlertStatus, before: string | null): Promise<AlertPage>;
  resolve(alertId: string): Promise<void>;
  snooze(alertId: string, days: number): Promise<void>;
  settings(): Promise<readonly AlertSetting[]>;
  updateSettings(changes: readonly AlertSetting[]): Promise<readonly AlertSetting[]>;
  registerDevice(token: string, platform: "ios" | "android"): Promise<void>;
  removeDevice(token: string): Promise<void>;
}

/**
 * Token do Expo Push deste aparelho. Nulo sem permissão, no Expo Go (push remoto só em build) ou
 * sem projeto EAS configurado: o app segue sem push.
 */
export interface PushTokenSource {
  /** @param ask pede permissão se ainda não pediu */
  current(ask: boolean): Promise<{ token: string; platform: "ios" | "android" } | null>;
}
