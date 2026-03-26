# Jalankan dari root repo: make dev
.PHONY: dev install-once emulator
dev:
	@chmod +x scripts/dev-emulator.sh
	@./scripts/dev-emulator.sh

# Satu kali build + install (tanpa watch)
install-once:
	@./gradlew installDebug

# Buka emulator Pixel_6_API34 (-gpu on); jalankan di terminal terpisah dari `make dev`
emulator:
	@chmod +x scripts/start-emulator-pixel6.sh
	@./scripts/start-emulator-pixel6.sh
