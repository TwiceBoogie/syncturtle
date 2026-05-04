import type { MetaRecord } from "nextra";

type MetaValue = MetaRecord[string];

type MetaGlobalRecord = Record<string, MetaValue | (MetaValue & { items: MetaGlobalRecord })>;

const ARCHITECTURE: MetaRecord = {
  frontend: "Frontend",
  backend: "Backend",
};

const meta: MetaGlobalRecord = {
  // root
  index: {
    title: "Home",
    display: "hidden",
    theme: {
      sidebar: false,
      toc: false,
      breadcrumb: false,
      pagination: false,
    },
  },
  // top navbar item
  docs: {
    title: "Documentation",
    type: "page",
    items: {
      index: {},
      "getting-started": {
        title: "Getting Started",
      },
      architecture: {
        items: ARCHITECTURE,
      },
    },
  },
  showcase: {
    title: "Showcase",
    type: "page",
    href: "https://syncturtle.com",
  },
};

export default meta;
