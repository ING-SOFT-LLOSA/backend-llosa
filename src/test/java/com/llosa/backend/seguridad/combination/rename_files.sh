#!/bin/bash
cd "$(dirname "$0")"
for f in *.bak; do
  if [ -f "$f" ]; then
    base="${f%.bak}"
    mv "$f" "$base"
    echo "Renamed $f to $base"
  fi
done
echo "Renaming complete!"
