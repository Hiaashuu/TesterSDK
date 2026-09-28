# TesterBuds SDK

[![](https://jitpack.io/v/hiaashuu/testerbuds-sdk.svg)](https://jitpack.io/#hiaashuu/testerbuds-sdk)

Automated background telemetry SDK for Google Play 14-day closed testing compliance on the TesterBuds peer testing platform.

---

## Features
- **Zero UI Disruption**: Runs completely silently in the background without intrusive overlays.
- **Genuine Activity Tracking**: Measures 90-second active foreground sessions and periodic 45-second heartbeats.
- **Automated Pairing**: Detects TesterBuds launch intent tokens automatically.
- **Easy Cleanup**: 1-line integration during testing; simply remove the single line before production publishing.

---

## Integration Guide

### Step 1: Add JitPack repository

In your root `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}

Step 2: Add Dependency

In your app module's build.gradle.kts:

dependencies {
    implementation("com.github.hiaashuu:testerbuds-sdk:1.0.0")
}

Step 3: Initialize in your Application class

package com.example.myapp

import android.app.Application
import com.hiaashuu.testerbuds.sdk.TesterSdk

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Pass your application instance and your package name or TesterBuds listing ID
        TesterSdk.init(this, "YOUR_PACKAGE_NAME_OR_LISTING_ID")
    }
}

(Ensure android:name=".MyApplication" is declared in your AndroidManifest.xml
<application> tag)

Production Release Notice

Once your 14 days of closed testing complete and Google approves your app for
production release, remove TesterSdk.init(...) from your Application class and
remove the dependency from build.gradle.kts.

