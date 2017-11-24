package com.mobiroo.n.sourcenextcorporation.tagstand.util;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.Date;

public class Logger {
	
    public static final String DIR_NAME = AppSpecific.DIR_NAME;
    public static final String FILE_NAME = AppSpecific.FILE_NAME;
    
    private static final int TRUNCATE_LOG_FILE_SIZE = 1048576;
    private static final String TRUNCATED_CHECKPOINT = "LOG_FILE_WAS_TRUNCATED_AT_THIS_TIME";


    public static void i(String Message) {
        Logger.i(AppSpecific.TAG, Message);
    }

    public static void i(String TAG, String Message) {
        Log.i(TAG, Message);
        if (AppSpecific.logDebug) {
            writeFile(Message, true);
        }
    }

    public static void e(String Message) {
        e(AppSpecific.TAG, Message);
    }

    public static void e(String Message, Exception e) {
        Logger.e(AppSpecific.TAG, Message, e);
    }
    
    public static void e(String TAG, String Message, Exception e) {
        e.printStackTrace();
        
        if (AppSpecific.logDebug) {
            Log.e(TAG, Message);
            writeFile(e.getMessage(), true);
        }
    }

    public static void e(String TAG, String Message) {
        if (AppSpecific.logDebug) {
            Log.e(TAG, Message);
            writeFile(Message, true);
        }
    }

    
    public static void d(String message) {
    	d(message, true);
    }
    
    public static void d(String Message, boolean truncateLogFileIfNeeded) {
        if (AppSpecific.logDebug) {
            Log.d(AppSpecific.TAG, Message);
            writeFile(Message, truncateLogFileIfNeeded);
        }
    }


    private static class fileWriter extends AsyncTask<String, Void, Void> {
        
        private boolean mTruncate = true;
        
        public fileWriter(boolean truncateLogFileIfNeeded) {
            this.mTruncate = truncateLogFileIfNeeded;
        }
        
        @SuppressWarnings("deprecation")
        @Override
        protected Void doInBackground(String... params) {
            String message = params[0];
            String title = params[1];

            // Check if external storage is mounted, if not, return.
            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return null;
            }

            File root = Environment.getExternalStorageDirectory();
            if (root.canWrite()) {
                try {
                    File container = new File(root.getPath() + "/" + DIR_NAME + "/");
                    container.mkdirs();
                    File log = new File(container, title);
                    
                    long truncateTime = -1;
                    if (mTruncate && (log.length() > TRUNCATE_LOG_FILE_SIZE)) {
                    	truncateTime = Logger.truncateFile(log);
                    }
                    
                    FileWriter writer = new FileWriter(log, true);
                    BufferedWriter out = new BufferedWriter(writer);
                    Date now = new Date();
                    if (truncateTime > 0) {
                    	out.write(now.toLocaleString() + ": Truncated log file in " + String.valueOf(truncateTime)+ " ms.\n");
                    }
                    out.write(now.toLocaleString() + ": " + message + "\n");
                    out.flush();
                    writer.flush();
                    out.close();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    public static void writeFile(String message, boolean truncate) {
        new Logger.fileWriter(truncate).execute(message, FILE_NAME);
    }
    
    public static synchronized long truncateFile(File log) {

    	long truncateBegin = System.currentTimeMillis();
    	try {
    		RandomAccessFile raf = new RandomAccessFile(log, "rw");
    		
    		if (raf.length() <= TRUNCATE_LOG_FILE_SIZE) {raf.close(); return -1;}

    		byte[] buf = new byte[TRUNCATE_LOG_FILE_SIZE / 2];                    	
    		raf.seek(TRUNCATE_LOG_FILE_SIZE / 2);
    		raf.readFully(buf);
    		raf.seek(0);
    		raf.write(buf);
    		raf.writeBytes("\n" + TRUNCATED_CHECKPOINT + "\n");
    		raf.setLength(TRUNCATE_LOG_FILE_SIZE / 2 + TRUNCATED_CHECKPOINT.length() + 2);
    		raf.close();
    	} catch (Exception e) {
    		return -1;
    	} catch (OutOfMemoryError e) {
            // This occurs when we cannot allocate enough space for the truncation / write.  Happens rarely.
            // If this becomes an issue we may need to use android:largeHeap="true"
            return -1;
        }

    	return (System.currentTimeMillis() - truncateBegin);
    }
}
