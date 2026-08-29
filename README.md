# Mock GPS - Android Location Spoofer

A fully-featured Android mock GPS application with minimal dependencies, built with Kotlin, Jetpack Compose, and OpenStreetMap (osmdroid).

## Features

### Core Features
- **Interactive OpenStreetMap** - No API key required, works offline with cached tiles
- **Global Search** - Search by coordinates, city names, country names, postal/area codes
- **Sub-locality Search** - Find neighborhoods, districts, suburbs within cities
- **Coordinate Display** - Decimal and DMS formats with copy/share functionality
- **Mock Location Provider** - System-level mock location with configurable accuracy, speed, heading

### Advanced Features
- **Per-App Spoofing** - Configure different mock locations for different apps
- **Route Simulation** - Save and replay routes with multiple waypoints
- **Speed Multiplier** - Simulate movement at 0.1x to 10x speed
- **Altitude Control** - GPS altitude or manual override
- **GPX Import/Export** - Standard GPX format support
- **Favorites & History** - Save frequently used locations

### Developer Features
- **Minimal Dependencies** - Only 8 core dependencies (Compose, Room, OkHttp, osmdroid, etc.)
- **No Google Play Services** - Works on devices without GMS
- **Foreground Service** - Reliable background mock location
- **GitHub Actions CI/CD** - Automated APK builds

## Requirements

- Android 7.0 (API 24) or higher
- Developer Options → "Select mock location app" set to Mock GPS
- Location permissions (fine/coarse)

## Building

### Local Build
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires keystore)
./gradlew assembleRelease
```

### GitHub Actions
1. Push to main branch or create a tag `v1.0.0`
2. GitHub Actions automatically builds debug/release APKs
3. Release APKs attached to GitHub Releases

### Keystore Setup (for release builds)
```bash
# Generate keystore
keytool -genkey -v -keystore keystore/release.keystore -alias mockgps -keyalg RSA -keysize 2048 -validity 10000

# Add to GitHub Secrets:
# KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
```

## Project Structure

```
app/
├── src/main/
│   ├── java/com/mockgps/
│   │   ├── data/           # Room entities & DAOs
│   │   ├── network/        # Nominatim API client
│   │   ├── repository/     # Data repositories
│   │   ├── service/        # Mock location foreground service
│   │   ├── ui/
│   │   │   ├── components/ # Reusable UI components
│   │   │   ├── screens/    # Screen composables
│   │   │   └── theme/      # Material3 theming
│   │   └── util/           # Utility classes
│   └── res/                # Resources
└── build.gradle.kts
```

## Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose | Modern UI toolkit |
| Room | Local database |
| OkHttp | HTTP client for Nominatim |
| kotlinx.serialization | JSON parsing |
| osmdroid | OpenStreetMap rendering |
| Accompanist Permissions | Runtime permissions |

Total: ~8MB APK (release, minified)

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_MOCK_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Usage

1. Install APK on device
2. Open Developer Options → "Select mock location app" → Choose "Mock GPS"
3. Grant location permissions
4. Search for any location worldwide
5. Tap "START" to activate mock location
6. Configure per-app spoofing in the "Per-App" tab

## Search Examples

- **Coordinates**: `40.7128, -74.0060`
- **City**: `Tokyo`, `New York`, `London`
- **Country**: `Japan`, `Germany`, `Brazil`
- **Postal Code**: `10001`, `SW1A 1AA`, `10115`
- **Area**: `Manhattan`, `Shibuya`, `Kreuzberg`
- **Landmark**: `Eiffel Tower`, `Statue of Liberty`

## License

MIT License - See LICENSE file for details.

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## Support

For issues and feature requests, please use GitHub Issues.