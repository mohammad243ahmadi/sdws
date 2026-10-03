import { AdminSection } from "@/components/admin-section";

export default async function AdminRoute({ params }: { params: Promise<{ section: string }> }) {
  const { section } = await params;
  return <AdminSection section={section} />;
}