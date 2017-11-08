package com.valeo.podometer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by l-avaratha on 08/06/2016
 */
public class LogFileUtils {
    public final static String LOGS_DIR = "/Logs/";
    private final static String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd_kk";
    private final static String RSSI_TIMESTAMP_FORMAT = "HH:mm:ss:SSS";
    public final static SimpleDateFormat sdfRssi = new SimpleDateFormat(RSSI_TIMESTAMP_FORMAT, Locale.FRANCE);
    private final static SimpleDateFormat sdfFilename = new SimpleDateFormat(FILENAME_TIMESTAMP_FORMAT, Locale.FRANCE);
    private final static String LOG_FILE_PREFIX = LOGS_DIR + "log_";
    private final static String FILE_EXTENSION = ".csv";
    private static File logFile = null;
    private static int counter = 0;
    private static BufferedWriter writer;

    private static void write(String text) {
        try {
            writer.append(text + "\n");
            Log.d("log", text);
        } catch (Exception e) {
        }
    }

    public static void closeWriter() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the string to write in the log file and add it
     */
    public static void appendLogs(float[] acceleration,
                                  float[] geomagnetic,
                                  float[] gyro,
                                  float[] gravity,
                                  float[] linAcc,
                                  float[] orientation,
                                  float[] globalAcc) {
        final String delimiter = ";";
        String log = acceleration[0] + delimiter + acceleration[1] + delimiter + acceleration[2] + delimiter +
                geomagnetic[0] + delimiter + geomagnetic[1] + delimiter + geomagnetic[2] + delimiter +
                gyro[0] + delimiter + gyro[1] + delimiter + gyro[2] + delimiter +
                gravity[0] + delimiter + gravity[1] + delimiter + gravity[2] + delimiter +
                linAcc[0] + delimiter + linAcc[1] + delimiter + linAcc[2] + delimiter +
                orientation[0] + delimiter + orientation[1] + delimiter + orientation[2] + delimiter +
                globalAcc[0] + delimiter + globalAcc[1] + delimiter + globalAcc[2];
        String timestamp = sdfRssi.format(new Date());
        write(timestamp + delimiter + log);
    }

    /**
     * Create a log file to register the settings and all rssi values
     *
     * @return true if the file exist or is succesfully created, false otherwise
     */
    public static boolean createLogFile(Context mContext) {
        if (createDir(mContext.getExternalCacheDir(), LOGS_DIR)) {
            final String timestampLog = sdfFilename.format(new Date());
            counter = mContext.getSharedPreferences("counter_pref", Context.MODE_PRIVATE).getInt("counter", 0);
            final String filename = LOG_FILE_PREFIX
                    + counter
                    + "_" + timestampLog + FILE_EXTENSION;
            logFile = new File(mContext.getExternalCacheDir(), filename);
            if (!logFile.exists()) {
                try {
                    if (logFile.createNewFile()) {
                        writer = new BufferedWriter(new FileWriter(logFile, true));
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Write in the log file a line with the rssi affiliated parameters column names
     */
    public static void writeColumnNames() {
        String delimiter = ";";
        String colNames = "Timestamp" + delimiter
                + "ACC_X" + delimiter + "ACC_Y" + delimiter + "ACC_Z" + delimiter
                + "MAGNET_X" + delimiter + "MAGNETC_Y" + delimiter + "MAGNET_Z" + delimiter
                + "GYRO_X" + delimiter + "GYRO_Y" + delimiter + "GYRO_Z" + delimiter
                + "GRAVITY_X" + delimiter + "GRAVITY_Y" + delimiter + "GRAVITY_Z" + delimiter
                + "LIN_ACC_X" + delimiter + "LIN_ACC_Y" + delimiter + "LIN_ACC_Z" + delimiter
                + "AZIMUTH" + delimiter + "PITCH" + delimiter + "ROLL" + delimiter
                + "GLOBAL_ACC_X" + delimiter + "GLOBAL_ACC_Y" + delimiter + "GLOBAL_ACC_Z";
        write(colNames);
    }

    private static boolean createDir(File dirPath, String dirName) {
        File dir = new File(dirPath, dirName);
        //if the folder doesn't exist
        if (!dir.exists()) {
            if (dir.mkdir()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static void incrementCounter(Context mContext) {
        counter++;
        final SharedPreferences mPrefs = mContext.getSharedPreferences("counter_pref", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt("counter", counter);
        editor.apply();
    }
}
