package com.brornordstrom.appointment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.brornordstrom.appointment.util.Global;
import com.brornordstrom.appointment.util.SQLiteManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@SuppressLint("DefaultLocale")
public class HomeActivity extends AppCompatActivity {

    private Context context;

    private String selected_date;
    private DatePicker date_picker;
    private DatePickerDialog pickerDialog = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        context = this;
        // calendar control define
        date_picker = findViewById(R.id.date_picker);

        // confirm permission
        // https://developer.android.com/training/permissions/requesting.html : reference site
        if (confirmationPermission()) {
            // sqlite db setting
            Global.dbHelper = new SQLiteManager(context);
            Global.mdb = Global.dbHelper.openDataBase();
        }

        // set current date to selected date
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        selected_date = sdf.format(Calendar.getInstance().getTime());
        Global.current_appointment = new String[4];
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == Global.PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                // check permission result
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Permission error", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // sqlite db setting
            Global.dbHelper = new SQLiteManager(context);
            Global.mdb = Global.dbHelper.openDataBase();
        }
    }

    /**
     *  confirm permission
     */
    private boolean confirmationPermission(){
        if ( ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context,Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED  ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    Global.PERMISSION_REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    // create appointment function
    public void doCreateAppointment(View view) {

        // get selected date
        selected_date = String.format("%02d-%02d-%d", date_picker.getYear(), date_picker.getMonth() + 1, date_picker.getDayOfMonth());

        // go to Creating Appointment page
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra("isEdit", false);
        intent.putExtra("date", selected_date);
        startActivity(intent);
    }

    // edit appointment function
    public void doEditAppointment(View view) {

        // get selected date
        selected_date = String.format("%02d-%02d-%d", date_picker.getYear(), date_picker.getMonth() + 1, date_picker.getDayOfMonth());

        // get appointment of this date
        Cursor c = Global.mdb.rawQuery("SELECT * FROM t_appointment WHERE date = '" + selected_date + "'", null);

        if (c.getCount() == 0) {
            // if not exist any appointment in this date, show error toast
            Toast.makeText(context, "Can not found appointment in this date", Toast.LENGTH_SHORT).show();
            return;
        }

        // get count of appointment
        int count = c.getCount();
        // all appointment array
        final String[][] appointments = new String[count][4];
        // all title of appointment array
        String[] titles = new String[count];

        // get array value from sqlite cursor
        for (int i = 0; i < count; i++) {
            c.moveToPosition(i);
            // appointment id
            appointments[i][0] = String.valueOf(c.getInt(0));
            // appointment title
            appointments[i][1] = c.getString(1);
            titles[i] = c.getString(1);
            // appointment date
            appointments[i][2] = c.getString(2);
            // appointment description
            appointments[i][3] = c.getString(3);
        }

        // show item select dialog with titles of appointment
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Appointment")
                .setCancelable(false)
                .setPositiveButton("Close", null)
                .setItems(titles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // set current appointment
                        Global.current_appointment = appointments[which];

                        // go to update page
                        Intent intent = new Intent(context, DetailActivity.class);
                        intent.putExtra("isEdit", true);
                        startActivity(intent);
                    }
                });
        builder.show();
    }

    // delete appointment function
    public void doDeleteAppointment(View view) {

        // get selected date
        selected_date = String.format("%02d-%02d-%d", date_picker.getYear(), date_picker.getMonth() + 1, date_picker.getDayOfMonth());

        String[] items = new String[]{"Delete all appointments for that date", "Select appointment to delete"};

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select deleting option")
                .setCancelable(false)
                .setPositiveButton("Close", null)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) doDeleteAllAppointment();
                        else if (which == 1) doSelectToDelete();
                    }
                });
        builder.show();
    }

    // move appointment function
    public void doMoveAppointment(View view) {

        // get selected date
        selected_date = String.format("%02d-%02d-%d", date_picker.getYear(), date_picker.getMonth() + 1, date_picker.getDayOfMonth());

        // get appointment of this date
        Cursor c = Global.mdb.rawQuery("SELECT * FROM t_appointment WHERE date = '" + selected_date + "'", null);

        if (c.getCount() == 0) {
            // if not exist any appointment in this date, show error toast
            Toast.makeText(context, "Can not found appointment in this date", Toast.LENGTH_SHORT).show();
            return;
        }

        // get count of appointment
        int count = c.getCount();
        // all appointment array
        final int[] ids = new int[count];
        // all title of appointment array
        String[] titles = new String[count];

        // get array value from sqlite cursor
        for (int i = 0; i < count; i++) {
            c.moveToPosition(i);
            // appointment id
            ids[i] = c.getInt(0);
            // appointment title
            titles[i] = c.getString(1);
        }

        // show item select dialog with titles of appointment
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Appointment")
                .setCancelable(false)
                .setPositiveButton("Close", null)
                .setItems(titles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doSelectDateForMove(ids[which]);
                    }
                });
        builder.show();

    }

    // search appointment function
    public void doSearchAppointment(View view) {

        final EditText edit_search = new EditText(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Search Appointment")
                .setView(edit_search)
                .setCancelable(false)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String search = edit_search.getText().toString();
                        doShowSearchedAppointment(search);
                    }
                });
        builder.show();
    }

    /**
     * Delete all appointments for that date from database
     */
    private void doDeleteAllAppointment() {
        // delete all appointment of selected date
        Global.mdb.delete("t_appointment", "date = '" + selected_date + "'", null);
        // show success toast
        Toast.makeText(context, "All appointment of this date deleted successfully.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Select appointment to delete from database
     */
    private void doSelectToDelete() {

        // get appointment of this date
        Cursor c = Global.mdb.rawQuery("SELECT * FROM t_appointment WHERE date = '" + selected_date + "'", null);

        if (c.getCount() == 0) {
            // if not exist any appointment in this date, show error toast
            Toast.makeText(context, "Can not found appointment in this date", Toast.LENGTH_SHORT).show();
            return;
        }

        // get count of appointment
        int count = c.getCount();
        // all appointment array
        final int[] ids = new int[count];
        // all title of appointment array
        String[] titles = new String[count];

        // get array value from sqlite cursor
        for (int i = 0; i < count; i++) {
            c.moveToPosition(i);
            // appointment id
            ids[i] = c.getInt(0);
            // appointment title
            titles[i] = c.getString(1);
        }

        // show item select dialog with titles of appointment
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Appointment")
                .setCancelable(false)
                .setPositiveButton("Close", null)
                .setItems(titles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // delete selected appointment from database
                        Global.mdb.delete("t_appointment", "id = " + ids[which], null);
                        // show success toast
                        Toast.makeText(context, "Selected appointment deleted successfully.", Toast.LENGTH_SHORT).show();
                    }
                });
        builder.show();

    }

    private void doSelectDateForMove(final int id) {

        Calendar newCalendar = Calendar.getInstance();
        pickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {

            @SuppressLint("DefaultLocale")
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                ContentValues values = new ContentValues();
                values.put("date", String.format("%d-%02d-%02d", year, monthOfYear + 1, dayOfMonth));
                Global.mdb.update("t_appointment", values, "id = " + id, null);
                pickerDialog.dismiss();
                Toast.makeText(context, "Appointment moved successfully.", Toast.LENGTH_SHORT).show();
            }

        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));

        pickerDialog.show();
    }

    /**
     * show all appointment searched appointment as search text
     */
    private void doShowSearchedAppointment(String search_text) {

        search_text = search_text.toLowerCase();

        // get appointment of this date
        Cursor c = Global.mdb.rawQuery(
                "SELECT * " +
                        "FROM t_appointment " +
                        "WHERE lower(title) LIKE \"%" + search_text + "%\" " +
                            "OR lower(description) LIKE \"%" + search_text + "%\"", null);

        if (c.getCount() == 0) {
            // if not exist any appointment in this date, show error toast
            Toast.makeText(context, "Can not found appointment as search text", Toast.LENGTH_SHORT).show();
            return;
        }

        // get count of appointment
        int count = c.getCount();
        // all appointment array
        final String[][] appointments = new String[count][4];
        // all title of appointment array
        String[] titles = new String[count];

        // get array value from sqlite cursor
        for (int i = 0; i < count; i++) {
            c.moveToPosition(i);
            // appointment id
            appointments[i][0] = String.valueOf(c.getInt(0));
            // appointment title
            appointments[i][1] = c.getString(1);
            titles[i] = c.getString(1);
            // appointment date
            appointments[i][2] = c.getString(2);
            // appointment description
            appointments[i][3] = c.getString(3);
        }

        // show item select dialog with titles of appointment
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Appointment")
                .setCancelable(false)
                .setPositiveButton("Close", null)
                .setItems(titles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // set current appointment
                        Global.current_appointment = appointments[which];

                        // go to update page
                        Intent intent = new Intent(context, DetailActivity.class);
                        intent.putExtra("isEdit", true);
                        startActivity(intent);
                    }
                });
        builder.show();

    }

}
