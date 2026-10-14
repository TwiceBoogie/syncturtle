/** @vitest-environment jsdom */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Banner } from "@/components/common/banner";

describe("Banner", () => {
  it("renders an error message with error styling", () => {
    render(<Banner type="error" message="The update failed" />);

    expect(screen.getByText("The update failed")).toHaveClass("text-red-600");
  });

  it("renders a success message with success styling", () => {
    render(<Banner type="success" message="The update succeeded" />);

    expect(screen.getByText("The update succeeded")).toHaveClass("text-green-600");
  });
});
