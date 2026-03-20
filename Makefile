.PHONY: build debug release clean install run uninstall lint test apk-size devices help \
       release-commit release-push release-tag version-bump

# Package name
PACKAGE := com.hightemp.offline_tube
MAIN_ACTIVITY := $(PACKAGE).MainActivity
APK_DEBUG := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE := app/build/outputs/apk/release/app-release-unsigned.apk
VERSION := $(shell cat VERSION 2>/dev/null | tr -d '[:space:]')

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

# ─── Build ────────────────────────────────────────────────────────────

build: debug ## Build debug APK (alias)

debug: ## Build debug APK
	./gradlew assembleDebug

release: ## Build release APK
	./gradlew assembleRelease

bundle: ## Build release AAB (App Bundle)
	./gradlew bundleRelease

clean: ## Clean build artifacts
	./gradlew clean

# ─── Install & Run ───────────────────────────────────────────────────

install: debug ## Build debug APK and install on connected device
	adb install -r $(APK_DEBUG)

install-release: release ## Build release APK and install on connected device
	adb install -r $(APK_RELEASE)

run: install ## Build, install and launch on device
	adb shell am start -n $(PACKAGE)/$(MAIN_ACTIVITY)

uninstall: ## Uninstall app from device
	adb uninstall $(PACKAGE)

stop: ## Force-stop the app on device
	adb shell am force-stop $(PACKAGE)

restart: stop run ## Stop and re-launch the app

clear-data: ## Clear app data on device
	adb shell pm clear $(PACKAGE)

# ─── Device ──────────────────────────────────────────────────────────

devices: ## List connected devices
	adb devices -l

logcat: ## Show app logs (Ctrl+C to stop)
	adb logcat --pid=$$(adb shell pidof -s $(PACKAGE)) 2>/dev/null || \
		adb logcat | grep -i "offline_tube\|$(PACKAGE)"

logcat-crash: ## Show crash logs
	adb logcat -b crash -d

screenshot: ## Take a screenshot from device
	@mkdir -p tmp
	adb exec-out screencap -p > tmp/screenshot_$$(date +%Y%m%d_%H%M%S).png
	@echo "Screenshot saved to tmp/"

# ─── Quality ─────────────────────────────────────────────────────────

lint: ## Run Android lint
	./gradlew lintDebug

test: ## Run unit tests
	./gradlew testDebugUnitTest

test-connected: ## Run instrumented tests on device
	./gradlew connectedDebugAndroidTest

apk-size: debug ## Show debug APK size
	@du -h $(APK_DEBUG)

# ─── Misc ────────────────────────────────────────────────────────────

deps: ## Show dependency tree
	./gradlew app:dependencies --configuration debugRuntimeClasspath | head -100

sync: ## Sync Gradle project
	./gradlew prepareKotlinBuildScriptModel

# ─── Release ─────────────────────────────────────────────────────────

version: ## Show current version
	@echo "v$(VERSION)"

version-set: ## Update version in build.gradle.kts from VERSION file
	@echo "Setting version to $(VERSION)..."
	@sed -i 's/versionName = ".*"/versionName = "$(VERSION)"/' app/build.gradle.kts
	@VCODE=$$(echo "$(VERSION)" | awk -F. '{printf "%d", $$1*10000+$$2*100+$$3}'); \
		sed -i "s/versionCode = .*/versionCode = $$VCODE/" app/build.gradle.kts
	@echo "Updated build.gradle.kts: versionName=$(VERSION), versionCode=$$VCODE"

release-commit: version-set ## Commit version change and create tag
	git add -A
	git commit -m "release: v$(VERSION)" || true
	git tag -f "v$(VERSION)"
	@echo "Created tag v$(VERSION)"

release-push: release-commit ## Commit, tag and force-push for release
	git push origin master
	git push origin "v$(VERSION)" -f
	@echo "Pushed v$(VERSION) — GitHub Actions will build the release"
