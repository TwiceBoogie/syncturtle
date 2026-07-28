export const checkEmailValidity = (email: string): boolean => {
  if (!email) return false;

  const isEmailValid =
    /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/.test(
      email
    );

  return isEmailValid;
};

export const checkURLValidity = (url: string): boolean => {
  if (!url) return false;

  // regex to support complex query parameters and fragments
  const urlPattern =
    /^(https?:\/\/)?((([a-z\d-]+\.)*[a-z\d-]+\.[a-z]{2,6})|(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}))(:\d+)?(\/[\w.-]*)*(\?[^#\s]*)?(#[\w-]*)?$/i;

  return urlPattern.test(url);
};

/**
 * @description Truncates text to specified length and adds ellipsis
 * @param {string} str - String to truncate
 * @param {number} length - Maximum length before truncation
 * @returns {string} Truncated string with ellipsis if needed
 * @example
 * truncateText("This is a long text", 7) // returns "This is..."
 */
export const truncateText = (str: string, length: number) => {
  if (!str || str === "") return "";

  return str.length > length ? `${str.substring(0, length)}...` : str;
};
