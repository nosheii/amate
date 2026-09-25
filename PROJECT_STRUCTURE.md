# Amate project structure

The app is organized by feature. Each feature should contain its own data access,
repository, UI state, ViewModel, and Compose screen where relevant.

This is a temporary list of responsibilities. It is completely random, and if 
anyone wants to switch just discuss it 

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

## How to work with branches

This is to try not to step on each others toes, and forces us to get accustomed to
each others code and what everyone else is doing.

You can do this either with Android Studio's VCS menu (buttons) or the
terminal (commands).

### 1. Create your new branch

Try to only work in your own branch.

**VCS menu**: Git > New Branch... (top right, click the branch name shown,
  e.g. "main", then "New Branch")

**Terminal**: git checkout -b your-branch-name
  Example: git checkout -b feature/navigation

### 2. Work on your code

Work on your code as normal in Android Studio.

### 3. Commit and push

When you're done and it feels ready, commit and push your changes, but stay
on your own branch.

**VCS menu**: Git > Commit... (write a short message, click "Commit"), then
  Git > Push...

**Terminal**:
  git add .
  git commit -m "Short description of what you did"
  git push origin your-branch-name

  The first time you push a new branch, Git might ask you to run the full
  command it suggests instead (something like
  git push --set-upstream origin your-branch-name) — just copy and run that.

### 4. Open a pull request

Go to GitHub (the website) and open a pull request from your branch into
main. GitHub usually shows a "Compare & pull request" button right after
you push, its big, green and on the page for Amate.

### 5. Get it reviewed

Someone else then needs to review your code on GitHub (comment or approve
the pull request) before it gets merged into main. Only merge on GitHub
once you have an approval.

### 6. Update your local main

After your branch is merged, switch back to main and pull the latest
changes before starting your next branch.

**VCS menu**:
click the branch name (bottom right), select "main", then
  Git > Pull...

**Terminal**:
  git checkout main
  git pull origin main