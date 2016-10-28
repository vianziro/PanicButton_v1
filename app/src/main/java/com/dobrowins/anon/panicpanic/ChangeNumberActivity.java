package com.dobrowins.anon.panicpanic;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChangeNumberActivity extends AppCompatActivity {

    private EditText changeOrAddEditText;
    public String sqlStringNumber;
    public DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_number);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // saving data that has been inputed by the user
    public void SaveData(View view) {
        dbHelper = new DatabaseHelper(ChangeNumberActivity.this);

        changeOrAddEditText = (EditText) findViewById(R.id.changeOrAddEditText);

        AlertDialog.Builder builder = new AlertDialog.Builder(ChangeNumberActivity.this);
        builder.setMessage("Изменить номер на " + (changeOrAddEditText.getText().toString()) + "?")
                .setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // checking how user inputed number and then editing it if needed for call and sms to happen
                        char charSeven = '7';
                        char charPlus = '+';
                        char charEight= '8';

                                /*
                                * save number if it's:
                                *   not null
                                *   not too short
                                */

                        if (!changeOrAddEditText.getText().toString().equals("")
                                        && changeOrAddEditText.getText().toString().length() > 9) {
                            dbHelper.SaveNumber(changeOrAddEditText.getText().toString());
                            GetOutOfHere();
                        } else {
                            ((EditText) findViewById(R.id.changeOrAddEditText)).setText(""); //clearing edittext
                            Toast.makeText(ChangeNumberActivity.this, R.string.cantSave, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ((EditText) findViewById(R.id.changeOrAddEditText)).setText(""); //clearing edittext
                    }
                })
                .create()
                .show();
    }

    //checking if data has been saved and opening other activity if it did
    private void GetOutOfHere() {
        if (dbHelper.CursorIsNotNull()) {// if data were inserted successfully
            // querying the inserted number from db
            Cursor cursor = dbHelper.getReadableDatabase()
                    .query("userInputTable", null, null, null, null, null, null);

            if (cursor != null && cursor.getCount() != 0) {
                cursor.moveToFirst();
                sqlStringNumber = cursor.getString(1); // достаем введенный номер из бд; getString(0) возвратит номер строки
                cursor.close();
            }
        } else { // if sqlStringNumber is null
            Toast.makeText(ChangeNumberActivity.this, "Не удалось сохранить номер в базу данных", Toast.LENGTH_LONG).show();
        }

        Toast.makeText(ChangeNumberActivity.this, ("Номер " + sqlStringNumber + " сохранен"), Toast.LENGTH_LONG).show();
        // starting intent with panic button
        startActivity(new Intent(getBaseContext(), MainActivity.class));
    }

    // this finishes the activity as back arrow in appbar is clicked
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}
