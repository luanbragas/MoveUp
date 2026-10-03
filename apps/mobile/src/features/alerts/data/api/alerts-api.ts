import {
  getAlertSettings,
  listAlerts,
  registerPushDevice,
  removePushDevice,
  resolveAlert,
  schemas,
  snoozeAlert,
  updateAlertSettings,
} from "@moveup/api-client";
import { z } from "zod";
import { callApi } from "../../../../shared/lib/http";
import type { Alert, AlertSetting } from "../../domain/alert";
import type { AlertsRepository } from "../../domain/ports";

const NoContent = z.undefined();
const PAGE_SIZE = 30;

type AlertDto = z.infer<typeof schemas.ListAlerts200Response>["items"][number];
type SettingDto = z.infer<typeof schemas.GetAlertSettings200ResponseItem>;

function toAlert(dto: AlertDto): Alert {
  return {
    id: dto.id,
    type: dto.type,
    severity: dto.severity,
    status: dto.status,
    clientId: dto.clientId,
    linkId: dto.linkId ?? null,
    clientName: dto.clientName ?? null,
    facts: dto.facts,
    createdAt: new Date(dto.createdAt),
    snoozedUntil: dto.snoozedUntil == null ? null : new Date(dto.snoozedUntil),
  };
}

function toSetting(dto: SettingDto): AlertSetting {
  return {
    type: dto.type,
    enabled: dto.enabled,
    push: dto.push,
    threshold: dto.threshold ?? null,
  };
}

export function createAlertsApiRepository(): AlertsRepository {
  return {
    async list(status, before) {
      const page = await callApi(
        () =>
          listAlerts(
            before === null ? { status, limit: PAGE_SIZE } : { status, before, limit: PAGE_SIZE },
          ),
        schemas.ListAlerts200Response,
      );
      return { items: page.items.map(toAlert), next: page.next ?? null, openCount: page.openCount };
    },
    async resolve(alertId) {
      await callApi(() => resolveAlert(alertId), NoContent);
    },
    async snooze(alertId, days) {
      await callApi(() => snoozeAlert(alertId, { days }), NoContent);
    },
    async settings() {
      const list = await callApi(() => getAlertSettings(), schemas.GetAlertSettings200Response);
      return list.map(toSetting);
    },
    async updateSettings(changes) {
      const list = await callApi(
        () =>
          updateAlertSettings({
            settings: changes.map((s) =>
              s.threshold === null
                ? { type: s.type, enabled: s.enabled, push: s.push }
                : { type: s.type, enabled: s.enabled, push: s.push, threshold: s.threshold },
            ),
          }),
        schemas.UpdateAlertSettings200Response,
      );
      return list.map(toSetting);
    },
    async registerDevice(token, platform) {
      await callApi(() => registerPushDevice({ token, platform }), NoContent);
    },
    async removeDevice(token) {
      await callApi(() => removePushDevice({ token }), NoContent);
    },
  };
}
