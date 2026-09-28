# SHealthSync — Samsung Health Sync

**Send today's Samsung Health data directly to your own server.**

English | [简体中文](README.zh-CN.md)

SHealthSync is a small Android app that reads daily health summaries through Samsung's official **Samsung Health Data SDK** and uploads them as JSON to a URL you configure. It supports manual uploads and a daily background schedule.

The project was created for a problem encountered on mainland China Samsung phones: data is visible in Samsung Health but unavailable to workflows that read from Health Connect, including Tasker plugins. SHealthSync provides a direct route from Samsung Health to your server, without depending on Samsung Health exporting that data to Health Connect.

```text
Samsung Health → Samsung Health Data SDK → SHealthSync → Your HTTP(S) endpoint
```

It does not read from or populate Health Connect. It reads existing Samsung Health data with your permission and does not modify it.

## Features

- Upload today's available health summaries with **Sync now**.
- Schedule one automatic upload per day at a configurable local time.
- Configure your own POST URL and optional Bearer token.
- Preview the JSON payload before uploading.
- View the last upload's HTTP status, payload, and server response.
- Use without Tasker or a Health Connect plugin.

The current app interface is in English. The app version is **0.2** (`versionCode = 2`); its package ID remains `com.example.shealthprobe` from the original probe project.

## Requirements and compatibility

- A physical Android phone running **Android 10 / API 29 or later**.
- **Samsung Health 6.30.2 or later**, with the data you want to export already available on the phone.
- Samsung Health Data SDK access and the requested read permissions.
- An HTTP(S) endpoint that accepts the payload described below. A backend is not included.

Samsung documents SDK support for Samsung and other Android smartphones, and excludes emulators. SHealthSync was developed for the mainland China Samsung use case; compatibility across every model, region, and Samsung Health version has not been verified. See [Samsung's SDK requirements](https://developer.samsung.com/health/data/guide/introduction.html).

This project's development workflow uses **Samsung Health's developer mode for data reads**. This is separate from Android's USB debugging setting. Samsung describes this mode as a testing/debugging facility; normal partner access is tied to an app's registered package name and signing certificate. See [Samsung's developer-mode documentation](https://developer.samsung.com/health/data/guide/developer-mode.html). Installing an APK alone does not grant access to health data.

## Getting started

1. Install an APK built for normal installation, or build one from source using the instructions below.
2. Open Samsung Health and confirm that the desired data is present. If it comes from a watch, let the watch sync to the phone first.
3. For development testing, enable Samsung Health's data-read developer mode following the official documentation above.
4. Open **Samsung Health Sync** and tap **Grant Samsung Health permissions**. Grant every requested permission.
5. Enter your server's complete POST URL. If authentication is required, enter only the token in the Bearer token field, without the `Bearer ` prefix.
6. Tap **Save settings**, then **Preview today's payload** to check the data.
7. Tap **Sync now**. Wait a few seconds, then tap **Refresh last result**. A `2xx` response is treated as a successful upload.
8. To enable daily uploads, select **Enable daily automatic sync**, choose the local time, and tap **Save settings**. The default time is **21:30**; automatic sync is off by default.

For a server running on your computer, use an address reachable from the phone, such as:

```text
http://192.168.1.20:5173/
```

This is only an example; the route is not hard-coded. `localhost` on the phone refers to the phone itself. For LAN testing, ensure the server listens on a reachable interface and that the firewall permits access. Use HTTPS for a deployed endpoint.

### Permissions

The app requests read access to steps, activity summary, sleep, floors climbed, water intake, nutrition, and heart rate.

**Preview, manual sync, and scheduled sync currently require all seven permissions.** Heart-rate permission is required by the current permission check even though no heart-rate field is uploaded. Individual metric read failures are handled separately once the initial permission check passes.

## Data and API contract

Each upload contains a snapshot of the current local day's data. Most metrics use the interval from local midnight to the time of reading. Sleep uses the SDK's date-based aggregate for the current local date.

| Field | Meaning | Unit / format |
| --- | --- | --- |
| `date` | Phone's current local date | `YYYY-MM-DD` |
| `source` | Fixed source identifier | `"samsung"` |
| `steps` | Aggregated steps | Count |
| `distance_m` | Total distance from activity summary | Metres |
| `active_minutes` | Total active time, converted to whole minutes | Minutes |
| `active_energy` | Total active calories burned | kcal |
| `sleep_hours` | Sleep duration for the current date, calculated from whole minutes | Hours |
| `floors` | Aggregated floors climbed | Floors |
| `water_ml` | Aggregated water intake | Millilitres |
| `sugar_g` | Sum of sugar values in the nutrition records returned by the SDK | Grams |

`date` and `source` are always included when a payload is produced. A metric is omitted if its read throws an exception. A successful read with an empty result is summed to **zero**, so zero does not necessarily confirm a measured value. The app does not expose individual metric read errors in the payload.

`resting_hr` and `caffeine_mg` are not implemented in this version. Raw heart-rate samples, workout records, and historical backfill are also not exported.

Example payload with illustrative values:

```json
{
  "date": "2026-09-29",
  "source": "samsung",
  "steps": 8240,
  "distance_m": 6120.5,
  "active_minutes": 63,
  "active_energy": 384.2,
  "sleep_hours": 7.5,
  "floors": 8.0,
  "water_ml": 1800.0,
  "sugar_g": 32.4
}
```

Requests use:

```http
POST /your-endpoint
Content-Type: application/json; charset=utf-8
Authorization: Bearer <your-token>
```

The `Authorization` header is omitted when the token field is empty. The app uses 15-second connection and read timeouts, treats HTTP `200–299` as success, and displays up to 800 characters of the response body. It does not require a particular response JSON schema.

### Receiving the data correctly

Your server should accept partial payloads and update the daily record for the authenticated user and `(date, source)`. **Replace the supplied daily totals rather than adding them to existing totals.** Repeated uploads contain overlapping snapshots, and requests may be repeated after failures. Preserve existing fields when they are absent from a new payload, while deciding explicitly how your server should handle zeros.

There is no user/device identifier in the JSON and no idempotency key. If you serve multiple users, associate each request with the correct account through authentication or the endpoint itself.

## Scheduling and limitations

- Scheduling uses Android WorkManager with a one-time job for the next selected local time. Saving settings replaces the pending daily job; after an attempt, the worker schedules the next daily job when automatic sync is enabled.
- Android background and battery restrictions can delay execution. This is not an exact alarm or a continuous sync service.
- Every execution reads **the day on which it actually runs**. An upload delayed past midnight reads the new day; there is no catch-up upload for yesterday. A 21:30 upload also excludes data recorded later that evening unless another upload occurs before midnight.
- A blank URL or missing permissions stops that attempt. HTTP failures and exceptions request a WorkManager retry, but daily rescheduling replaces the same named job; reliable retry-until-delivered behavior is not guaranteed.
- There is no durable queue of captured payloads. A new attempt reads the current data again.

## Privacy and storage

The app sends health data to the URL you configure. The endpoint and token start empty; no fixed upload server is configured in the application code.

Settings, the token, and the last sync status are stored in app-private **SharedPreferences without additional encryption**. The saved status can include the health payload and server response. Android backup is enabled in the manifest. Plain HTTP is permitted for local testing; use HTTPS when transmitting real data beyond a trusted test network, and remove personal data before sharing status screenshots or logs.

## Build from source

Open the project directory containing `settings.gradle.kts` in Android Studio. The IDE may display the project name **SHealthProbe**.

The source snapshot uses:

| Component | Version / setting |
| --- | --- |
| Samsung Health Data SDK | `1.1.0` |
| Android Gradle Plugin | `8.7.3` |
| Kotlin | `2.0.21` |
| Gradle wrapper | `9.3.0` |
| Java / Kotlin bytecode target | `17` |
| Compile / target SDK | `35` |
| Minimum SDK | `29` |

Ensure the Samsung SDK library exists at:

```text
app/libs/samsung-health-data-api-1.1.0.aar
```

The supplied source folder includes this AAR. If it is absent from a checkout, obtain the matching SDK from [Samsung Developer](https://developer.samsung.com/health/data/overview.html). Install Android SDK Platform 35 and configure a compatible Gradle JDK in Android Studio.

The listed versions describe the supplied files, not a newly verified build environment. This snapshot pairs AGP 8.7.3 with Gradle 9.3.0; if sync fails, review their compatibility before building. Android's [AGP 8.7 documentation](https://developer.android.com/build/releases/past-releases/agp-8-7-0-release-notes) lists Gradle 8.9 and JDK 17 as its baseline. No toolchain changes are implied by this README.

Once Gradle sync succeeds, build a debug APK from the project root:

```powershell
# Windows PowerShell
.\gradlew.bat assembleDebug
```

```sh
# macOS / Linux
bash ./gradlew assembleDebug
```

The normal output location is `app/build/outputs/apk/debug/app-debug.apk`. For an APK intended for distribution, use Android Studio's **Build → Generate Signed Bundle / APK → APK**, select the release variant, and retain your signing key for future updates. See [Android's signing guide](https://developer.android.com/studio/publish/app-signing).

An APK created by Android Studio's **Run** action may be marked `testOnly` and require ADB installation. Use a build/export action to produce a downloadable APK; see [Android's APK build documentation](https://developer.android.google.cn/build/build-for-release?hl=en).

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| Permission request fails | Samsung Health version, SDK access/developer mode, and the requested read permissions. |
| Heart-rate permission seems unnecessary | It is part of the current all-permissions check, although heart rate is not uploaded. |
| A metric is missing or zero | Confirm the data is present in Samsung Health and synced from the watch. Failed reads are omitted; empty results can become zero. |
| Upload cannot reach the server | URL, phone-to-server connectivity, firewall, and the use of a reachable host instead of `localhost`. |
| HTTP `401` or `403` | The token and server authorization rules; do not include `Bearer ` in the token field. |
| Scheduled upload is late or absent | Automatic sync setting, saved time, battery/background restrictions, and the last result. |
| Status still shows an earlier attempt | Tap **Refresh last result** after the queued job has had time to run. |
| Downloaded APK will not install | Check whether it is test-only and whether its signing certificate matches an already installed copy. |

## Acknowledgements and licensing

Samsung Health access is provided by Samsung's official **Samsung Health Data SDK**. SHealthSync is an independent project and is not an official Samsung sync service.

The supplied source snapshot contains no project `LICENSE` file. This README does not assign a license to the application or to Samsung's SDK. The SDK and any reused Samsung materials remain subject to their applicable terms; preserve the original notices when preparing a public repository.
