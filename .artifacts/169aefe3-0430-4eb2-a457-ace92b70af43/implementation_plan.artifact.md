# Implementation Plan - User Authentication (Login & Register)

This plan outlines the steps to add a local user authentication system to the Expense Tracker app using the existing Room database and `SharedPreferences`.

## Proposed Changes

### Database Layer
We need to store user credentials locally.

#### [NEW] [User.java](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/User.java)
- Create a new Room Entity with fields: `id` (Primary Key), `name`, `email`, and `password`.

#### [NEW] [UserDao.java](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/UserDao.java)
- Define methods for:
    - `insert(User user)`: To register a new user.
    - `getUser(String email, String password)`: To verify credentials during login.
    - `checkUserExists(String email)`: To prevent duplicate registrations.

#### [MODIFY] [AppDatabase.java](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/database/AppDatabase.java)
- Add the `User` class to the `@Database` entities list.
- Incremement the database version (or handle schema change).
- Add an abstract method to provide `UserDao`.

---

### UI Layer (Layouts)
Create modern, clean interfaces for authentication.

#### [NEW] [activity_register.xml](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_register.xml)
- Fields for Name, Email, Password, and Confirm Password.
- "Register" button and a link to go to the Login page.

#### [NEW] [activity_login.xml](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/res/layout/activity_login.xml)
- Fields for Email and Password.
- "Login" button and a link to go to the Register page.

---

### Business Logic (Activities)

#### [NEW] [RegisterActivity.java](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/RegisterActivity.java)
- Handle registration logic: validation (empty fields, email format, password matching).
- Save the user to the database upon successful validation.

#### [NEW] [LoginActivity.java](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/LoginActivity.java)
- Verify credentials against the database.
- On success, save the "LoggedIn" state and User ID in `SharedPreferences`.
- Redirect to `MainActivity`.

#### [MODIFY] [SplashActivity.java](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/java/com/example/expensetracker/SplashActivity.java)
- Update the delay logic:
    - Check `SharedPreferences` for a logged-in user.
    - If logged in -> Go to `MainActivity`.
    - If NOT logged in -> Go to `LoginActivity`.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/Rushikesh Jagtap/AndroidStudioProjects/ExpenseTracker/app/src/main/AndroidManifest.xml)
- Register `RegisterActivity` and `LoginActivity`.

---

## Verification Plan

### Automated Tests
- Since this is a UI-heavy change, manual verification is recommended first.

### Manual Verification
1. **Registration**: Open the app (first time), go to Register, enter details, and verify success toast.
2. **Login**: Use the newly created credentials to log in.
3. **Session Persistence**: Close the app and reopen. It should bypass Login and go straight to the Dashboard (`MainActivity`).
4. **Validation**: Try registering with an existing email or mismatched passwords.
