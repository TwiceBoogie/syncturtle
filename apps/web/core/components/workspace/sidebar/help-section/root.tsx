import { BookOpen, CircleHelp, Keyboard, MessageCircle, Sparkles, UserRound, UsersRound } from "lucide-react";
// heroui
import { Button, Dropdown, Label, Separator } from "@heroui/react";

export const HelpMenuRoot = () => (
  <Dropdown>
    <Button isIconOnly variant="ghost" className="size-[28px] min-w-[28px] rounded-sm p-0">
      <CircleHelp aria-hidden="true" className="size-5" />
    </Button>
    <Dropdown.Popover placement="bottom end" offset={6} className="rounded-lg">
      <Dropdown.Menu aria-label="Help">
        <Dropdown.Item id={"documentation"} textValue="Documentation" className="rounded-lg">
          <BookOpen aria-hidden className="size-4" />
          <Label>Documentation</Label>
        </Dropdown.Item>
        <Dropdown.Item id="support" textValue="Message support" className="rounded-lg">
          <MessageCircle aria-hidden="true" className="size-4" />
          <Label>Message support</Label>
        </Dropdown.Item>
        <Dropdown.Item id="sales" textValue="Contact sales" className="rounded-lg">
          <UserRound aria-hidden="true" className="size-4" />
          <Label>Contact sales</Label>
        </Dropdown.Item>

        <Separator className="my-1 border-custom-border-200" />

        <Dropdown.Item id="shortcuts" textValue="Keyboard shortcuts" className="rounded-lg">
          <Keyboard aria-hidden="true" className="size-4" />
          <Label>Keyboard shortcuts</Label>
        </Dropdown.Item>
        <Dropdown.Item id="updates" textValue="What's new?" className="rounded-lg">
          <Sparkles aria-hidden="true" className="size-4" />
          <Label>What&apos;s new?</Label>
        </Dropdown.Item>
        <Dropdown.Item id="forum" textValue="Forum" className="rounded-lg">
          <UsersRound aria-hidden="true" className="size-4" />
          <Label>Forum</Label>
        </Dropdown.Item>
      </Dropdown.Menu>
      <div className="border-t border-custom-border-200 px-3 py-2 text-[11px] text-custom-text-300">
        Version: Latest
      </div>
    </Dropdown.Popover>
  </Dropdown>
);
