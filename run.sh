#!/bin/bash
# This shell script is used when running the app inside a docker container.
# The command to run the container is
# docker run -it -v "$(pwd)/output:/app/output" ted-scrape
# this sets up a directory to write the odt file to

# Trap Ctrl+C
trap "echo -e '\nExiting...'; exit 0" SIGINT

# Hardcoded output directory inside Docker
OUTPUT_DIR="/app/output"

echo "Welcome to TED Scrape CLI (Docker Edition)"
echo "Press Ctrl+C to exit."

while true; do
  echo -n "Enter TED URL: "
  read URL || break

  if [[ -z "$URL" ]]; then
    echo "URL is required. Try again."
    continue
  fi

  echo "Using output directory: $OUTPUT_DIR"
  echo "Running scrape..."
  clojure -M -m ted-scrape.cli -o "$OUTPUT_DIR" -u "$URL"
  echo "Scrape complete."
  echo ""
done