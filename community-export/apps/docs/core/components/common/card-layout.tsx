import Link from "next/link";
import { Card } from "@heroui/react";

export const CardLayout = () => (
  <div className="mt-8 grid grid-cols-1 gap-8 sm:grid-cols-2">
    <Link href={`/docs/architecture/frontend`}>
      <Card className="p-6">
        <Card.Header>
          <Card.Title>Frontend</Card.Title>
          <Card.Description>Check out how the frontend is setup</Card.Description>
        </Card.Header>
      </Card>
    </Link>
  </div>
);
