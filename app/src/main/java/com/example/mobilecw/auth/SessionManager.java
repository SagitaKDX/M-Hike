package com.example.mobilecw.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Simple session manager to keep track of the currently active local user.
 *
 * For now this works entirely with a local Room User (integer userId).
 * Later, when Firebase Auth is added, login/logout code can call
 * {@link #setCurrentUserId(Context, int)} and {@link #clearCurrentUser(Context)}
 * after successful sign-in/sign-out.
 */
public class SessionManager {

    private static final String PREFS_NAME = "mhike_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FIREBASE_UID = "firebase_uid";

    /**
     * Returns the currently active local userId, or -1 if there is none.
     */
    public static int getCurrentUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    /**
     * Sets the active local userId after a successful login/registration.
     */
    public static void setCurrentUserId(Context context, int userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public static String getCurrentFirebaseUid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FIREBASE_UID, null);
    }

    public static void setCurrentFirebaseUid(Context context, String firebaseUid) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FIREBASE_UID, firebaseUid).apply();
    }

    /**
     * Clears the active user on logout.
     */
    public static void clearCurrentUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_FIREBASE_UID)
                .apply();
    }

    /**
     * Returns true if a userId is currently stored (logged-in state).
     */
    public static boolean isLoggedIn(Context context) {
        return getCurrentUserId(context) != -1;
    }
}


