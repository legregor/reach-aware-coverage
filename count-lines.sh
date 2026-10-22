#!/bin/bash

# Counts the non-blank lines of the project's own code: the Java backend in
# coverage/ and the Svelte frontend in frontend/.
#
# A line counts when it holds anything other than whitespace, so blank and
# whitespace-only lines are left out; comment lines are counted as code.
#
# Usage: ./count-lines.sh [--by-file]
#   --by-file   also list every counted file with its own line count

set -u

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT" || exit 1

BY_FILE=false
if [[ "${1:-}" == "--by-file" ]]; then
  BY_FILE=true
elif [[ -n "${1:-}" ]]; then
  echo "Usage: $0 [--by-file]" >&2
  exit 1
fi

# Generated, vendored and build output: none of it is code we wrote
PRUNE=(-name node_modules -o -name target -o -name .svelte-kit -o -name build
       -o -name dist -o -name .git)

# The line count of the group counted last, for the section totals
GROUP_LINES=0

# Prints the matching files of a folder, one per line.
# $1: folder, rest: extensions
find_sources() {
  local folder="$1"
  shift
  [[ -d "$folder" ]] || return 0

  local name_args=()
  local extension
  for extension in "$@"; do
    [[ ${#name_args[@]} -gt 0 ]] && name_args+=(-o)
    name_args+=(-name "*.$extension")
  done

  find "$folder" \( "${PRUNE[@]}" \) -prune -o -type f \( "${name_args[@]}" \) -print | sort
}

# Counts the files on stdin and prints their row. $1: label
report_files() {
  local label="$1" files=0 lines=0 file
  while IFS= read -r file; do
    [[ -z "$file" ]] && continue
    files=$((files + 1))
    lines=$((lines + $(awk 'NF' "$file" | wc -l | tr -d ' ')))
  done
  printf '  %-32s %5s files %9s lines\n' "$label" "$files" "$lines"
}

# Counts one group of files and prints its row; sets GROUP_LINES.
# $1: label, $2: folder, rest: extensions
report_group() {
  local label="$1" folder="$2"
  shift 2

  local files=0 lines=0 file file_lines
  local listing=""

  while IFS= read -r file; do
    file_lines=$(awk 'NF' "$file" | wc -l | tr -d ' ')
    files=$((files + 1))
    lines=$((lines + file_lines))
    listing+=$(printf '\n      %7s  %s' "$file_lines" "$file")
  done < <(find_sources "$folder" "$@")

  GROUP_LINES=$lines
  printf '  %-32s %5s files %9s lines\n' "$label" "$files" "$lines"
  [[ "$BY_FILE" == true && -n "$listing" ]] && printf '%s\n' "$listing"
  return 0
}

backend_total=0
frontend_total=0

echo
echo "Non-blank lines of code"
echo "======================="
echo
echo "Backend (Java)"

report_group "coverage/src/main/java" coverage/src/main java
backend_total=$((backend_total + GROUP_LINES))

report_group "coverage/src/test/java" coverage/src/test java
backend_total=$((backend_total + GROUP_LINES))

printf '  %-32s %24s\n' "backend total" "$backend_total lines"

echo
echo "Frontend (Svelte)"

report_group "frontend/src (.svelte)" frontend/src svelte
frontend_total=$((frontend_total + GROUP_LINES))

report_group "frontend/src (.js)" frontend/src js
frontend_total=$((frontend_total + GROUP_LINES))

report_group "frontend/src (.css, .html)" frontend/src css html
frontend_total=$((frontend_total + GROUP_LINES))

printf '  %-32s %24s\n' "frontend total" "$frontend_total lines"

echo
printf '  %-32s %24s\n' "PROJECT TOTAL" "$((backend_total + frontend_total)) lines"

echo
echo "Counted apart (not application code)"

find frontend -maxdepth 1 -type f -name "*.config.js" | sort \
  | report_files "frontend/*.config.js"
find . -maxdepth 1 -type f \( -name "*.sh" -o -name "*.py" \) | sort \
  | report_files "project scripts (*.sh, *.py)"
echo
