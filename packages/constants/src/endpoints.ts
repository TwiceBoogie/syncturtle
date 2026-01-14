export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "";
export const API_BASE_PATH = process.env.NEXT_PUBLIC_API_BASE_PATH || "";
export const API_URL = encodeURI(`${API_BASE_URL}${API_BASE_PATH}`);
// god mode admin app base url
export const ADMIN_BASE_URL = process.env.NEXT_PUBLIC_ADMIN_BASE_URL || "";
export const ADMIN_BASE_PATH = process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || "";
export const GOD_MODE_URL = encodeURI(`${ADMIN_BASE_URL}${ADMIN_BASE_PATH}`);
// web app base url
export const WEB_BASE_URL = process.env.NEXT_PUBLIC_WEB_BASE_URL || "";
export const WEB_BASE_PATH = process.env.NEXT_PUBLIC_WEB_BASE_PATH || "";
export const WEB_URL = encodeURI(`${WEB_BASE_URL}${WEB_BASE_PATH}`);
// syncturtle website url
export const WEBSITE_URL = process.env.NEXT_PUBLIC_WEBSITE_URL || "https://syncturtle.com";
// support email
export const SUPPORT_EMAIL = process.env.NEXT_PUBLIC_SUPPORT_EMAIL || "support@syncturtle.com";
// marketing links
export const MARKETING_PRICING_PAGE_LINK = "https://syncturtle.com/pricing";
export const MARKETING_CONTACT_US_PAGE_LINK = "https://syncturtle.com/contact";
