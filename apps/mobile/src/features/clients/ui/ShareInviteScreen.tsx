import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { Linking, Share, StyleSheet, Text, View } from "react-native";
import QRCode from "react-native-qrcode-svg";
import { Button } from "../../../shared/ui/Button";
import { Message } from "../../../shared/ui/Message";
import { Screen } from "../../../shared/ui/Screen";
import { spacing, typography, useColors } from "../../../shared/ui/theme";
import { inviteMessage, whatsappLink } from "../domain/client";
import { useProfessionalFirstName } from "../hooks/use-professional-name";
import { parseShareParams } from "./share-params";
import { strings } from "./strings";

const t = strings.share;
const QR_SIZE = 200;

/** Convite criado: código grande, QR Code, WhatsApp e compartilhar (SCREEN-FLOWS 2.2). */
export function ShareInviteScreen() {
  const colors = useColors();
  const params = parseShareParams(useLocalSearchParams());
  const professional = useProfessionalFirstName();
  const [whatsappFailed, setWhatsappFailed] = useState(false);

  const done = () => {
    router.dismissTo("/dashboard");
  };

  if (params === null) {
    return (
      <Screen title={t.title}>
        <Message text={t.invalid} />
        <Button label={t.done} onPress={done} />
      </Screen>
    );
  }

  const message = inviteMessage(professional, params);
  const whatsapp = params.phone === null ? null : whatsappLink(params.phone, message);

  return (
    <Screen title={t.title} subtitle={t.subtitle(params.name)}>
      <View style={styles.center}>
        <Text style={[typography.label, { color: colors.textMuted }]}>{t.code}</Text>
        <Text
          accessibilityLabel={`${t.code} ${params.code.split("").join(" ")}`}
          selectable
          style={[styles.code, { color: colors.text }]}
        >
          {params.code}
        </Text>
        <View
          accessible
          accessibilityRole="image"
          accessibilityLabel={t.qrLabel}
          style={[styles.qr, { backgroundColor: "#FFFFFF" }]}
        >
          {/* QR sempre preto no branco, inclusive no tema escuro: leitores exigem contraste */}
          <QRCode value={params.url} size={QR_SIZE} color="#000000" backgroundColor="#FFFFFF" />
        </View>
        <Text style={[typography.small, { color: colors.textMuted }]}>
          {t.expires(params.expiresAt.toLocaleDateString("pt-BR"))}
        </Text>
      </View>
      {whatsappFailed ? <Message text={t.whatsappFailed} /> : null}
      {whatsapp === null ? null : (
        <Button
          label={t.whatsapp}
          onPress={() => {
            Linking.openURL(whatsapp).catch(() => {
              setWhatsappFailed(true);
            });
          }}
        />
      )}
      <Button
        label={t.shareOther}
        variant={whatsapp === null ? "primary" : "secondary"}
        onPress={() => {
          // cancelar o compartilhamento não é erro
          Share.share({ message }).catch(() => undefined);
        }}
      />
      <Button label={t.done} variant="secondary" onPress={done} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { alignItems: "center", gap: spacing.md },
  code: { fontSize: 36, fontWeight: "700", letterSpacing: 4, fontVariant: ["tabular-nums"] },
  qr: { padding: spacing.md, borderRadius: 12 },
});
