package com.example.VBScheckin;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity2 extends AppCompatActivity {
    private String Submission_ID;
    private String day = "Day 1";
    private int toggle = 0; // 0 = checkin | 1 = checkout


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        //String Submission_ID;
        String Child_First_Name = "";
        String Child_Last_Name = "";
        String Child_name = "";
        //String Grade;
        String Classroom = "";
        String Special_Needs_Allergies = "";
        String Inhaler = "";

        day = whatDayIsIt();

        TextView name = findViewById(R.id.name);
        TextView classroom = findViewById(R.id.classroom);
        TextView medical_notes = findViewById(R.id.medical);

        Button confirmButton = (Button)findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(confirm);
        Button cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(cancel);

        name.setText(Child_name);
        classroom.setText(Classroom);
        medical_notes.setText(Special_Needs_Allergies);

        Intent intent = getIntent();
        if (null != intent) { //Null Checking
            String jsonString = intent.getStringExtra("jsonString");
            try {
                assert jsonString != null;
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray arr = jsonObject.getJSONArray("values");
                Submission_ID = arr.getJSONObject(0).getString("Submission ID");
                Child_First_Name = arr.getJSONObject(0).getString("Child First Name");
                Child_Last_Name = arr.getJSONObject(0).getString("Child Last Name");
                Child_name = Child_First_Name +" " + Child_Last_Name;
                Classroom = arr.getJSONObject(0).getString("Classroom");
                Special_Needs_Allergies = arr.getJSONObject(0).getString("Special Needs / Allergies");
                Inhaler = arr.getJSONObject(0).getString("Is the child prescribed an inhaler? If yes, please explain any instructions.");

                String checkIn = arr.getJSONObject(0).getString(day + " Check-in");
                String checkOut = arr.getJSONObject(0).getString(day + " Check-out");

                if(checkIn.isEmpty()) {
                    confirmButton.setText("Check-In");
                }
                else if (checkOut.isEmpty()) {
                    confirmButton.setText("Check-out");
                    toggle = 1;
                }
                else  {
                    String[] checkInSplit = checkIn.split(" ");
                    String[] checkOutSplit = checkOut.split(" ");
                    String checkInTime = checkInSplit[1];
                    String checkOutTime = checkOutSplit[1];
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    Date d1 = sdf.parse(checkInTime);
                    Date d2 = sdf.parse(checkOutTime);

                    assert d2 != null;
                    if (d2.after(d1)){
                        confirmButton.setText("Check-In");
                        toggle = 0;
                    }
                    else {
                        confirmButton.setText("Check-Out");
                        toggle = 1;
                    }
                }

                name.setText(Child_name);
                classroom.setText(Classroom);
                medical_notes.setText("Medical: " + Special_Needs_Allergies + "\n" + "Inhaler: " + Inhaler);

            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON", e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String whatDayIsIt() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean("day1", false)) {
            return "Day 1";
        } else if (sharedPreferences.getBoolean("day2", false)) {
            return "Day 2";
        } else if (sharedPreferences.getBoolean("day3", false)) {
            return "Day 3";
        } else if (sharedPreferences.getBoolean("day4", false)) {
            return "Day 4";
        } else if (sharedPreferences.getBoolean("day5", false)) {
            return "Day 5";
        } else {
            return "Day 1";
        }
    }

    private View.OnClickListener confirm = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(toggle == 0){
                checkIn();
            }
            else {
                checkOut();
            }
        }
    };

    private View.OnClickListener cancel = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    private void checkOut() {
        String url = String.format("https://sheetdb.io/api/v1/0e9lvobrvl3fc/Submission%%20ID/%s?sheet=2024+VBS+Master+List", Submission_ID);

        // Create the JSON payload
        JSONObject jsonBody = new JSONObject();
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            jsonBody.put(day + " Check-out", dtf.format(now));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in checkOut: ", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PATCH, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(MainActivity2.this, "Checked-out", Toast.LENGTH_SHORT).show();
                        // Log the raw JSON response
                        Log.d(TAG, "Raw JSON response: " + response.toString());

                        Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error response: ", error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Add the request to the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private void checkIn() {
        String url = String.format("https://sheetdb.io/api/v1/{SHEETSDB KEY}/Submission%%20ID/%s?sheet=2024+VBS+Master+List", Submission_ID);

        // Create the JSON payload
        JSONObject jsonBody = new JSONObject();
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            jsonBody.put(day + " Check-in", dtf.format(now));
        } catch (JSONException e) {
            Log.e(TAG, "JSONException in checkIn: ", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PATCH, url, jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(MainActivity2.this, "Checked-in", Toast.LENGTH_SHORT).show();
                        // Log the raw JSON response
                        Log.d(TAG, "Raw JSON response: " + response.toString());

                        Intent intent = new Intent(MainActivity2.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error response: ", error);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // Add the request to the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}