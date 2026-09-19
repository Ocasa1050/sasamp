---
name: Android build environment
description: Build prerequisites for this Android project in Replit
---

Gradle can start only when Java and an Android SDK are available; a Java runtime alone is not enough because Android Gradle Plugin configuration fails without `ANDROID_HOME` or `local.properties`.

**Why:** The workspace initially had no Java runtime, and after Java was installed the build stopped at SDK discovery before compiling source.

**How to apply:** Before treating a Gradle build result as a code result, verify both `java -version` and the Android SDK path are configured for the workspace.