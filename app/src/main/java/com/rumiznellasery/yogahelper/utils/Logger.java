package com.rumiznellasery.yogahelper.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static final String TAG = "YogaHelper";
    private static final String LOG_FILE_NAME = "yoga_helper_logs.txt";
    private static final int MAX_LOG_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_LOG_FILES = 3;
    
    private static File logFile;
    private static FileWriter fileWriter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    
    public static void init(Context context) {
        try {
            File logDir = new File(context.getFilesDir(), "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            logFile = new File(logDir, LOG_FILE_NAME);
            
            // Rotate logs if file is too large
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                rotateLogs(logDir);
            }
            
            fileWriter = new FileWriter(logFile, true);
            log("INFO", "Logger initialized", null);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize logger", e);
        }
    }
    
    private static void rotateLogs(File logDir) {
        try {
            // Delete oldest log file if we have too many
            for (int i = MAX_LOG_FILES - 1; i >= 0; i--) {
                File oldFile = new File(logDir, LOG_FILE_NAME + "." + i);
                if (i == MAX_LOG_FILES - 1 && oldFile.exists()) {
                    oldFile.delete();
                } else if (oldFile.exists()) {
                    oldFile.renameTo(new File(logDir, LOG_FILE_NAME + "." + (i + 1)));
                }
            }
            
            // Rename current log file
            if (logFile.exists()) {
                logFile.renameTo(new File(logDir, LOG_FILE_NAME + ".0"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to rotate logs", e);
        }
    }
    
    public static void log(String level, String message, Throwable throwable) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] %s: %s", timestamp, level, message);
        
        // Log to Android logcat
        switch (level) {
            case "ERROR":
                Log.e(TAG, message, throwable);
                break;
            case "WARN":
                Log.w(TAG, message, throwable);
                break;
            case "DEBUG":
                Log.d(TAG, message, throwable);
                break;
            default:
                Log.i(TAG, message, throwable);
                break;
        }
        
        // Write to file
        writeToFile(logEntry, throwable);
    }
    
    private static synchronized void writeToFile(String logEntry, Throwable throwable) {
        if (fileWriter == null) return;
        
        try {
            fileWriter.write(logEntry + "\n");
            if (throwable != null) {
                fileWriter.write("Exception: " + throwable.toString() + "\n");
                for (StackTraceElement element : throwable.getStackTrace()) {
                    fileWriter.write("\tat " + element.toString() + "\n");
                }
            }
            fileWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file", e);
        }
    }
    
    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }
    
    public static void error(String message) {
        log("ERROR", message, null);
    }
    
    public static void warn(String message, Throwable throwable) {
        log("WARN", message, throwable);
    }
    
    public static void warn(String message) {
        log("WARN", message, null);
    }
    
    public static void info(String message) {
        log("INFO", message, null);
    }
    
    public static void debug(String message) {
        log("DEBUG", message, null);
    }
    
    public static void close() {
        try {
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to close logger", e);
        }
    }
    
    public static String getLogFilePath(Context context) {
        File logDir = new File(context.getFilesDir(), "logs");
        return new File(logDir, LOG_FILE_NAME).getAbsolutePath();
    }
} 