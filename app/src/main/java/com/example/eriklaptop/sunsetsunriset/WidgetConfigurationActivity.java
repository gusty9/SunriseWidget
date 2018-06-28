package com.example.eriklaptop.sunsetsunriset;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class WidgetConfigurationActivity extends Activity
{
    EditText longitudeEditText;
    EditText latitudeEditText;
    private static final String mSharedPrefsFile = "com.example.sunrisesunshinewidget";
    private static final String LAT_KEY = "latitude";
    private static final String LONG_KEY = "longitude";
    private static final String SETUP_COMPLETE = "setup";
    int appWidgetId;
    SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstances)
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //requesting permission to location because we currently do not have it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        super.onCreate(savedInstances);
        setContentView(R.layout.widget_configuration_activity);
        setResult(RESULT_CANCELED);
        prefs = getSharedPreferences(mSharedPrefsFile, 0);
        longitudeEditText = (EditText) findViewById(R.id.longitude_edittext);
        latitudeEditText = (EditText) findViewById(R.id.latitude_edittext);
        Button okayButton = (Button)findViewById(R.id.widget_okay_button);
        okayButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                float latitude = -200.00f;
                float longitude = -200.00f;
                if(longitudeEditText.getText().length() != 0 || latitudeEditText.getText().length() !=0)
                {
                    latitude = Float.parseFloat(latitudeEditText.getText().toString());
                    longitude = Float.parseFloat(longitudeEditText.getText().toString());
                }
                //checking to see if the entered lat and long is actually a real long and lat
                if((latitude> 90 || latitude < -90) || (longitude>180 || longitude < -180))
                {
                    Toast.makeText(getApplicationContext(), "The Latitude or Longitude is not valid", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    setAppWidgetId();
                    SharedPreferences.Editor prefsEditor = prefs.edit();
                    prefsEditor.putFloat((LAT_KEY + appWidgetId), latitude);
                    prefsEditor.putFloat((LONG_KEY + appWidgetId), longitude);
                    //passing a boolean to see if the setup is complete, it would sometimes update without the activity being complete and this prevents that
                    prefsEditor.putBoolean((SETUP_COMPLETE + appWidgetId), true);
                    prefsEditor.apply();
                    showAppWidget();
                }
            }
        });
        Button useCurrentLocationButton = (Button) findViewById(R.id.widget_use_current_location_button);
        useCurrentLocationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {
                    //requesting permission to location because we currently do not have it
                    ActivityCompat.requestPermissions(WidgetConfigurationActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                }
                float latitude = -200.00f;
                float longitude = -200.00f;
                float[] latAndLong = getLocation();
                if(latAndLong == null)
                {
                    Toast.makeText(getApplicationContext(), "Unable to get your location", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    latitude = latAndLong[0];
                    longitude = latAndLong[1];
                    if((latitude> 90 || latitude < -90) || (longitude>180 || longitude < -180))
                    {
                        Toast.makeText(getApplicationContext(), "Unable to get your location", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        setAppWidgetId();
                        SharedPreferences.Editor prefsEditor = prefs.edit();
                        prefsEditor.putFloat(LAT_KEY + appWidgetId, latitude);
                        prefsEditor.putFloat(LONG_KEY + appWidgetId, longitude);
                        prefsEditor.putBoolean(SETUP_COMPLETE + appWidgetId, true);
                        prefsEditor.apply();
                        showAppWidget();
                    }

                }

            }
        });
    }
    private void setAppWidgetId()
    {
        //setting the appWidgetId to an invalid id so we can test if it was correctly set
        appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        //retrieving the app widget ID from the intent
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null)
        {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID , AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        //check and see if we have a valid widget ID, if not we bail
        if(appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)
        {
            finish();
        }
    }

    private void showAppWidget()
    {

        //call the onUpdate method from the widgets java class, using a configuration file does not call this method on startup like it usually does
        Context context = WidgetConfigurationActivity.this;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        SunriseSunsetWidget.updateAppWidget(context, appWidgetManager, appWidgetId);

        //creating the return intent
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
    private float[] getLocation()
    {
        //element 1 will be latitude, element 2 will be longitude
        float[] latAndLongFloat = null;
        //checking if we have permission to use the users location
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            //requesting permission to location because we currently do not have it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else
        {
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(location!=null)
            {
                latAndLongFloat = new float[2];
                latAndLongFloat[0] = (float) location.getLatitude();
                latAndLongFloat[1] = (float) location.getLongitude();
            }
            else{
                location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location!=null)
                {
                    latAndLongFloat[0] = (float) location.getLatitude();
                    latAndLongFloat[1] = (float) location.getLongitude();
                }
            }
        }
        return latAndLongFloat;

    }
}
