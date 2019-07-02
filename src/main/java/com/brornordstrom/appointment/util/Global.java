package com.brornordstrom.appointment.util;

import android.database.sqlite.SQLiteDatabase;

/**
 * Created by brornordstrom on 2018-03-21.
 *
 */

public class Global {

    public static SQLiteManager dbHelper;
    public static SQLiteDatabase mdb;

    public static final int PERMISSION_REQUEST_CODE = 1111;
    public static String[] current_appointment;

}
