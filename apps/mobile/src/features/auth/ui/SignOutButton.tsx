import { router } from "expo-router";
import { Button } from "../../../shared/ui/Button";
import { useSignOut } from "../hooks/use-auth-actions";
import { strings } from "./strings";

export function SignOutButton() {
  const signOut = useSignOut();
  return (
    <Button
      label={strings.entry.signOut}
      variant="secondary"
      loading={signOut.isPending}
      onPress={() => {
        signOut.mutate(undefined, {
          onSuccess: () => {
            router.replace("/");
          },
        });
      }}
    />
  );
}
