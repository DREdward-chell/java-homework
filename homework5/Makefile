# gpucalc demo / smoke / benchmark targets.
#
# All targets shell out to the fat JAR built by `mvn package`. Each target is a single, named
# invocation so it can be copy-pasted for debugging and so `make -n <target>` prints the exact
# command line (CLAUDE.md §2.5 — "prefer more functions"). The PowerShell companion at
# scripts/demo.ps1 mirrors these targets one-to-one for Windows.
#
# Environment:
#   JAVA       — java executable (default: $JAVA_HOME/bin/java, else `java` on PATH)
#   MVN        — maven wrapper path (default: ./mvnw)
#   BACKEND    — rendering backend id passed as -b (default: java)
#   OUT_DIR    — output root (default: out/demo)
#
# Examples:
#   make build smoke
#   make BACKEND=opengl-compute swirl-classic
#   make benchmark-threads
#   make all

SHELL          := /bin/bash
MVN            ?= ./mvnw
JAR            := target/gpucalc-0.1.0-SNAPSHOT.jar
BACKEND        ?= java
OUT_DIR        ?= out/demo

ifeq ($(origin JAVA), undefined)
ifdef JAVA_HOME
JAVA           := $(JAVA_HOME)/bin/java
else
JAVA           := java
endif
endif

RUN            := $(JAVA) -jar $(JAR) -b $(BACKEND)

# Classic 2-transform Sierpinski-like skeleton — reused by many targets.
AFFINE_2X      := 0.5,0,0,0,0.5,0/0.5,0,0.5,0,0.5,0.5
# A 3-transform flame with gentle asymmetry — exercises the transform picker.
AFFINE_3X      := 0.5,0,-0.25,0,0.5,0/0.5,0,0.25,0,0.5,0/0.5,0,0,0,0.5,0.5
# A 4-transform spiral-ish setup. Color coordinates spread across [0,1] per TransformFactory.
AFFINE_4X      := 0.6,0.2,0,-0.2,0.6,0/0.5,0,0.3,0,0.5,0.3/0.5,0,-0.3,0,0.5,0.3/0.4,0,0,0,0.4,-0.4

.PHONY: help build clean-demo tree smoke \
        sierpinski swirl-classic horseshoe-gallery spherical-star sinusoidal-waves \
        blended-pair blended-triple dense-4x \
        dry-run dry-run-json \
        epochs-10 epochs-20 \
        gif-30 gif-50-slow gif-fast-loop \
        threads-1 threads-2 threads-4 threads-8 benchmark-threads \
        opencl-smoke opencl-swirl opencl-list benchmark-backends \
        reject-opengl-on-mac all

help:
	@echo "gpucalc make targets:"
	@echo "  build               — mvn package (produces fat JAR)"
	@echo "  clean-demo          — remove $(OUT_DIR)"
	@echo "  smoke               — 500x500, 1M iter, swirl, sanity check"
	@echo ""
	@echo "Single PNG renders (different variations, dense iterations):"
	@echo "  sierpinski          — 2 affines × linear, 5M iter"
	@echo "  swirl-classic       — swirl × 1.0 + linear × 0.3, 20M iter"
	@echo "  horseshoe-gallery   — horseshoe × 1.0, 20M iter, 3 affines"
	@echo "  spherical-star      — spherical × 1.0 + linear × 0.2, 15M iter"
	@echo "  sinusoidal-waves    — sinusoidal × 1.0, 12M iter"
	@echo "  blended-pair        — swirl+horseshoe blend, 25M iter"
	@echo "  blended-triple      — swirl+sinusoidal+spherical, 30M iter, 4 affines"
	@echo "  dense-4x            — 4-affine spiral, 50M iter, 4 threads"
	@echo ""
	@echo "Configuration echo (no render):"
	@echo "  dry-run             — prints resolved config from CLI args"
	@echo "  dry-run-json        — prints resolved config merged with $(OUT_DIR)/config.json"
	@echo ""
	@echo "Epoch sequences:"
	@echo "  epochs-10           — 10 PNGs showing density buildup"
	@echo "  epochs-20           — 20 PNGs, longer run"
	@echo ""
	@echo "Animated GIFs:"
	@echo "  gif-30              — 30-frame swirl animation, default delay"
	@echo "  gif-50-slow         — 50-frame horseshoe, 30cs per frame"
	@echo "  gif-fast-loop       — 60-frame blended, 5cs per frame"
	@echo ""
	@echo "Thread scaling:"
	@echo "  threads-1           — 10M iter, 1 thread"
	@echo "  threads-2           — 10M iter, 2 threads"
	@echo "  threads-4           — 10M iter, 4 threads"
	@echo "  threads-8           — 10M iter, 8 threads"
	@echo "  benchmark-threads   — run all four, time each"
	@echo ""
	@echo "OpenCL backend (availability-gated; use --cl-platform / --cl-device to pick):"
	@echo "  opencl-smoke         — BACKEND=opencl, 500x500, 1M iter"
	@echo "  opencl-swirl         — BACKEND=opencl, 1920x1080 swirl render"
	@echo "  opencl-list          — lists CL platforms/devices via --dry-run help"
	@echo "  benchmark-backends   — times java / opengl-compute / opencl at the same render"
	@echo ""
	@echo "  reject-opengl-on-mac — verifies the macOS rejection message"
	@echo "  all                  — every demo target above"

tree $(OUT_DIR):
	@mkdir -p $(OUT_DIR) $(OUT_DIR)/epochs $(OUT_DIR)/benchmark

build $(JAR):
	$(MVN) -q -DskipTests package

clean-demo:
	rm -rf $(OUT_DIR)

smoke: $(JAR) tree
	$(RUN) -w 500 -h 500 -i 1000000 --seed 7 \
	    -f swirl:1.0 -ap $(AFFINE_2X) \
	    -o $(OUT_DIR)/smoke.png

sierpinski: $(JAR) tree
	$(RUN) -w 1024 -h 1024 -i 5000000 --seed 1 -t 2 \
	    -f linear:1.0 \
	    -ap 0.5,0,0,0,0.5,0/0.5,0,0.5,0,0.5,0/0.5,0,0.25,0,0.5,0.5 \
	    -o $(OUT_DIR)/sierpinski.png

swirl-classic: $(JAR) tree
	$(RUN) -w 1920 -h 1080 -i 20000000 --seed 32.123531 -t 2 \
	    -f swirl:1.0,linear:0.3 -ap $(AFFINE_2X) \
	    -o $(OUT_DIR)/swirl-classic.png

horseshoe-gallery: $(JAR) tree
	$(RUN) -w 1600 -h 1200 -i 20000000 --seed 11.5 -t 4 \
	    -f horseshoe:1.0 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/horseshoe-gallery.png

spherical-star: $(JAR) tree
	$(RUN) -w 1200 -h 1200 -i 15000000 --seed 2.71828 -t 2 \
	    -f spherical:1.0,linear:0.2 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/spherical-star.png

sinusoidal-waves: $(JAR) tree
	$(RUN) -w 1600 -h 900 -i 12000000 --seed 3.14159 -t 2 \
	    -f sinusoidal:1.0 -ap $(AFFINE_2X) \
	    -o $(OUT_DIR)/sinusoidal-waves.png

blended-pair: $(JAR) tree
	$(RUN) -w 1600 -h 1200 -i 25000000 --seed 99 -t 4 \
	    -f swirl:0.7,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/blended-pair.png

blended-triple: $(JAR) tree
	$(RUN) -w 1920 -h 1080 -i 30000000 --seed 17.75 -t 4 \
	    -f swirl:0.5,sinusoidal:0.3,spherical:0.2 -ap $(AFFINE_4X) \
	    -o $(OUT_DIR)/blended-triple.png

dense-4x: $(JAR) tree
	$(RUN) -w 2048 -h 2048 -i 50000000 --seed 137.035999 -t 4 \
	    -f swirl:1.0,linear:0.2 -ap $(AFFINE_4X) \
	    -o $(OUT_DIR)/dense-4x.png

dry-run: $(JAR)
	$(RUN) --dry-run -w 1600 -h 900 -i 123456 --seed 4.2 -t 3 \
	    -f swirl:1.0,linear:0.3 -ap $(AFFINE_2X)

dry-run-json: $(JAR) tree
	@printf '%s\n' \
	  '{' \
	  '  "width": 1280,' \
	  '  "height": 720,' \
	  '  "seed": 8.8,' \
	  '  "iteration_count": 777777,' \
	  '  "threads": 2,' \
	  '  "functions": "horseshoe:1.0,linear:0.25",' \
	  '  "affine_params": "$(AFFINE_3X)"' \
	  '}' > $(OUT_DIR)/config.json
	$(RUN) --dry-run --config $(OUT_DIR)/config.json -t 8

epochs-10: $(JAR) tree
	$(RUN) --output-mode epochs -w 800 -h 800 -i 4000000 --seed 5 -t 2 \
	    -f swirl:1.0,linear:0.3 -ap $(AFFINE_2X) \
	    --epoch-count 10 \
	    -o $(OUT_DIR)/epochs/swirl.png

epochs-20: $(JAR) tree
	$(RUN) --output-mode epochs -w 800 -h 800 -i 20000000 --seed 12 -t 4 \
	    -f horseshoe:0.7,sinusoidal:0.3 -ap $(AFFINE_3X) \
	    --epoch-count 20 \
	    -o $(OUT_DIR)/epochs/horseshoe.png

gif-30: $(JAR) tree
	$(RUN) --output-mode gif -w 600 -h 600 -i 6000000 --seed 5 -t 2 \
	    -f swirl:1.0,linear:0.3 -ap $(AFFINE_2X) \
	    --epoch-count 30 \
	    -o $(OUT_DIR)/swirl-30.gif

gif-50-slow: $(JAR) tree
	$(RUN) --output-mode gif -w 500 -h 500 -i 10000000 --seed 99 -t 4 \
	    -f horseshoe:1.0 -ap $(AFFINE_3X) \
	    --epoch-count 50 --gif-delay 30 \
	    -o $(OUT_DIR)/horseshoe-slow.gif

gif-fast-loop: $(JAR) tree
	$(RUN) --output-mode gif -w 480 -h 480 -i 15000000 --seed 17 -t 4 \
	    -f swirl:0.6,sinusoidal:0.4 -ap $(AFFINE_4X) \
	    --epoch-count 60 --gif-delay 5 \
	    -o $(OUT_DIR)/fast-loop.gif

threads-1: $(JAR) tree
	$(RUN) -w 1280 -h 720 -i 10000000 --seed 42 -t 1 \
	    -f swirl:1.0,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/benchmark/t1.png

threads-2: $(JAR) tree
	$(RUN) -w 1280 -h 720 -i 10000000 --seed 42 -t 2 \
	    -f swirl:1.0,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/benchmark/t2.png

threads-4: $(JAR) tree
	$(RUN) -w 1280 -h 720 -i 10000000 --seed 42 -t 4 \
	    -f swirl:1.0,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/benchmark/t4.png

threads-8: $(JAR) tree
	$(RUN) -w 1280 -h 720 -i 10000000 --seed 42 -t 8 \
	    -f swirl:1.0,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/benchmark/t8.png

benchmark-threads: $(JAR) tree
	@for n in 1 2 4 8; do \
	  echo ">>> threads=$$n"; \
	  /usr/bin/time -p $(RUN) -w 1280 -h 720 -i 10000000 --seed 42 -t $$n \
	    -f swirl:1.0,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/benchmark/t$$n.png 2>&1 | grep -E '(real|java backend: done)'; \
	done

opencl-smoke: $(JAR) tree
	$(JAVA) -jar $(JAR) -b opencl -w 500 -h 500 -i 1000000 --seed 7 \
	    -f swirl:1.0 -ap $(AFFINE_2X) \
	    -o $(OUT_DIR)/opencl-smoke.png

opencl-swirl: $(JAR) tree
	$(JAVA) -jar $(JAR) -b opencl -w 1920 -h 1080 -i 20000000 --seed 32.123531 \
	    -f swirl:1.0,linear:0.3 -ap $(AFFINE_2X) \
	    -o $(OUT_DIR)/opencl-swirl.png

opencl-list: $(JAR)
	@echo "Use --cl-platform <index|substring> and --cl-device <index|substring> to pick."
	@$(JAVA) -jar $(JAR) --help | grep -A2 -E 'cl-platform|cl-device'

benchmark-backends: $(JAR) tree
	@for be in java opengl-compute opencl; do \
	  echo ">>> backend=$$be"; \
	  /usr/bin/time -p $(JAVA) -jar $(JAR) -b $$be \
	    -w 1280 -h 720 -i 10000000 --seed 42 -t 4 \
	    -f swirl:1.0,horseshoe:0.3 -ap $(AFFINE_3X) \
	    -o $(OUT_DIR)/benchmark/$$be.png 2>&1 | grep -E '(real|backend: done|not supported|No OpenCL)'; \
	done

reject-opengl-on-mac: $(JAR)
	@echo "Expect: 'not supported on macOS' message and exit code 3."
	@$(JAVA) -jar $(JAR) -b opengl-compute -f linear:1.0 \
	    -ap 0.5,0,0,0,0.5,0 -o /tmp/should-not-exist.png; \
	code=$$?; \
	echo "exit=$$code"; \
	test $$code -eq 3

all: smoke sierpinski swirl-classic horseshoe-gallery spherical-star sinusoidal-waves \
     blended-pair blended-triple dense-4x dry-run dry-run-json \
     epochs-10 epochs-20 gif-30 gif-50-slow gif-fast-loop benchmark-threads
