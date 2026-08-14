

# Packages the Spring Boot application into a Docker image so it
# can run identically on any machine — your laptop, a teammate's
# Windows machine, a Linux cloud server — without any "works on
# my machine" issues.


# WHY TWO STAGES (multi-stage build)?
# Stage 1 (builder): compiles the Java source code and packages
#   it into a fat jar. This stage uses a full JDK image (~500MB).
# Stage 2 (runtime): runs the compiled jar. This stage uses a
#   minimal JRE image (~200MB) — no compiler, no build tools.


# The final image only contains Stage 2. Stage 1 is discarded
# after compilation. Result: a much smaller production image that
# contains only what's needed to RUN the app, not build it.


# HOW TO BUILD AND RUN MANUALLY (without docker-compose):
#   docker build -t ecom-store .
#   docker run -p 9092:9092 ecom-store




# ─────────────────────────────────────────────────────────────
# STAGE 1 — Build
# Uses Maven + JDK 21 to compile and package the application
# ─────────────────────────────────────────────────────────────


FROM eclipse-temurin:21-jdk-alpine AS builder

# Set the working directory inside the container
WORKDIR /app



# Copy Maven wrapper and pom.xml FIRST (before source code).
# WHY? Docker caches each layer. If pom.xml hasn't changed,
# Docker reuses the cached dependency download layer on the
# next build — only re-downloading dependencies when pom.xml
# actually changes, not on every code change.

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .



# Download all dependencies (cached separately from source code)
# RUN ./mvnw dependency:go-offline -B
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy the actual source code
COPY src ./src


# Build the fat jar, skipping tests (tests run in CI pipeline,
# not during Docker image build, to keep build times fast)
# RUN ./mvnw clean package -DskipTests -B
RUN chmod +x mvnw && ./mvnw clean package -DskipTests -B


# ─────────────────────────────────────────────────────────────
# STAGE 2 — Runtime
# Minimal JRE — only what's needed to RUN Java, not compile it
# ─────────────────────────────────────────────────────────────


FROM eclipse-temurin:21-jre-alpine AS runtime


# Create a non-root user for security — running as root inside
# a container is a security risk even with Docker's isolation
RUN addgroup -S ecomstore && adduser -S ecomstore -G ecomstore
USER ecomstore

WORKDIR /app


# Copy ONLY the compiled jar from Stage 1 into the runtime image
COPY --from=builder /app/target/*.jar app.jar


# Create logs directory (matches application.yml's logs/ecom.log)
RUN mkdir -p logs

# Tell Docker which port this container listens on (documentation
# only — actual port mapping happens in docker-compose or docker run)
EXPOSE 8080


# Health check — Docker uses this to determine if the container
# is "healthy" and ready to receive traffic. Matches the
# /actuator/health endpoint we enabled in Part 2's application.yml
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1



# The command that runs when the container starts
# -Djava.security.egd: speeds up startup by using /dev/urandom
#   instead of /dev/random for random number generation (which
#   can block startup in containerized environments)
# -XX:+UseContainerSupport: tells the JVM to respect Docker's
#   memory limits rather than using the host machine's total RAM
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]