#!/bin/bash

if [ ! -t 0 ]; then
  echo "This script requires an interactive terminal."
  exit 1
fi

echo "Welcome to TED Scrape CLI"
echo "Press Ctrl+C to exit."

while true; do
  echo -n "Enter output directory: "
  read DIR
  echo -n "Enter TED URL: "
  read URL

  if [[ -z "$DIR" || -z "$URL" ]]; then
    echo "Both directory and URL are required. Try again."
    continue
  fi

  echo "Running scrape..."
  clojure -M -m ted-scrape.core "$DIR" "$URL"
  echo "Scrape complete."
  echo ""
done