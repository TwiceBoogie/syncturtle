import { FC } from "react";
// heroui
import { Alert, CloseButton } from "@heroui/react";
// helpers
import { TAuthErrorInfo } from "@/helpers/authentication.helper";
// i18n
import { useTranslation } from "@syncturtle/i18n";

interface IAuthBanner {
  bannerData: TAuthErrorInfo | undefined;
  handleBannerData?: (bannerData: TAuthErrorInfo | undefined) => void;
}

export const AuthBanner: FC<IAuthBanner> = (props) => {
  const { bannerData, handleBannerData } = props;
  // translation
  const { t } = useTranslation();

  if (!bannerData) return <></>;

  return (
    <Alert status="danger">
      <Alert.Indicator />
      <Alert.Content>
        <Alert.Title>{bannerData.title}</Alert.Title>
        <Alert.Description>{bannerData.message}</Alert.Description>
      </Alert.Content>
      <CloseButton onPress={() => handleBannerData?.(undefined)} aria-label={t("aria_labels.auth_forms.close_alert")} />
    </Alert>
  );
};
