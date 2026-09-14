#!/usr/bin/env bash
# Bulk-creates milestones + issues for the LogisticsConnect elective.
#
# Prereqs:
#   1. gh CLI installed (https://cli.github.com)
#   2. Run `gh auth login` once if you haven't
#   3. Run this from inside your cloned repo (or set REPO below explicitly)
#
# Usage:
#   chmod +x create-issues.sh
#   ./create-issues.sh

set -euo pipefail

# If you're not running this from inside the repo directory, uncomment and set:
# REPO="yourusername/sin-001-logisticsconnect"
# GH_FLAGS="--repo $REPO"
GH_FLAGS=""

echo "Creating milestones..."

gh api $GH_FLAGS repos/:owner/:repo/milestones -f title="Stage 1 - Clean CSV" -f state="open" >/dev/null 2>&1 || true
gh api $GH_FLAGS repos/:owner/:repo/milestones -f title="Stage 2 - REST services" -f state="open" >/dev/null 2>&1 || true
gh api $GH_FLAGS repos/:owner/:repo/milestones -f title="Stage 3 - MQ decoupling" -f state="open" >/dev/null 2>&1 || true
gh api $GH_FLAGS repos/:owner/:repo/milestones -f title="Stage 4 - AlertBot (stretch)" -f state="open" >/dev/null 2>&1 || true
gh api $GH_FLAGS repos/:owner/:repo/milestones -f title="Cross-cutting" -f state="open" >/dev/null 2>&1 || true

# gh issue create doesn't take a milestone *number* lookup easily inline,
# so we pass milestone by title via --milestone (gh resolves it for us).

echo "Creating issues..."

gh issue create $GH_FLAGS \
  --title "Parse CSV into a domain model" \
  --body $'Read `hubs-global.csv` in `IngestionServiceApp`, map each row to a `HubRecord`. No cleaning yet, just get raw rows into objects.\n\n- [ ] CSV reads without error\n- [ ] One `HubRecord` per row' \
  --milestone "Stage 1 - Clean CSV"

gh issue create $GH_FLAGS \
  --title "Fix casing and padding" \
  --body $'Trim whitespace, collapse double spaces, normalize casing on `hub_id`, `province`, `sorting_center`.\n\n- [ ] `" gauteng "` -> `"Gauteng"`\n- [ ] `h-501` -> `H-501`\n- [ ] `"Cape Town  Port"` -> `"Cape Town Port"`' \
  --milestone "Stage 1 - Clean CSV"

gh issue create $GH_FLAGS \
  --title "Normalize booleans and missing values" \
  --body $'Handle `Y`/`yes`/`1`/`true`/`YES` -> single boolean form. Handle `N/A`, `unknown`, blank, `-`, `NaN`.\n\n- [ ] `active` field is a real `boolean` in the cleaned model\n- [ ] Missing-value policy documented (what happens to a row with no province, etc.)' \
  --milestone "Stage 1 - Clean CSV"

gh issue create $GH_FLAGS \
  --title "Duplicate detection" \
  --body $'Detect and resolve records describing the same real-world hub under different IDs (e.g. `H-500`, `H-504`, `H-510`, `H-515` all = "Johannesburg Central").\n\n- [ ] Dedup strategy implemented\n- [ ] Reasoning written down (comment or PR description) - rubric explicitly wants you to be able to explain this' \
  --milestone "Stage 1 - Clean CSV"

gh issue create $GH_FLAGS \
  --title "Expose cleaned records via REST" \
  --body $'Add a `GET /hubs` endpoint returning the cleaned list as JSON.\n\n- [ ] `curl localhost:7050/hubs` returns cleaned JSON array\n- [ ] Closes Stage 1' \
  --milestone "Stage 1 - Clean CSV"

gh issue create $GH_FLAGS \
  --title "hub-service serves place-name data" \
  --body $'`hub-service` calls `ingestion-service` (`GET :7050/hubs`) and exposes `GET /hubs/{hubId}`.\n\n- [ ] Real endpoint beyond `/health`\n- [ ] Data sourced from ingestion-service, not re-parsed' \
  --milestone "Stage 2 - REST services"

gh issue create $GH_FLAGS \
  --title "delay-stage-service tracks delay stages" \
  --body $'`GET /delay-stage/{hubId}` and `POST /delay-stage/{hubId}` with `{ "stage": n }`.\n\n- [ ] In-memory store of hub -> stage is fine\n- [ ] Reasonable status codes (404 for unknown hub, etc.)' \
  --milestone "Stage 2 - REST services"

gh issue create $GH_FLAGS \
  --title "transit-service calculates an ETA" \
  --body $'`transit-service` calls hub-service + delay-stage-service synchronously and returns an ETA-shaped response.\n\n- [ ] `GET /eta/{hubId}` works end-to-end for at least one hub\n- [ ] Closes Stage 2' \
  --milestone "Stage 2 - REST services"

gh issue create $GH_FLAGS \
  --title "Broker + delay-stage-service publishes" \
  --body $'Bring up ActiveMQ via `common/docker-compose.yml`. `delay-stage-service` publishes to `package-status-topic` on stage change.\n\n- [ ] Broker runs locally\n- [ ] Message visible in ActiveMQ web console or logs on a `POST /delay-stage/{hubId}`' \
  --milestone "Stage 3 - MQ decoupling"

gh issue create $GH_FLAGS \
  --title "transit-service subscribes instead of calling directly" \
  --body $'Replace the synchronous call to delay-stage-service with an MQ subscription.\n\n- [ ] transit-service no longer makes the direct REST call for delay stage\n- [ ] End-to-end message flow demonstrated\n- [ ] Closes Stage 3' \
  --milestone "Stage 3 - MQ decoupling"

gh issue create $GH_FLAGS \
  --title "AlertBot subscribes and simulates alerts" \
  --body $'Subscribe to `package-status-topic`, pick a stage threshold, log/simulate a post when crossed.\n\n- [ ] Threshold chosen and documented\n- [ ] Simulated alert fires on a real message' \
  --milestone "Stage 4 - AlertBot (stretch)"

gh issue create $GH_FLAGS \
  --title "Tests" \
  --body $'Add JUnit 5 + Surefire to at least one module, write a few real tests beyond `/health`.' \
  --milestone "Cross-cutting"

gh issue create $GH_FLAGS \
  --title "Debrief notes" \
  --body $'Short write-up: dedup strategy + reasoning, why REST vs MQ at each stage, what you would change with more time. (Rubric explicitly grades this.)' \
  --milestone "Cross-cutting"

echo "Done. Run 'gh issue list' to check."
