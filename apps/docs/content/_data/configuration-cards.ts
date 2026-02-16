export type IConfigCard = {
  icon?: React.ReactNode;
  href: string;
  title: string;
  description: string;
};

export const configurationCards: IConfigCard[] = [
  {
    href: "/docs/getting-started/configuration/instance-admin",
    title: "Instance Admin and God Mode",
    description:
      "Configure instance-wide settings, manage users, and access God Mode for advanced administrative controls.",
  },
  {
    href: "/docs/getting-started/configuration/authentication",
    title: "Authentication",
    description:
      "Set up SSO, OAuth, LDAP, or other authentication methods. Support for Google, Github, GitLab, and custom providers.",
  },
  {
    href: "/docs/getting-started/configuration/communication",
    title: "Email and communication",
    description:
      "Configure SMTP for email notifications, invitations, and alerts. Integrate with SendGrid, AWS SES, or your own mail server.",
  },
  {
    href: "/docs/getting-started/configuration/database-and-storage",
    title: "External services",
    description:
      "Connect to managed databases (PostgreSQL, Redis) and cloud storage (S3, MinIO, GCS) for scalable, production-ready deployments.",
  },
];
