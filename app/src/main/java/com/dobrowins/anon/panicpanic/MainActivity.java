package com.dobrowins.anon.panicpanic;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    // misc variables
    private TextView yourCoord;
    private Button panicButton;
    private String helperPhoneNumber;
    private static final int REQUEST_CODE_LOCATION = 2;
    private static final int REQUEST_SEND_SMS = 4;
    private static final int REQUEST_ALERT_WINDOW = 6;

    // GoogleApiClient & location variables
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private TextView coordinatesText;
    private Location lastLocation;
    private double myLatitude;
    private double myLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        coordinatesText = (TextView) findViewById(R.id.coordinatesText);
        yourCoord = (TextView)  findViewById(R.id.yourCoord);
        panicButton = (Button)  findViewById(R.id.panicButton);

        // creating googleApiClient
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API) //adding Location.API
                .build();

        // LocationRequest requred for periodic request of location
        locationRequest = new LocationRequest();
        locationRequest
                .setInterval(15 * 1000)
                .setFastestInterval(30 * 1000)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        panicButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // requesting permission for overlay if needed
                if (Build.VERSION.SDK_INT >= 23) {
                    if (!Settings.canDrawOverlays(MainActivity.this)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, REQUEST_ALERT_WINDOW);
                    }
                }

                GetCoordinates();
                SendSms();

                return false;

            } //end of onLongClick
        });
    }

    private void GetCoordinates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // requesting permissions if required
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
        } else {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }
    } 

    // sending sms after onLongClick
    private void SendSms() {
        if (lastLocation != null) {
            Toast.makeText(MainActivity.this, "Высылаю sms...", Toast.LENGTH_SHORT).show();

            myLatitude = lastLocation.getLatitude();
            myLongitude = lastLocation.getLongitude();

            try {
                DatabaseHelper databaseHelper = new DatabaseHelper(this);

                // querying the inserted number from db
                Cursor cursor = databaseHelper.getReadableDatabase()
                        .query("userInputTable", null, null, null, null, null, null);

                if (cursor != null && cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    helperPhoneNumber = cursor.getString(1); //  getting phone number from db; getString(0) would return row number
                    cursor.close();
                }

                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                }
                else { // if sms permission is granted
                    // for cyrillic messages arrayList is more convenient as all needed info might not fit in one sms
                    ArrayList<String> arrayMsg = new ArrayList<String>();
                    arrayMsg.add("Мне нужна помощь!\n");
                    arrayMsg.add(("Я сейчас нахожусь - " + String.valueOf(myLatitude) + ", " + String.valueOf(myLongitude) + "\n"));
                    arrayMsg.add("Ссылка - http://maps.google.com/maps?daddr=" + String.valueOf(myLatitude) + "," + String.valueOf(myLongitude));

                    // pendingIntent for checking if sms is actually sent
                    String SMS_SENT = "SMS_SENT";
                    PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
                    ArrayList<PendingIntent> sentPIArrayList = new ArrayList<PendingIntent>();
                    sentPIArrayList.add(sentPI);

                    registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            switch (getResultCode()) {

                                case Activity.RESULT_OK:
                                    Toast.makeText(getBaseContext(), R.string.helpIsOnTheWay, Toast.LENGTH_SHORT).show();

                                    if (lastLocation != null) {
                                        // making textViews with geolocation visible
                                        yourCoord.setVisibility(View.VISIBLE);
                                        coordinatesText.setVisibility(View.VISIBLE);
                                        coordinatesText.setText(String.valueOf(lastLocation.getLatitude()) + ", " + String.valueOf(lastLocation.getLongitude()));
                                    }
                                    //changing text of button itself
                                    panicButton.setText(R.string.panicButtonClicked);
                                    break;

                                case SmsManager.RESULT_ERROR_NO_SERVICE:
                                    Toast.makeText(context, "Сервис не доступен", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    }, new IntentFilter(SMS_SENT));

                    SmsManager.getDefault()
                            .sendMultipartTextMessage(helperPhoneNumber, null, arrayMsg, sentPIArrayList, null);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {// if lastLocation is null
            Toast.makeText(this, R.string.lastLocationIsNull, Toast.LENGTH_LONG).show();
        }
    } // end of SendSms()

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId(); // all the items is in the menu_main.xml

        // handling menu items clicks
        switch (id) {
//            case R.id.moneymoneymoney:
//                Toast.makeText(MainActivity.this, "TBD", Toast.LENGTH_SHORT).show();
//                return true;

            case R.id.feedback:
                // creating email intent, it's implicit as it is not known which email app is used
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822"); // message/rfc822 значит email-only
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"dobrowins@gmail.com"}); // recipients
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "О приложении PanicButton");
                startActivity(Intent.createChooser(emailIntent, "Share via")); //меню выбора метода связи
                return true;

            case R.id.changeOrAddNumber:
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Вы хотите сменить номер адресата?")
                        .setPositiveButton(R.string.YES, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(getBaseContext(), ChangeNumberActivity.class));
                            }
                        })
                        .setNegativeButton(R.string.NO, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // no means no
                            }
                        })
                        .create()
                        .show();
                return true;

            case R.id.instructions:
                final AlertDialog.Builder menuInstructionsBuilder = new AlertDialog.Builder(MainActivity.this);
                menuInstructionsBuilder.setMessage(R.string.instruction)
                        .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // ok then
                            }
                        })
                        .create()
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * if the permission wasn’t granted by the user, the requestPermissions method was called to ask the user to grant them.
     * The response from the user is captured in the onRequestPermissionsResult callback.
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ALERT_WINDOW: {
                // If request is cancelled, the result arrays are empty
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was denied or request was cancelled
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                            // display UI with rationale
                            Snackbar.make(findViewById(R.id.mainActivityContainer),
                                    R.string.overlayRationale,
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.YES, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, REQUEST_ALERT_WINDOW);
                                        }
                                    })
                                    .setAction(R.string.NO, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            //
                                        }
                                    })
                                    .show();
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.SYSTEM_ALERT_WINDOW}, REQUEST_ALERT_WINDOW);
                        }
                    }
                }
            }
            break;

            case REQUEST_CODE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was denied or request was cancelled
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.ACCESS_FINE_LOCATION)) {
                            // display UI with rationale
                            Snackbar.make(findViewById(R.id.mainActivityContainer),
                                    R.string.locationRationale,
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.YES, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
                                        }
                                    })
                                    .setAction(R.string.NO, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            finish();
                                        }
                                    })
                                    .show();
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
                        }
                    } else {
                        // permission has been granted, proceed as usual
                        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    }
                }
            }
            break;

            case REQUEST_SEND_SMS: {
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                Manifest.permission.SEND_SMS)) {
                            // display UI with rationale
                            Snackbar.make(findViewById(R.id.mainActivityContainer),
                                    R.string.smsRationale,
                                    Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.YES, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                                        }
                                    })
                                    .setAction(R.string.NO, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            finish();
                                        }
                                    })
                                    .show();
                        } else {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                        }
                    } else {
                        // permission has been granted, proceed as usual
                        SendSms();
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        coordinatesText.setVisibility(View.INVISIBLE);
        yourCoord.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (lastLocation != null) {
            myLatitude = lastLocation.getLatitude();
            myLongitude = lastLocation.getLongitude();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.onConnectionFailed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        coordinatesText.setText(String.valueOf(location.getLatitude()) + ", " + String.valueOf(location.getAltitude()));
    }
}
