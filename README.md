# SubTracker

Android app that tracks recurring subscriptions, shows total spend, and reminds you before renewals.

## Monetization
Freemium: free tier tracks up to 5 subscriptions. Premium subscription ($2.99/mo or $19.99/yr via Google Play Billing) unlocks unlimited subscriptions, spending charts, CSV export, and a home-screen widget. Paywall logic lives in `Entitlements.kt`; the purchase flow is in `BillingManager.kt`.

## Getting started on a Chromebook (no local Android Studio needed)

1. **Push this project to GitHub** (see steps below).
2. **Open it in GitHub Codespaces**: on the repo page, click the green "Code" button → "Codespaces" tab → "Create codespace on main". This spins up a full cloud dev environment (JDK + Android SDK, pre-configured via `.devcontainer/devcontainer.json`) that runs entirely in your browser — your Chromebook just needs the tab open.
3. Inside the Codespace terminal, build the app with:
   ```
   ./gradlew assembleDebug
   ```
4. Every push to `main` also triggers a cloud build via GitHub Actions (`.github/workflows/build.yml`), and the resulting APK is attached as a downloadable artifact on the Actions run — so you can get an installable APK without building locally at all.

## Pushing this to GitHub for the first time

From this project folder:
```
git init
git add .
git commit -m "Initial SubTracker scaffold"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/subtracker.git
git push -u origin main
```
(Create the empty repo first at github.com/new — don't initialize it with a README there, to avoid a merge conflict.)

## Before publishing to Google Play
- Create the app listing in Play Console and set up the `premium_monthly` / `premium_yearly` subscription products under Monetize > Products > Subscriptions (prices are set there, not in code).
- Replace the placeholder app icon and theme.
- Add a privacy policy (required for apps using Billing).
- Sign the release build with your own keystore (do NOT commit the keystore to GitHub).
