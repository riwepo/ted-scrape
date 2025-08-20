# Base image with Java and Clojure CLI
FROM clojure:openjdk-17-tools-deps

# Install curl and Node.js (for Puppeteer)
RUN apt-get update && apt-get install -y curl gnupg \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g npm@latest

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

# Install Puppeteer browser (Chrome)
RUN npx puppeteer browsers install chrome

# Set back to app root
WORKDIR /app

CMD ["clojure", "-M", "-m", "ted-scrape.cli"]

