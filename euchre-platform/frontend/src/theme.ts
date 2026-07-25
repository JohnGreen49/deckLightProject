import type { GroupTheme } from "./types";

/**
 * Apply a group's theme by setting CSS custom properties on :root. This is the skinning
 * mechanism: every component styles itself from these variables, so a group's colors and logo
 * flow through the whole UI. Unspecified fields keep the default palette from styles.css.
 */
export function applyTheme(themeJson: string | undefined | null) {
  const root = document.documentElement;
  const defaults: Record<keyof GroupTheme, string> = {
    primary: "--color-primary",
    accent: "--color-accent",
    background: "--color-background",
    surface: "--color-surface",
    text: "--color-text",
    logoUrl: "",
  };

  let theme: GroupTheme = {};
  if (themeJson) {
    try {
      theme = JSON.parse(themeJson) as GroupTheme;
    } catch {
      theme = {};
    }
  }

  (Object.keys(defaults) as (keyof GroupTheme)[]).forEach((key) => {
    const cssVar = defaults[key];
    const value = theme[key];
    if (cssVar && value) {
      root.style.setProperty(cssVar, value);
    }
  });
}
