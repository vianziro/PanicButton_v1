package com.dobrowins.anon.panicpanic;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class WelcomeActivity extends AppCompatActivity {

    private EditText phoneNumberEdittext;
    public DatabaseHelper dbHelper;
    public String sqlStringNumber;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // checking - if number is already inserted, skip this activity
        sharedPreferences = this.getSharedPreferences("firstRun", MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean("firstRun", true); // creating sharedPreferences, setting it firstly that no first run ever happened

        if (isFirstRun) {
            setContentView(R.layout.activity_welcome);
        } else {
            // it is first run of application, starting mainactivity
            startActivity(new Intent(getBaseContext(), MainActivity.class));
        }
    }

    // saving data that has been inputed by the user
    public void SaveData(View view) {
        dbHelper = new DatabaseHelper(WelcomeActivity.this);

        phoneNumberEdittext = (EditText) findViewById(R.id.phoneNumberEdittext);

        // popup are you sure?
        AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
        builder.setMessage("Сохранить номер " + (phoneNumberEdittext.getText().toString()) + "?")
                // if YES
                .setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                                /*
                                * save number if it's:
                                *   not empty
                                *   not too short
                                */

                        if (!phoneNumberEdittext.getText().toString().equals("")
                                && phoneNumberEdittext.getText().toString().length() > 9) {
                            dbHelper.SaveNumber(phoneNumberEdittext.getText().toString());
                            GetOutOfHere();
                        } else {
                            Toast.makeText(WelcomeActivity.this, R.string.cantSave, Toast.LENGTH_LONG).show();
                            ((EditText) findViewById(R.id.phoneNumberEdittext)).setText(""); //clearing edittext
                        }
                    }
                })
                // if NO
                .setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((EditText) findViewById(R.id.phoneNumberEdittext)).setText(""); //clearing edittext
                    }
                })
                .create()
                .show();
    }

    //checking if data has been saved and opening other activity if it did
    public void GetOutOfHere() {
        if (dbHelper.CursorIsNotNull()) { //if data were inserted successfully
            // querying the inserted number from db
            Cursor cursor = dbHelper.getReadableDatabase()
                    .query("userInputTable", null, null, null, null, null, null);

            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                sqlStringNumber = cursor.getString(1); // getting inserted number from db; getString(0) would return number of row
                cursor.close();
            }
        } else { //if sqlStringNumber is null
            Toast.makeText(WelcomeActivity.this, R.string.numberNotSaved, Toast.LENGTH_LONG).show();
        }

        // firstRun must be set to false only if number is saved
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firstRun", false); // editing sharedPreferences with info that first run has happened
        editor.apply();

        // starting activity with panic button
        startActivity(new Intent(getBaseContext(), MainActivity.class));
        Toast.makeText(WelcomeActivity.this, ("Номер " + sqlStringNumber + " сохранен"), Toast.LENGTH_LONG).show();
        finish();
    }

    // showing instructions
    public void ShowInstructions(View view) {
        AlertDialog.Builder instructionsBuilder = new AlertDialog.Builder(WelcomeActivity.this);
        instructionsBuilder.setMessage(R.string.instruction)
                .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // ok then
                    }
                })
                .create()
                .show();
    }
}
