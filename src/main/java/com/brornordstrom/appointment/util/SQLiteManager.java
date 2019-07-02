package com.brornordstrom.appointment.util;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.brornordstrom.appointment.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by brornordstrom on 2018-03-20.
 * sqlite database manager
 */

public class SQLiteManager extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "db.sqlite";
    private static final String DB_PATH_SUFFIX = "/.appointment/";
    private Context ctx;

    public SQLiteManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        ctx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {}

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {}

    /**
     * copy database file of raw folder to sdcard
     */
    private void CopyDataBaseFromRaw() throws IOException {

        InputStream myInput = ctx.getResources().openRawResource(R.raw.db_appointment);

        // Path to the just created empty db
        String outFileName = getDatabasePath();

        // if the path doesn't exist first, create it
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + DB_PATH_SUFFIX);
        if (!f.exists()) f.mkdir();

        // Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        // transfer bytes from the input file to the output file
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    /**
     * get database file path
     */
    private String getDatabasePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + DB_PATH_SUFFIX
                + DATABASE_NAME;
    }

    /**
     * get sqlite database from file of sdcard
     */
    public SQLiteDatabase openDataBase() throws SQLException {
        File dbFile = new File(getDatabasePath());
        if (!dbFile.exists()) {
            try {
                CopyDataBaseFromRaw();
                System.out.println("Copying success from Raw folder");
            } catch (IOException e) {
                throw new RuntimeException("Error creating source database", e);
            }
        }
        return SQLiteDatabase.openDatabase(dbFile.getPath(), null,
                SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.CREATE_IF_NECESSARY);
    }

}
