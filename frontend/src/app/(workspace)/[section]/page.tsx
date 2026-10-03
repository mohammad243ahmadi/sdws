import { WorkspaceSection } from "@/components/workspace-section";

export default async function WorkspaceRoute({ params }: { params: Promise<{ section: string }> }) {
  const { section } = await params;
  return <WorkspaceSection section={section} />;
}