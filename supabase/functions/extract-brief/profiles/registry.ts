import { GENERAL_PROFILE } from "./general.ts";
import { STUB_PROFILE } from "./stub.ts";
import type { ExtractionProfile } from "./types.ts";

const PROFILES: Record<string, ExtractionProfile> = {
  general: GENERAL_PROFILE,
  stub: STUB_PROFILE,
};

export function resolveProfile(name: string): ExtractionProfile {
  return PROFILES[name] ?? GENERAL_PROFILE;
}
