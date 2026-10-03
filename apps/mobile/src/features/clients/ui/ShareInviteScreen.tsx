import * as Clipboard from "expo-clipboard";
import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Linking, Share, StyleSheet, Text, View } from "react-native";
import QRCode from "react-native-qrcode-svg";
import { Button } from "../../../shared/ui/Button";
import { Message } from "../../../shared/ui/Message";
import { RoundButton } from "../../../shared/ui/RoundButton";
import { Screen } from "../../../shared/ui/Screen";
import { fonts, palette, radius, spacing, typography } from "../../../shared/ui/theme";
import { inviteMessage, whatsappLink } from "../domain/client";
import { useProfessionalFirstName } from "../hooks/use-professional-name";
import { parseShareParams } from "./share-params";
import { strings } from "./strings";

const t = strings.share;
const QR_SIZE = 168;
const dayFormat = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

/** Convite criado: código grande, QR Code, WhatsApp e compartilhar (SCREEN-FLOWS 2.2). */
export function ShareInviteScreen() {
  const params = parseShareParams(useLocalSearchParams());
  const professional = useProfessionalFirstName();
  const [whatsappFailed, setWhatsappFailed] = useState(false);
  const [copied, setCopied] = useState(false);

  const close = () => {
    router.dismissTo("/clients");
  };
  const header = <RoundButton icon="close" label={t.close} onPress={close} />;

  if (params === null) {
    return (
      <Screen header={header} title={t.title}>
        <Message text={t.invalid} />
      </Screen>
    );
  }

  const message = inviteMessage(professional, params);
  const whatsapp = params.phone === null ? null : whatsappLink(params.phone, message);
  const share = () => {
    // cancelar o compartilhamento não é erro
    Share.share({ message }).catch(() => undefined);
  };

  return (
    <Screen
      header={header}
      title={t.title}
      footer={
        <>
          {whatsapp === null ? null : (
            <Button
              label={t.whatsapp}
              icon="chat"
              onPress={() => {
                Linking.openURL(whatsapp).catch(() => {
                  setWhatsappFailed(true);
                });
              }}
            />
          )}
          <View style={styles.row}>
            <View style={styles.half}>
              <Button
                label={copied ? t.copied : t.copy}
                variant="secondary"
                icon={copied ? "check" : "copy"}
                onPress={() => {
                  void Clipboard.setStringAsync(params.url).then(() => {
                    setCopied(true);
                  });
                }}
              />
            </View>
            <View style={styles.half}>
              <Button
                label={t.shareOther}
                variant={whatsapp === null ? "primary" : "secondary"}
                icon="share"
                onPress={share}
              />
            </View>
          </View>
        </>
      }
    >
      <View style={styles.card}>
        {params.name === null ? null : (
          <Text style={[typography.label, { color: palette.textSoft }]}>{params.name}</Text>
        )}
        <Text
          accessibilityLabel={`${t.code} ${params.code.split("").join(" ")}`}
          selectable
          style={styles.code}
        >
          {`${params.code.slice(0, 4)} ${params.code.slice(4)}`}
        </Text>
        <Text style={[typography.small, { color: palette.muted }]}>
          {t.expires(dayFormat.format(params.expiresAt))}
        </Text>
        <View accessible accessibilityRole="image" accessibilityLabel={t.qrLabel} style={styles.qr}>
          {/* QR sempre preto no branco: leitores exigem contraste */}
          <QRCode value={params.url} size={QR_SIZE} color="#000000" backgroundColor="#FFFFFF" />
        </View>
        <Text style={[typography.small, styles.hint]}>{t.qrHint}</Text>
      </View>
      {whatsappFailed ? <Message text={t.whatsappFailed} /> : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: palette.surface,
    borderRadius: radius.xl,
    padding: spacing.lg,
    alignItems: "center",
    gap: spacing.sm + 2,
  },
  code: {
    fontFamily: fonts.number,
    fontSize: 40,
    letterSpacing: 4,
    color: palette.lime,
    fontVariant: ["tabular-nums"],
  },
  qr: {
    backgroundColor: "#FFFFFF",
    padding: spacing.md - 4,
    borderRadius: radius.md,
    marginTop: spacing.sm,
  },
  hint: { color: palette.muted, textAlign: "center" },
  row: { flexDirection: "row", gap: spacing.sm },
  half: { flex: 1 },
});
