package com.example.eriklaptop.sunsetsunriset;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static android.content.Context.MODE_PRIVATE;

/**
 * This is currently coded using the api https://sunrise-sunset.org/api
 * I am not sure if using an internet connection or using this api is a problem, however there might be a way to calculate it
 * or use a pre-existing library to calculate it.
 */
public class SunriseSunsetWidget extends AppWidgetProvider
{
    private static final String mSharedPrefsFile = "com.example.sunrisesunshinewidget";
    private static final String LAT_KEY = "latitude";
    private static final String LONG_KEY = "longitude";
    private static final String SETUP_COMPLETE = "setup";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {

        SharedPreferences prefs = context.getSharedPreferences(mSharedPrefsFile, MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sunrise_sunset_widget);
        //getting the boolean to see if the setup is complete, to make sure the widget doesnt try and update before it has the data it needs to perform it's tasks
        Boolean setupIsComplete = prefs.getBoolean((SETUP_COMPLETE + appWidgetId), false);
        Float latitude = prefs.getFloat((LAT_KEY + appWidgetId), 0.0f);
        Float longitude = prefs.getFloat((LONG_KEY + appWidgetId), 0.0f);
        if(setupIsComplete)
        {
            //rounding the LAT and LONG to two decimals so it can be displayed better
            //the times are still calculated from the full un-rounded float
            views.setTextViewText(R.id.long_widget, Float.toString(round(longitude,2)));
            views.setTextViewText(R.id.lat_widget, Float.toString(round(latitude,2)));
            String todayUrl = getTodayUrl(latitude, longitude);
            String tomorrowUrl = getTomorrowUrl(latitude, longitude);
            new JsonTask(appWidgetManager, appWidgetId, views).execute(todayUrl);
            new JsonTask(appWidgetManager, appWidgetId, views).execute(tomorrowUrl);

        }
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    //because widgets are receivers this should update every 28800000 milliseconds, or 8 hours. You can change this time to be more or less in the info.xml file

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
    //an async task that gets the JSON data from the api
    private static class JsonTask extends AsyncTask<String, Void, String>
    {
        String urlString;
        private AppWidgetManager awm;
        private int awi;
        private RemoteViews rv;
        //private constructor that lets the variables be initialized
        private JsonTask(AppWidgetManager appwidgetManager, int appWidgetId, RemoteViews remoteViews)
        {
            awm = appwidgetManager;
            awi = appWidgetId;
            rv = remoteViews;
        }
        @Override
        protected String doInBackground(String... params)
        {
            //saves the url sent to a String to test if it was getting the data for today or tomorrow
            urlString = params[0];
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try
            {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder buffer = new StringBuilder();
                String line = "";
                while((line = reader.readLine()) !=null)
                {
                    buffer.append(line);
                    buffer.append("\n");
                }
                return buffer.toString();
            }
            catch(MalformedURLException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                if(connection != null)
                {
                    connection.disconnect();
                }
                try
                {
                    if (reader != null)
                    {
                        reader.close();
                    }

                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result)
        {
            //strings for all of the 4 times being displayed
            String sunriseTimeToday = "";
            String sunsetTimeToday = "";
            String sunriseTimeTomorrow = "";
            String sunsetTimeTomorrow = "";
            try
            {
                JSONObject reader = new JSONObject(result);
                JSONObject results = reader.getJSONObject("results");
                //this checks if it was today or tomorrow by checking the last letter in the URL (y for today, else for tomorrow)
                if(urlString.substring(urlString.length() - 1).equals("y"))
                {
                    sunriseTimeToday = formatDate(results.getString("sunrise"));
                    sunsetTimeToday = formatDate(results.getString("sunset"));
                }
                else
                {
                    sunriseTimeTomorrow = formatDate(results.getString("sunrise"));
                    sunsetTimeTomorrow = formatDate(results.getString("sunset"));
                }
            }
            catch(JSONException e)
            {
                e.printStackTrace();
                Log.e("JSONException" , "JSONException thrown");
            }
            catch(ParseException e)
            {
                e.printStackTrace();
                Log.e("ParseException", "ParseException thrown parsing the JSON data");
            }
            if(urlString.substring(urlString.length() - 1).equals("y")) {
                rv.setTextViewText(R.id.sunset_time_today_widget, sunsetTimeToday);
                rv.setTextViewText(R.id.sunrise_time_today_widget, sunriseTimeToday);
                awm.updateAppWidget(awi, rv);
            }
            else
            {
                rv.setTextViewText(R.id.sunset_time_tomorrow_widget, sunsetTimeTomorrow);
                rv.setTextViewText(R.id.sunrise_time_tomorrow_widget, sunriseTimeTomorrow);
                awm.updateAppWidget(awi, rv);
            }

        }
    }
    /**
     * @param utcTime The time in UTC to be converted to the local time set in the devices settings
     */
    private static String formatDate(String utcTime) throws ParseException
    {
        //change this format to get it in either 24hour time, or to change however it is formatted
        String DATE_FORMAT = "HH:mm";
        String strDate = "";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        try{
            TimeZone utcZone = TimeZone.getTimeZone("UTC");
            sdf.setTimeZone(utcZone);
            Date myDate = sdf.parse(utcTime);
            sdf.setTimeZone(TimeZone.getDefault());
            //this checks if the users currently in daylight savings, and if they are it adds 1 hours
            //this is because the api does not format for daylight savings times
            if(isInDST())
            {
                Calendar c = Calendar.getInstance();
                c.setTime(myDate);
                c.add(Calendar.HOUR, 1);
                myDate = c.getTime();

            }
            strDate = sdf.format(myDate);
            return strDate;
        }
        catch(ParseException e)
        {
            e.printStackTrace();
        }
        return strDate;
    }

    /**
     * Checks to see if the user is currently in daylight savings time or not
     * @return false if not in daylight savings, true if in daylight savings
     */
    private static boolean isInDST()
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTime(Calendar.getInstance().getTime());
        return calendar.get(Calendar.DST_OFFSET) != 0;
    }
    private static String getTodayUrl(float latitude, float longitude)
    {
        //https://api.sunrise-sunset.org/json?lat=40&lng=-40&date=today
        StringBuilder s = new StringBuilder();
        s.append("https://api.sunrise-sunset.org/json?lat=");
        s.append(latitude);
        s.append("&lng=");
        s.append(longitude);
        s.append("&date=today");
        return s.toString();
    }
    private static String getTomorrowUrl(float latitude, float longitude)
    {
        //https://api.sunrise-sunset.org/json?lat=41.955509&lng=-88.389255&date=tomorrow
        StringBuilder s = new StringBuilder();
        s.append("https://api.sunrise-sunset.org/json?lat=");
        s.append(latitude);
        s.append("&lng=");
        s.append(longitude);
        s.append("&date=tomorrow");
        return s.toString();
    }
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}

