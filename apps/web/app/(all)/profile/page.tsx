"use client";

import { PageHead } from "@/components/core";
import { ProfileForm } from "@/components/profile/form";
import { ProfileSettingContentWrapper } from "@/components/profile/profile-setting-content-wrapper";
import { useUser } from "@/hooks/store/user";
import { useTranslation } from "@syncturtle/i18n";
import type { IUser } from "@syncturtle/types";

export const mockUser: IUser = {
  id: "usr_01JZKJQ7YQ8A6X8H4J2M9R1N3F",
  displayName: "Sebastian Garcia",
  username: "sebastian",
  firstName: "Sebastian",
  lastName: "Garcia",
  email: "sebastian@example.com",
  mobileNumber: "+1 (555) 123-4567",

  avatarUrl: "https://i.pravatar.cc/300?img=12",
  avatarAssetId: "asset_avatar_01JZKK3XQW6QJ8D1M9Y7H5P2A",
  coverImageUrl: "https://picsum.photos/1600/400",
  coverImage: null,
  coverImageAsset: null,

  isBot: false,
  isActive: true,
  isEmailVerified: true,
  isPasswordAutoset: false,
  isTourCompleted: true,

  joining_date: "2026-01-15T09:30:00Z",
  userTimezone: "America/Chicago",
  lastLoginMedium: "email", // Adjust to your TLoginMediums type

  lastWorkspaceId: "ws_01JZKK8D7M3XJ2P5N8Q4R6T1Y",
  theme: {
    text: undefined,
    theme: undefined,
    palette: undefined,
    primary: undefined,
    background: undefined,
    darkPalette: undefined,
    sidebarText: undefined,
    sidebarBackground: undefined,
  },
};

export default function ProfileSettingsPage() {
  // hooks
  const { t } = useTranslation();
  const { data: currentUser, userProfile } = useUser();

  const user = currentUser ?? mockUser;
  return (
    <>
      <PageHead title={`${t("profile.label")} - ${t("general_settings")}`} />
      <ProfileSettingContentWrapper>
        <ProfileForm user={user} profile={userProfile.data} />
      </ProfileSettingContentWrapper>
    </>
  );
}
