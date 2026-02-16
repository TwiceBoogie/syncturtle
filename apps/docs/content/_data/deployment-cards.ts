export type TDocCard = {
  href: string;
  title: string;
  description: string;
};

export const deploymentCards: TDocCard[] = [
  {
    href: "/docs/getting-started/installation/docker",
    title: "Docker Compose",
    description: "Quick setup with minimal configuration",
  },
  {
    href: "/docs/getting-started/installation/kubernetes",
    title: "Kubernetes",
    description: "Production-ready deployment for clusters",
  },
  {
    href: "/docs/getting-started/installation/bare-metal",
    title: "Bare Metal",
    description: "Install directly on a server or VM",
  },
];
