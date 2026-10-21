#!/usr/bin/env sh
set -eu
PROJECT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
printf '\nJARVIS Android setup launcher\n\n'
if command -v studio >/dev/null 2>&1; then
  exec studio "$PROJECT_DIR"
fi
for candidate in \
  "$HOME/Applications/Android Studio.app/Contents/MacOS/studio" \
  "/Applications/Android Studio.app/Contents/MacOS/studio" \
  "$HOME/android-studio/bin/studio.sh" \
  "/opt/android-studio/bin/studio.sh"; do
  if [ -x "$candidate" ]; then
    exec "$candidate" "$PROJECT_DIR"
  fi
done
printf 'Android Studio was not found. Install it from:\nhttps://developer.android.com/studio\n\n'
printf 'Then open this project folder in Android Studio:\n%s\n' "$PROJECT_DIR"
