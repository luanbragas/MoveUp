import NetInfo from "@react-native-community/netinfo";
import { QueryClient, focusManager, onlineManager } from "@tanstack/react-query";
import { AppState, Platform } from "react-native";
import { isClientError } from "../shared/lib/http";

const MAX_RETRIES = 3;

/** Padrões do TanStack Query no mobile (FRONTEND-PATTERN, seção 8). */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        networkMode: "offlineFirst",
        // 4xx não melhora repetindo; rede e 5xx sim
        retry: (failureCount, error) => !isClientError(error) && failureCount < MAX_RETRIES,
      },
      mutations: { networkMode: "offlineFirst", retry: false },
    },
  });
}

let connected = false;

/** Liga o onlineManager ao NetInfo e o focusManager ao AppState. Idempotente. */
export function connectQueryToDevice(): void {
  if (connected) {
    return;
  }
  connected = true;
  onlineManager.setEventListener((setOnline) =>
    NetInfo.addEventListener((state) => {
      setOnline(state.isConnected === true);
    }),
  );
  if (Platform.OS !== "web") {
    focusManager.setEventListener((setFocused) => {
      const subscription = AppState.addEventListener("change", (status) => {
        setFocused(status === "active");
      });
      return () => {
        subscription.remove();
      };
    });
  }
}
