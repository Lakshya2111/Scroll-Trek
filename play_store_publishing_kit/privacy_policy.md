# Privacy Policy for ScrollTrek

**Last Updated: May 30, 2026**

ScrollTrek is committed to protecting your privacy. This Privacy Policy describes how ScrollTrek handles your information when you use our mobile application.

## 1. Zero Data Collection
ScrollTrek is designed with a strict privacy-first architecture. 
* **No External Servers**: We do not maintain external servers, cloud databases, or tracking systems.
* **No Account Creation**: You do not need to create an account, log in, or provide any personal details (such as name, email, or phone number) to use the app.
* **No Personal Data Collection**: We do not collect, store, transmit, or share any personal identity information, location data, or device identifiers.

## 2. On-Device Scroll Tracking
ScrollTrek calculates your scroll distance locally on your device.
* **Local Processing**: Raw scrolling event coordinates are processed in memory and immediately discarded. Only the computed distance (in meters) and associated app package names are saved.
* **Room Database**: All computed metrics, daily aggregates, and milestones are stored exclusively in a secure, private Room Database on your device.
* **No Cloud Sync**: None of your scrolling habits or data ever leave your device. 

## 3. Accessibility Service Usage
To track scrolling movement across third-party applications, ScrollTrek utilizes Android's Accessibility Service API.
* **Limited Scope**: The Accessibility Service reads screen interaction events strictly to detect scroll delta heights (`TYPE_VIEW_SCROLLED`).
* **Content Privacy**: The service does **not** read, record, or capture any text content, input values, passwords, keyboard strokes, or personal information displayed on the screen.
* **Exclusions**: Tracking is automatically disabled on sensitive system and finance categories, and secure windows are automatically shielded by the operating system.

## 4. Third-Party Services
ScrollTrek does not integrate third-party analytics SDKs, advertising networks, or social trackers. Your usage remains completely private to you.

## 5. Data Deletion
Since all data is stored on-device, you have full control:
* You can purge all tracked records and aggregates at any time by going to **Settings > Clear All Data**.
* Uninstalling the application from your device will permanently delete all local databases and tracking history.

## 6. Contact Us
If you have any questions or feedback regarding this Privacy Policy, you can reach out directly via your app distribution portal.
