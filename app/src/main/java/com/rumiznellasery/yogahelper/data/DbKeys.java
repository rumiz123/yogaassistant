package com.rumiznellasery.yogahelper.data;

import android.content.Context;

import org.json.JSONObject;
import java.io.InputStream;

/** Utility to load Firebase database keys from assets/db_keys.json. */
public class DbKeys {
    private static DbKeys instance;

    public final String databaseUrl;
    public final String users;
    public final String displayName;
    public final String workouts;
    public final String totalWorkouts;
    public final String calories;
    public final String streak;
    public final String score;
    public final String level;

    // moved declaration here, will be initialized in ctor
    public final String emailVerified;

    private DbKeys(Context context) {
        try {
            InputStream is = context.getAssets().open("db_keys.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();

            String json = new String(buffer, java.nio.charset.StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(json);

            databaseUrl   = obj.getString("database_url");
            users         = obj.getString("users");
            displayName   = obj.getString("displayName");
            workouts      = obj.getString("workouts");
            totalWorkouts = obj.optString("totalWorkouts", "totalWorkouts");
            calories      = obj.optString("calories", "calories");
            streak        = obj.optString("streak", "streak");
            score         = obj.getString("score");
            level         = obj.getString("level");

            // read emailVerified key (default to "emailVerified")
            emailVerified = obj.optString("emailVerified", "emailVerified");

        } catch (Exception e) {
            throw new RuntimeException("Failed to load db keys", e);
        }
    }

    public static synchronized DbKeys get(Context context) {
        if (instance == null) {
            instance = new DbKeys(context.getApplicationContext());
        }
        return instance;
    }
}
