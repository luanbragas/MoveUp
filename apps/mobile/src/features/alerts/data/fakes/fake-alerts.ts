import { ALERT_TYPES, type Alert, type AlertSetting } from "../../domain/alert";
import type { AlertsRepository, PushTokenSource } from "../../domain/ports";

const DEFAULT_THRESHOLD: Partial<Record<Alert["type"], number>> = {
  inactive: 7,
  high_effort: 9,
  low_adherence: 50,
};

/** Central em memória para testes: resolver e adiar mudam o status como no servidor. */
export function createFakeAlerts(seed: readonly Alert[] = []): AlertsRepository & {
  readonly devices: Set<string>;
} {
  let alerts = [...seed];
  let settings: readonly AlertSetting[] = ALERT_TYPES.map((type) => ({
    type,
    enabled: true,
    push: type === "pain_reported" || type === "inactive",
    threshold: DEFAULT_THRESHOLD[type] ?? null,
  }));
  const devices = new Set<string>();
  const set = (id: string, patch: Partial<Alert>) => {
    alerts = alerts.map((a) => (a.id === id ? { ...a, ...patch } : a));
  };
  return {
    devices,
    list(status) {
      const items = alerts.filter((a) => a.status === status);
      return Promise.resolve({
        items,
        next: null,
        openCount: alerts.filter((a) => a.status === "open").length,
      });
    },
    resolve(alertId) {
      set(alertId, { status: "resolved", snoozedUntil: null });
      return Promise.resolve();
    },
    snooze(alertId, days) {
      set(alertId, { status: "snoozed", snoozedUntil: new Date(Date.now() + days * 86_400_000) });
      return Promise.resolve();
    },
    settings: () => Promise.resolve(settings),
    updateSettings(changes) {
      settings = settings.map((s) => changes.find((c) => c.type === s.type) ?? s);
      return Promise.resolve(settings);
    },
    registerDevice(token) {
      devices.add(token);
      return Promise.resolve();
    },
    removeDevice(token) {
      devices.delete(token);
      return Promise.resolve();
    },
  };
}

export function createFakePushTokens(token: string | null = null): PushTokenSource {
  return {
    current: () => Promise.resolve(token === null ? null : { token, platform: "android" }),
  };
}
