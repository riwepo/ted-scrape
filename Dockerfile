# Base image with Java + Node
FROM node:20-bullseye

# Install Clojure CLI
RUN curl -O https://download.clojure.org/install/linux-install-1.11.1.1413.sh \
    && chmod +x linux-install-1.11.1.1413.sh \
    && ./linux-install-1.11.1.1413.sh

# Create app directory
WORKDIR /app

# Copy Clojure app
COPY deps.edn .
COPY src ./src
COPY resources ./resources

# Install Puppeteer dependencies
WORKDIR /app/resources/scripts
COPY resources/scripts/package.json .
RUN npm install --omit=dev

# Set back to app root
WORKDIR /app

# Default command (can be overridden)
CMD clojure -M -m ted-scrape.cli
