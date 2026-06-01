# ScrollTrek - Play Store Policy Declarations

Use these answers to fill out the policy questionnaires under **Policy and programs > App content** in the Google Play Console.

---

## 1. Accessibility Service Declaration
Google Play requires detailed justification for using the Accessibility Service. Copy and paste the following when asked.

### Purpose Declaration
```text
This app uses the Accessibility Service API solely to calculate scrolling movement coordinates (on-device) across active applications. This allows users to track their daily cumulative scroll distance and reach milestone landmark goals. It does not monitor keystrokes, collect personal data, or transmit information externally.
```

### Prominent Disclosure Statement
The app handles this locally prior to requesting permissions. Confirm to Google Play that:
> The app displays a prominent on-device disclosure on the onboarding screen explaining the scrolling distance calculation before directing the user to enable the accessibility permission.

---

## 2. Foreground Service Declaration
For modern Android versions (Android 14+), you must declare and justify Foreground Service usage.

### Foreground Service Type
Select **Special Use** (`FOREGROUND_SERVICE_SPECIAL_USE`).

### Use Case Explanation
```text
A persistent foreground service is required to display real-time scroll tracking statistics (today's current distance) to the user via a background notification, ensuring the tracking task remains active and is not killed by the Android operating system.
```

---

## 3. Data Safety Questionnaire
Since ScrollTrek works 100% locally and stores everything on-device, use these exact settings to declare your data practices.

### Core Data Collection & Sharing Questions
* **Does your app collect or share any of the required user data types?**
  * Select: **No**
* **Is all of the user data collected by your app encrypted in transit?**
  * Select: **Not applicable** (no data is collected or transmitted)
* **Do you provide a way for users to request that their data be deleted?**
  * Select: **Yes** (users can clear all logs and data directly in the Settings menu)
