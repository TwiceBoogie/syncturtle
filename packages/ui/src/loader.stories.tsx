import type { Meta, StoryObj } from "@storybook/react-vite";
import { Loader } from "./loader";

const meta = {
  title: "Components/Loader",
  component: Loader,
  tags: ["autodocs"],
} satisfies Meta<typeof Loader>;

export default meta;

type Story = StoryObj<typeof meta>;

export const CardSkeleton: Story = {
  render: () => (
    <Loader className="border-custom-border-200 w-80 space-y-3 rounded-lg border p-4">
      <Loader.Item height="1rem" width="60%" />
      <Loader.Item height="0.75rem" width="100%" />
      <Loader.Item height="0.75rem" width="90%" />
      <Loader.Item height="2.5rem" width="100%" className="mt-4" />
    </Loader>
  ),
};
