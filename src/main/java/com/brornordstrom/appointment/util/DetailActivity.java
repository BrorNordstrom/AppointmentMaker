package com.brornordstrom.appointment;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.brornordstrom.appointment.util.Global;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DetailActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private Context context;
    private boolean isEdit;
    private String selected;

    private ListView list_synonyms;
    private EditText edit_title;
    private TextView text_date;
    private EditText edit_description;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        context = this;
        initActivity();
        progressDialog = new ProgressDialog(context);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // when click back button, finish activity
            case android.R.id.home:
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has
     * been clicked.
     * <p>
     * Implementers can call getItemAtPosition(position) if they need
     * to access the data associated with the selected item.
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String replace = (String) list_synonyms.getAdapter().getItem(position);
        edit_description.setText(edit_description.getText().toString().replace(selected, replace));
        list_synonyms.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new ArrayList<>()));
        edit_description.setSelection(edit_description.getText().toString().length());
    }

    private void initActivity() {
        // set back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.button_get_synonyms).setOnClickListener(this);

        // synonyms list view
        list_synonyms = findViewById(R.id.list_synonyms);
        // define item click event
        list_synonyms.setOnItemClickListener(this);

        // title edit text
        edit_title = findViewById(R.id.edit_title);
        text_date = findViewById(R.id.text_date);
        edit_description = findViewById(R.id.edit_description);

        // when press edit button, isEdit is true
        // when press create button, isEdit is false
        isEdit = getIntent().getExtras().getBoolean("isEdit");

        // if isEdit is true, show current appointment in edit text box, otherwise skip
        if (isEdit) {
            edit_title.setText(Global.current_appointment[1]);
            text_date.setText(Global.current_appointment[2]);
            edit_description.setText(Global.current_appointment[3]);
            ((TextView) findViewById(R.id.button_create)).setText(R.string.update_appointment);
            // set title of action bar
            getSupportActionBar().setTitle(R.string.update_appointment);
        } else {
            // set selected date
            text_date.setText(getIntent().getExtras().getString("date"));
            // set title of action bar
            getSupportActionBar().setTitle(R.string.create_appointment);
            ((TextView) findViewById(R.id.button_create)).setText(R.string.create_appointment);
        }

    }

    /**
     * get synonyms of selected word in description text box
     */
    /**
     * Called when a view has been clicked.
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_get_synonyms) {

            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edit_description.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);

            try {
                selected = edit_description.getText().toString().substring(edit_description.getSelectionStart(), edit_description.getSelectionEnd());
                if ("".equals(selected)) {
                    Toast.makeText(context, "You must select word in Description text", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                String url = "http://thesaurus.altervista.org/thesaurus/v1?word=" + selected + "&language=en_US&key=yHPGJChI4Z3Y4pFItT92&output=xml";
                new AsyncGetData().execute(url);
            } catch (Exception e) {
                e.printStackTrace();

                selected = edit_description.getText().toString().trim();
                if ("".equals(selected)) {
                    Toast.makeText(context, "You must select word in Description text", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressDialog.setMessage("Please wait...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                String url = "http://thesaurus.altervista.org/thesaurus/v1?word=" + selected + "&language=en_US&key=yHPGJChI4Z3Y4pFItT92&output=xml";
                new AsyncGetData().execute(url);
            }
        }
    }


    /**
     * create appointment
     */
    public void doCreateAppointment(View view) {

        // get title of appointment from edit text
        String title = edit_title.getText().toString();
        // get date of appointment from edit text
        String date = text_date.getText().toString();
        // get description of appointment from edit text
        String description = edit_description.getText().toString();

        // if any value is empty, show error toast and return
        if ("".equals(title) || "".equals(date) || "".equals(description)) {
            Toast.makeText(context, "Parameter empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEdit) {

            // update appointment

            Cursor c = Global.mdb.rawQuery("SELECT * FROM t_appointment WHERE id = " + Global.current_appointment[0] + " LIMIT 1", null);

            if (c.getCount() == 0) {
                // if can not found appointment in database, show error toast
                Toast.makeText(context, "Current appointment not exist in database", Toast.LENGTH_SHORT).show();
            } else {
                // create ContentValue for updating
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("date", date);
                values.put("description", description);
                // update table using query builder of android sqlite database
                // reference url : https://developer.android.com/reference/android/database/sqlite/package-summary.html
                Global.mdb.update("t_appointment", values, "id = " + Global.current_appointment[0], null);
                // show success toast
                Toast.makeText(context, "Appointment updated successfully", Toast.LENGTH_SHORT).show();
                finish(); // finish this activity
            }

        } else {

            // create appointment


            Cursor c = Global.mdb.rawQuery("SELECT * FROM t_appointment WHERE title = \"" + title + "\" AND date = '" + date + "' LIMIT 1", null);

            if (c.getCount() != 0) {
                // if exist appointment of same title in database, show error toast
                c.moveToFirst();
                Toast.makeText(context, String.format("Appointment %s. " +
                                "\nalready exists, please choose a different event title", c.getString(1)),
                        Toast.LENGTH_LONG).show();
            } else {
                // create ContentValue for creating
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("date", date);
                values.put("description", description);
                // insert table using query builder of android sqlite database
                Global.mdb.insert("t_appointment", null, values);
                // show success toast
                Toast.makeText(context, "Appointment created successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     * parse xml from inputStream and show synonyms to list view
     */
    private void doParseXML(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {

        // xml parser factory
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // xml parser document builder
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        // xml parser document
        Document doc = dBuilder.parse(inputStream);

        // get xml element
        Element element = doc.getDocumentElement();
        element.normalize();

        // list of xml elements
        NodeList nList = doc.getElementsByTagName("list");

        int count = nList.getLength();
        final List<String> synonyms = new ArrayList<>();

        // get one synonym and put array list
        for (int i = 0; i < count; i++) {
            Node node = nList.item(i);
            Element element2 = (Element) node;
            String str = element2.getChildNodes().item(1).getChildNodes().item(0).getNodeValue();
            String[] array = str.split("\\|");
            synonyms.addAll(Arrays.asList(array));
        }

        // show synonyms to list view
        // in at time use runOnUiThread, because only can change view in origin thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                list_synonyms.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, synonyms));
                list_synonyms.requestFocus();

                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edit_description.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);

            }
        });

    }



    /**
     * async task for calling web service
     */
    @SuppressLint("StaticFieldLeak")
    private class AsyncGetData extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            InputStream inputStream;
            try {
                /* create Apache HttpClient */
                HttpClient httpclient = new DefaultHttpClient();

                /* HttpGet Method */
                HttpGet httpGet = new HttpGet(params[0]);

                /* optional request header */
                httpGet.setHeader("Content-Type", "application/xml");

                /* optional request header */
                httpGet.setHeader("Accept", "application/xml");

                /* Make http request call */
                HttpResponse httpResponse = httpclient.execute(httpGet);

                int statusCode = httpResponse.getStatusLine().getStatusCode();
                /* 200 represents HTTP OK */
                if (statusCode ==  200) {
                    /* receive response as inputStream */
                    inputStream = httpResponse.getEntity().getContent();
                    doParseXML(inputStream);
                }
                progressDialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
                progressDialog.dismiss();
            }
            return 1;
        }
    }

}
