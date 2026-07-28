import type { FC } from "react";
import Link from "next/link";
// heroui
import { Card } from "@heroui/react";

export type TDocCard = {
  href: string;
  title: string;
  description: string;
};

export interface IDocCards {
  items: TDocCard[];
}
export const DocsCards: FC<IDocCards> = (props) => {
  const { items } = props;

  return (
    <>
      {items.map((item) => (
        <Link key={item.href} href={item.href}>
          <Card className="p-6 hover:bg-tertiary">
            <Card.Header>
              <Card.Title className="text-lg">{item.title}</Card.Title>
              <Card.Description className="text-muted">{item.description}</Card.Description>
            </Card.Header>
          </Card>
        </Link>
      ))}
    </>
  );
};
