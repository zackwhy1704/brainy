#!/usr/bin/env python3
"""
Reliability guard for fact-diffing (same vs. changed), run against the REAL pipeline:
items REST insert -> Postgres trigger -> extract-brief Edge Function -> facts/fact_sources.

Same/changed is an LLM judgment, so this is a pinned, repeatable measurement, not a unit test:
each pair runs N times and the script reports pass counts per pair. A false "Changed" is the
failure that matters most (it is worse than no change detection at all), so pairs (a), (b) and
(d) are the ones to watch.

Pairs (all in the `location` category, one person each):
  a) verbatim identical statement          -> expect SAME  (1 current fact, corroborated once)
  b) reworded, semantically identical      -> expect SAME  (1 current fact, corroborated once)
  c) genuinely changed value               -> expect CHANGED (old superseded, new current)
  d) same category, different person       -> expect A untouched, B inserted (no cross-person link)

Usage:
  SB_ACCESS_TOKEN=<user jwt> python3 supabase/tests/fact_diff_guard.py [--runs 5]
Reads SUPABASE_URL / SUPABASE_ANON_KEY from local.properties. Test content is prefixed
GUARDTEST so it can be deleted afterwards; the script does not delete (cleanup is a separate,
deliberate step).
"""
import argparse
import base64
import json
import os
import random
import string
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timezone

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def local_prop(key: str) -> str:
    with open(os.path.join(ROOT, "local.properties")) as f:
        for line in f:
            if line.startswith(key + "="):
                return line.strip().split("=", 1)[1]
    sys.exit(f"{key} not in local.properties")


URL = local_prop("SUPABASE_URL")
ANON = local_prop("SUPABASE_ANON_KEY")
TOKEN = os.environ.get("SB_ACCESS_TOKEN") or sys.exit("SB_ACCESS_TOKEN env var required")
USER_ID = json.loads(base64.urlsafe_b64decode(TOKEN.split(".")[1] + "==="))["sub"]
HEADERS = {"apikey": ANON, "Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"}


def rest(method: str, path: str, body=None, prefer=None):
    h = dict(HEADERS)
    if prefer:
        h["Prefer"] = prefer
    req = urllib.request.Request(f"{URL}/rest/v1/{path}", method=method, headers=h,
                                 data=json.dumps(body).encode() if body is not None else None)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        sys.exit(f"{method} {path} -> {e.code}: {e.read().decode()[:300]}")


def capture(text: str) -> str:
    item_id = str(uuid.uuid4())
    now = datetime.now(timezone.utc).isoformat()
    rest("POST", "items", [{
        "id": item_id, "user_id": USER_ID, "source_type": "text", "source_door": "share",
        "raw_text": text, "profile": "relationship", "captured_at": now, "created_at": now, "updated_at": now,
    }], prefer="return=minimal")
    return item_id


def wait_for_brief(item_id: str, timeout_s: int = 90) -> str:
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        rows = rest("GET", f"briefs?item_id=eq.{item_id}&select=status,failure_reason")
        if rows and rows[0]["status"] in ("ready", "failed"):
            if rows[0]["status"] == "failed":
                print(f"    ! extraction failed: {rows[0]['failure_reason']}")
            return rows[0]["status"]
        time.sleep(3)
    return "timeout"


def facts_for(subject: str):
    return rest("GET", f"facts?subject=ilike.{urllib.parse.quote(subject)}&select=id,category,value,superseded_by,source_item_id&order=valid_from.asc")


def wait_for_facts(item_id: str, timeout_s: int = 45) -> None:
    """Facts are reconciled AFTER the brief row flips to ready (see extract-brief/index.ts), so
    `ready` alone races the facts insert. Wait for at least one fact or a corroboration link that
    names this item; give up quietly after timeout (the caller's assertions then report it)."""
    deadline = time.time() + timeout_s
    while time.time() < deadline:
        if rest("GET", f"facts?source_item_id=eq.{item_id}&select=id&limit=1") or \
           rest("GET", f"fact_sources?item_id=eq.{item_id}&select=id&limit=1"):
            return
        time.sleep(2)


def sources_for(fact_id: str) -> int:
    return len(rest("GET", f"fact_sources?fact_id=eq.{fact_id}&select=id"))


def rand_name(tag: str) -> str:
    return f"Guard {tag} " + "".join(random.choices(string.ascii_uppercase, k=4))


def loc(facts):
    return [f for f in facts if f["category"] == "location"]


def describe(facts):
    return "; ".join(f"{f['category']}='{f['value'][:40]}'{' [superseded]' if f['superseded_by'] else ''}" for f in facts) or "(none)"


def run_pair(pair: str):
    a = rand_name(pair)
    if pair == "a":
        c1 = f"GUARDTEST notes with {a}. {a} said: I am based in Singapore."
        c2 = c1
    elif pair == "b":
        c1 = f"GUARDTEST notes with {a}. {a} said: I am based in Singapore."
        c2 = f"GUARDTEST catch-up with {a}. {a} told me: I'm in Singapore for the foreseeable future."
    elif pair == "c":
        c1 = f"GUARDTEST notes with {a}. {a} said: I am based in Singapore."
        c2 = f"GUARDTEST update from {a}. {a} said: I have relocated to Jakarta as of this month."
    else:  # d
        b = rand_name("d2")
        c1 = f"GUARDTEST notes with {a}. {a} said: I am based in Singapore."
        c2 = f"GUARDTEST intro call with {b}. {b} said: I am based in Jakarta."

    i1 = capture(c1)
    s1 = wait_for_brief(i1)
    if s1 != "ready":
        return False, f"first extraction {s1}"
    wait_for_facts(i1)
    base = loc(facts_for(a))
    if len(base) != 1:
        return False, f"baseline produced {len(base)} location facts: {describe(facts_for(a))}"

    i2 = capture(c2)
    s2 = wait_for_brief(i2)
    if s2 != "ready":
        return False, f"second extraction {s2}"
    wait_for_facts(i2)

    fa = loc(facts_for(a))
    if pair in ("a", "b"):
        ok = len(fa) == 1 and fa[0]["superseded_by"] is None and sources_for(fa[0]["id"]) == 1
        return ok, f"A: {describe(fa)} extra_sources={sources_for(fa[0]['id']) if fa else '-'}"
    if pair == "c":
        current = [f for f in fa if f["superseded_by"] is None]
        ok = len(fa) == 2 and len(current) == 1 and current[0]["source_item_id"] == i2 and "jakarta" in current[0]["value"].lower()
        return ok, f"A: {describe(fa)}"
    fb = loc(facts_for(b))
    ok = len(fa) == 1 and fa[0]["superseded_by"] is None and sources_for(fa[0]["id"]) == 0 and len(fb) == 1
    return ok, f"A: {describe(fa)} extra_sources={sources_for(fa[0]['id']) if fa else '-'} | B: {describe(fb)}"


def main():
    import urllib.parse  # noqa: F401 (used above via urllib.parse)
    ap = argparse.ArgumentParser()
    ap.add_argument("--runs", type=int, default=5)
    ap.add_argument("--pairs", default="abcd")
    args = ap.parse_args()

    totals = {}
    for pair in args.pairs:
        passes = 0
        for n in range(args.runs):
            t0 = time.time()
            ok, detail = run_pair(pair)
            passes += ok
            print(f"[{pair}] run {n + 1}/{args.runs}: {'PASS' if ok else 'FAIL'} ({time.time() - t0:.0f}s) {detail}", flush=True)
        totals[pair] = passes
    print("\n=== SUMMARY ===")
    names = {"a": "verbatim identical -> same", "b": "reworded identical -> same", "c": "changed value -> changed", "d": "same category, different person -> independent"}
    for pair, passes in totals.items():
        print(f"  ({pair}) {names[pair]}: {passes}/{args.runs}")
    sys.exit(0 if all(p == args.runs for p in totals.values()) else 1)


if __name__ == "__main__":
    import urllib.parse
    main()
