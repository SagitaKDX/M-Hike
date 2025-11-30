# Home Page Implementation Guide

## Overview
The home page has been successfully implemented with the following features:
- Weather display (fetches current weather)
- Activity statistics (hikes completed, total km)
- Nearby trails list (from database)
- Bottom navigation bar

## Files Created

### Layouts
1. **activity_home.xml** - Main home page layout
2. **bottom_navigation.xml** - Bottom navigation bar
3. **item_nearby_trail.xml** - RecyclerView item for trails

### Java Classes
1. **HomeActivity.java** - Main activity with weather fetching and database queries
2. **NearbyTrailAdapter.java** - RecyclerView adapter for displaying trails

### Resources
1. **colors.xml** - Updated with green theme colors
2. **strings.xml** - Updated with app strings
3. **nav_item_background.xml** - Drawable for active nav item

## Features Implemented

### 1. Weather Display
- Fetches weather from OpenWeatherMap API
- Displays: Location, Temperature, Description
- **IMPORTANT**: You need to add your OpenWeatherMap API key in `HomeActivity.java` line 147:
  ```java
  String apiKey = "YOUR_API_KEY_HERE"; // Replace with your API key
  ```
- Get free API key from: https://openweathermap.org/api
- If API key is not set, it will show mock data on error

### 2. Activity Statistics
- **Hikes Completed**: Counts all hikes for current user (registered or non-registered)
- **Total km**: Sums up the length of all hikes
- Automatically updates based on database data

### 3. Nearby Trails
- Displays hikes from database in a RecyclerView
- Shows: Name, Distance, Difficulty, Estimated Time
- If user has less than 3 hikes, shows all available hikes (up to 10)
- Click "View" button to see hike details (currently shows Toast)

### 4. Bottom Navigation
- Four tabs: Home, My Hiking, Users, Settings
- Home tab is active by default
- Other tabs show "Coming Soon" toast (to be implemented)

## User Types Support

### Non-Registered Users
- Hikes stored with `userId = null`
- Statistics calculated from non-registered hikes
- Data stored locally only

### Registered Users
- Hikes stored with `userId` (from SharedPreferences)
- Statistics calculated from user's hikes
- Data can be synced to cloud

### Migration When User Registers
When a non-registered user registers, call:
```java
hikeDao.migrateHikesToUser(userId, System.currentTimeMillis());
```
This will update all `userId = null` hikes to the new user's ID.

## Database Queries Used

1. **For Non-Registered Users**:
   ```java
   List<Hike> hikes = hikeDao.getHikesForNonRegisteredUsers();
   ```

2. **For Registered Users**:
   ```java
   List<Hike> hikes = hikeDao.getHikesByUserId(userId);
   ```

3. **All Hikes** (for nearby trails):
   ```java
   List<Hike> allHikes = hikeDao.getAllHikes();
   ```

## SharedPreferences Keys

The app uses SharedPreferences to store user session:
- `KEY_USER_ID` - Current logged-in user ID (-1 if not logged in)
- `KEY_USER_NAME` - Display name for user

## Next Steps

1. **Add Weather API Key**: 
   - Get API key from OpenWeatherMap
   - Replace `YOUR_API_KEY_HERE` in HomeActivity.java

2. **Implement Other Navigation Tabs**:
   - My Hiking: Show user's hike list
   - Users: User management screen
   - Settings: App settings

3. **Add Hike Detail Screen**:
   - Create activity to show full hike details
   - Navigate from "View" button in nearby trails

4. **Enhance Weather**:
   - Add location services to get user's current location
   - Show weather icon from API response

5. **Add Empty State**:
   - Show message when no hikes exist
   - Add "Create First Hike" button

## Testing

To test the home page:
1. Build and run the app
2. The app will launch directly to HomeActivity
3. If no hikes exist, statistics will show 0
4. Weather will show error/mock data if API key not set
5. Nearby trails will be empty if no hikes in database

## Dependencies Added

- RecyclerView (1.3.2)
- CardView (1.0.0)
- Volley (1.2.1) - For HTTP requests
- Glide (4.16.0) - For image loading (ready for future use)

All dependencies are configured in `gradle/libs.versions.toml` and `app/build.gradle.kts`.

