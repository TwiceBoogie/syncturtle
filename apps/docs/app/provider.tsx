import type { FC } from "react";
import { ThemeProvider } from "next-themes";

interface IAppProvider {
  children: React.ReactNode;
}

export const AppProvider: FC<IAppProvider> = (props) => {
  const { children } = props;

  return (
    <ThemeProvider themes={["light", "dark"]} defaultTheme="system" enableSystem>
      {children}
    </ThemeProvider>
  );
};
