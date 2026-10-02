import { SignOutButton } from "../../features/auth";
import { ClientsScreen } from "../../features/clients";

export default function ProfessionalDashboard() {
  return <ClientsScreen footer={<SignOutButton />} />;
}
