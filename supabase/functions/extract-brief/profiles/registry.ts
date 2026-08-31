import { GENERAL_PROFILE } from "./general.ts";
import { RELATIONSHIP_PROFILE } from "./relationship.ts";
import { STUB_PROFILE } from "./stub.ts";
import type { ExtractionProfile } from "./types.ts";

const PROFILES: Record<string, ExtractionProfile> = {
  general: GENERAL_PROFILE,
  relationship: RELATIONSHIP_PROFILE,
  stub: STUB_PROFILE,
};

export function resolveProfile(name: string): ExtractionProfile {
  return PROFILES[name] ?? GENERAL_PROFILE;
}
