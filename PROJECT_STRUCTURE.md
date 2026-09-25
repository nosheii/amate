# Amate project structure

The app is organized by feature. Each feature should contain its own data access,
repository, UI state, ViewModel, and Compose screen where relevant.

Suggested ownership:

- Ana: profile, avatar, and design system
- Najma: goals, tasks, database coordination, and navigation
- Felix: steps, sensors, and location
- Nora: water, reminders, weather, and pollen integration

Implement one vertical feature at a time:

1. Entity
2. DAO
3. Repository
4. ViewModel and UI state
5. Compose screen
6. Tests
7. Navigation integration

The files currently contain placeholders only. Add Room, Navigation, WorkManager,
sensor, and network implementations when the relevant feature is started.
