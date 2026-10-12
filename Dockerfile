# GnuCOBOL reference runtime for ParityProof (Satish lane).
# Build:  docker build -t parityproof-cobol .
# Run the full pipeline in one container (also needs the Java side in-built).
FROM debian:bookworm
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
      gnucobol openjdk-21-jdk-headless && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY cobol/ cobol/
COPY java/  java/
COPY parity/ parity/
COPY run.sh .
CMD ["bash", "run.sh"]