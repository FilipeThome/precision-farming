# Mobile

Windows slice: Android app in `composeApp`, gateway at `10.0.2.2:8080`.

Approved layout from [kmp-architect](91d4d619-25da-4b0f-8cdc-1ba7d5e9e33e) for the next extract:

```
mobile/
  shared/      # KMP library: domain, data, Compose UI, SQLDelight
  androidApp/  # thin host
  iosApp/      # Swift host + GMSServices.provideAPIKey (macOS only)
```

Gate iOS targets with `HostManager.hostIsMac`. `xcodebuild` is not required on Windows.

Offline: enqueue mutations with `clientOperationId`, then `POST /api/v1/sync/push`. Google Maps via expect/actual — never a static satellite PNG.
