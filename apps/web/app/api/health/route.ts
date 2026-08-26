export const dynamic = "force-dynamic";

export function GET() {
  return Response.json({ status: "UP", application: "web" }, { headers: { "Cache-Control": "no-store" } });
}
