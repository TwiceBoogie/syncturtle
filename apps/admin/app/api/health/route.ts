export const dynamic = "force-dynamic";

export function GET() {
  return Response.json({ status: "UP", application: "admin" }, { headers: { "Cache-Control": "no-store" } });
}
