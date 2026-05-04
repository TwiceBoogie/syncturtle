import coreEn from "../locales/en/core.json";
import { Listener, Unsubscribe } from "@syncturtle/types";
import { FALLBACK_LANGUAGE, STORAGE_KEY, SUPPORTED_LANGUAGES } from "../constants";
import { ILanguageOption, ITranslation, TLanguage } from "../types";
import { Emitter } from "@syncturtle/utils";
import IntlMessageFormat, { PrimitiveType } from "intl-messageformat";
import { get, merge } from "lodash";

export type TTranslationSnapshot = {
  currentLocale: TLanguage;
  isLoading: boolean;
  isInitialized: boolean;
  // bump when translations for any lang changes
  translationVersion: number;
};

const initialSnapshot: TTranslationSnapshot = {
  currentLocale: FALLBACK_LANGUAGE,
  isLoading: true,
  isInitialized: false,
  translationVersion: 0,
};

type LocaleMap<T> = Partial<Record<TLanguage, T>>;
type FlatLocaleMap = Record<string, string>;
export type MessageValues = Record<string, PrimitiveType>;

export interface ITranslationStoreInternal {
  _subscribe(listener: Listener): Unsubscribe;
  _getSnapshot(): TTranslationSnapshot;
  _getServerSnapshot(): TTranslationSnapshot;
}

export type TTranslationStore = Omit<TranslationStore, keyof ITranslationStoreInternal>;

/**
 * External store for translations.
 */
export class TranslationStore implements ITranslationStoreInternal {
  private emitter = new Emitter();
  private _snap: TTranslationSnapshot = initialSnapshot;

  // full nested translations by locale (core + feature)
  private translations: LocaleMap<ITranslation> = {};
  private coreTranslation: LocaleMap<ITranslation> = {
    en: coreEn as ITranslation,
  };
  // flattened key -> message map for fast lookup: "auth.common.email.label" => "Email"
  private flatTranslations: LocaleMap<FlatLocaleMap> = {};
  private messageCache: Map<string, IntlMessageFormat> = new Map();
  private loadedLanguages: Set<TLanguage> = new Set();

  // useSyncExternalStore integration
  /** @internal */
  public _subscribe = (listener: Listener): Unsubscribe => this.emitter.subscribe(listener);
  /** @internal */
  public _getSnapshot = (): TTranslationSnapshot => this._snap;
  /** @internal */
  public _getServerSnapshot = (): TTranslationSnapshot => this._snap;

  // raw getters for data
  get currentLocale(): TLanguage {
    return this._snap.currentLocale;
  }

  get isLoading(): boolean {
    return this._snap.isLoading;
  }

  get isInitialized(): boolean {
    return this._snap.isInitialized;
  }

  get availableLanguages(): ILanguageOption[] {
    return SUPPORTED_LANGUAGES;
  }

  // Lifecycle
  public async init(): Promise<void> {
    await this.bootstrap();
  }

  public dispose(): void {}

  // Actions
  public changeLanguage = async (lng: TLanguage): Promise<void> => {
    if (!this.isValidLanguage(lng)) {
      console.warn("[i18n] ignoring invalid language: ", lng);
      return;
    }
    this.set({ isLoading: true });

    if (!this.loadedLanguages.has(lng)) {
      await this.loadLanguageTranslations(lng);
    }

    if (typeof window !== "undefined") {
      localStorage.setItem(STORAGE_KEY, lng);
      document.documentElement.lang = lng;
    }

    this.set({ currentLocale: lng, isLoading: false });
  };

  public t = (key: string, params?: MessageValues): string => {
    try {
      const { currentLocale } = this._snap;

      let formatter = this.getMessageInstance(key, currentLocale);

      if (!formatter && currentLocale !== FALLBACK_LANGUAGE) {
        formatter = this.getMessageInstance(key, FALLBACK_LANGUAGE);
      }

      if (!formatter) {
        // last resort: raw string or key
        return this.getRawMessage(key, currentLocale) ?? this.getRawMessage(key, FALLBACK_LANGUAGE) ?? key;
      }
      return String(formatter.format(params || {}));
    } catch (error) {
      console.error(`[i18n] translation error for key "${key}`, error);
      return key;
    }
  };

  private async bootstrap(): Promise<void> {
    try {
      const initialLocale = this.detectInitialLocale();

      this.set({ currentLocale: initialLocale });

      // load current + fallback in parallel
      await this.loadPrimaryLanguages(initialLocale);
    } catch (error) {
      console.error("[i18n] bootstrap error", error);
    } finally {
      this.set({ isLoading: false, isInitialized: true });
    }

    // fire & forgot remaining locales
    this.loadRemainingLanguages();
  }

  private detectInitialLocale(): TLanguage {
    if (typeof window === "undefined") {
      // SSR -> FALLBACK, client -> corrects after mount
      return FALLBACK_LANGUAGE;
    }

    const savedLocale = localStorage.getItem(STORAGE_KEY) as TLanguage | null;
    if (this.isValidLanguage(savedLocale)) return savedLocale;

    return this.getBrowserLanguage();
  }

  private getBrowserLanguage(): TLanguage {
    if (typeof navigator === "undefined") return FALLBACK_LANGUAGE;

    const browserLang = navigator.language.toLowerCase();
    const exact = SUPPORTED_LANGUAGES.find((l) => l.value.toLowerCase() === browserLang);
    if (exact) return exact.value;

    const base = browserLang.split("-")[0];
    const baseMatch = SUPPORTED_LANGUAGES.find((l) => l.value.toLowerCase() === base);
    if (baseMatch) return baseMatch.value;

    const similar = SUPPORTED_LANGUAGES.find(
      (l) => browserLang.includes(l.value.toLowerCase()) || l.value.toLowerCase().includes(browserLang)
    );

    return similar?.value ?? FALLBACK_LANGUAGE;
  }

  private async loadPrimaryLanguages(initialLocale: TLanguage): Promise<void> {
    const toLoad = new Set<TLanguage>([initialLocale, FALLBACK_LANGUAGE]);
    await Promise.all([...toLoad].map((lng) => this.loadLanguageTranslations(lng)));
  }

  private loadRemainingLanguages(): void {
    const rest = SUPPORTED_LANGUAGES.map((l) => l.value).filter(
      (lng) => !this.loadedLanguages.has(lng) && lng != FALLBACK_LANGUAGE
    );

    void Promise.all(rest.map((lng) => this.loadLanguageTranslations(lng))).catch((e) => {
      console.error("[i18n] failed to load some languages", e);
    });
  }

  private async loadLanguageTranslations(language: TLanguage): Promise<void> {
    if (this.loadedLanguages.has(language)) return;

    try {
      const translationsModule = await this.importLanguageFile(language);
      const raw = translationsModule.default as ITranslation;

      const merged = merge({}, this.coreTranslation[language] || {}, raw);

      this.translations[language] = merged;
      this.flatTranslations[language] = this.flattenTranslations(merged);

      this.loadedLanguages.add(language);

      this.invalidateLanguageCache(language);

      this.set({ translationVersion: this._snap.translationVersion + 1 });
    } catch (error) {
      console.error(`[i18n] failed to load language '${language}'`, error);
    }
  }

  private importLanguageFile(language: TLanguage): Promise<{ default: ITranslation }> {
    switch (language) {
      case "en":
        return import("../locales/en/translation.json");
      case "es":
        return import("../locales/es/translation.json");
      default:
        return Promise.reject(new Error(`Unsupported language: ${language}`));
    }
  }

  private flattenTranslations(obj: ITranslation, prefix = ""): FlatLocaleMap {
    const out: FlatLocaleMap = {};

    for (const [key, value] of Object.entries(obj)) {
      const fullKey = prefix ? `${prefix}.${key}` : key;
      if (value && typeof value === "object") {
        Object.assign(out, this.flattenTranslations(value as ITranslation, fullKey));
      } else {
        out[fullKey] = String(value);
      }
    }

    return out;
  }

  private invalidateLanguageCache(_language: TLanguage): void {
    this.messageCache.clear();
  }

  private getRawMessage(key: string, locale: TLanguage): string | undefined {
    const flat = this.flatTranslations[locale];
    if (flat) return flat[key];

    // fallback: nested get if flat map missing
    const nested = this.translations[locale];
    if (!nested) return undefined;

    const value = get(nested, key);
    return typeof value === "string" ? value : undefined;
  }

  private getMessageInstance(key: string, locale: TLanguage): IntlMessageFormat | null {
    const cacheKey = `${locale}:${key}`;
    const cached = this.messageCache.get(cacheKey);
    if (cached) return cached;

    const message = this.getRawMessage(key, locale);
    if (!message) return null;

    try {
      const formatter = new IntlMessageFormat(message, locale);
      this.messageCache.set(cacheKey, formatter);
      return formatter;
    } catch (error) {
      console.error(`[i18n] failed to create formatter for "${key}"`, error);
      return null;
    }
  }

  private isValidLanguage(lang: string | null): lang is TLanguage {
    return !!lang && SUPPORTED_LANGUAGES.some((l) => l.value === lang);
  }

  private set(patch: Partial<TTranslationSnapshot>): void {
    const prev = this._snap;
    const next = { ...prev, ...patch };

    let changed = false;
    for (const key in next) {
      const k = key as keyof TTranslationSnapshot;
      if (!Object.is(prev[k], next[k])) {
        changed = true;
        break;
      }
    }

    if (!changed) return;

    this._snap = next;
    this.emitter.emit();
  }
}
