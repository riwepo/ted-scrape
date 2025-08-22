FROM clojure:openjdk-17-tools-deps

WORKDIR /app
COPY . /app

RUN clojure -P
RUN chmod +x run.sh

CMD ["./run.sh"]