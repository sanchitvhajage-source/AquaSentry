Aqua Sentry is a stable, dual-check flood intelligence app that combines river and rainfall data to provide rock-solid, hyperlocal risk scores, ensuring reliable evacuation routes when minutes matter.
Our project focused on solving the fundamental problem of inaccurate flood data in urban and coastal environments (like our testing location, Vikhroli, Mumbai).
Exceptional Features & Technical Achievements
BOMB-PROOF STABILITY-We engineered highly defensive Java code with synchronization and comprehensive null checks to eliminate all critical crashes (NullPointerExceptions, JSON failures) that occur when external APIs return faulty or incomplete data. The app cannot crash when you need it most.
DUAL-API RISK FUSION
Solved the "false safe" problem by fusing data from two APIs: GloFAS River Discharge AND Open-Meteo Precipitation Sum. The app takes the highest risk factor, accurately flagging danger from extreme rainfall even when river levels are zero.

SEAMLESS UX ONBOARDING
Streamlined the user journey by fixing the common Android bug that required double-tapping permissions. The app now automatically proceeds to the dashboard immediately upon final permission grant

FULL UTILITY SUITE
Provides one-tap access to National Emergency Contacts and reliable, AI-powered Evacuation Routes that trigger upon high-risk detection

 Built With
Platform: Native Android (Java)
Version Control: Git
Fluvial Risk API: Open-Meteo Flood API (GloFAS)
Pluvial Risk API: Open-Meteo Weather Forecast API (precipitation_sum and timezone=auto for reliability)
Location: Google Fused Location Provider
UI/Design: Material Design Components


  -Future Vision (Scalability & Social Impact)
Multi-Language Access: Implementing localization to ensure critical alerts are delivered in the native language of every global user, fostering genuine social inclusion.
Community Vetting: Adding a feature for users to crowdsource ground-level flood photos and reports, instantly validating our predictive models against real-world user data.

To start my app

Clone the repository.
Open the project in Android Studio (4.1+ recommended).
Build and run on an Android device with location services enabled.

Licensing
This project is open source and licensed under the MIT License.
Copyright (c) 2025 Sanchit Sanjay Vhajage
