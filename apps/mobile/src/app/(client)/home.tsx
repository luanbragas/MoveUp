import { SignOutButton } from "../../features/auth";
import { ClientHomeScreen } from "../../features/invite";

export default function ClientHome() {
  return <ClientHomeScreen footer={<SignOutButton />} />;
}
