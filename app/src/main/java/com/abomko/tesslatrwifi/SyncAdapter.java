/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abomko.tesslatrwifi;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Define a sync adapter for the app.
 * <p>
 * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 * <p>
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "SyncAdapter";

    /**
     * URL to fetch content from during a sync.
     * <p>
     * <p>This points to the Android Developers Blog. (Side note: We highly recommend reading the
     * Android Developer Blog to stay up to date on the latest Android platform developments!)
     */
    private static final String PROVIDER_URL = "http://my.tessla.com.ua";

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;
    private SharedPreferences prefs;

    private static CookieManager cookieManager = new CookieManager();


    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(
                this.getContext()
        );
        Log.i(TAG, "Beginning network synchronization");
        CookieHandler.setDefault(cookieManager);
        Log.i(TAG, "Streaming data from network");

        boolean authenticated = this.getDevices();
        if (!authenticated) {
            authenticated = this.authenticateUser();
            if (authenticated) {
                 authenticated = this.getDevices();
            }
        }
        if (authenticated) {
            int devices = this.prefs.getInt("device_count", 0);
            for (int i=1; i<=devices; i++) {
                String deviceId = this.prefs.getString("device_id_" + i, "");
                if (!deviceId.isEmpty()) {
                    String[] temperatures = this.readData(deviceId);
                    if (temperatures.length == 2) {
                        String oldValue = prefs.getString("temperature_" + i, "");
                        if (!oldValue.isEmpty()) {
                            prefs.edit().putString("temperature_" + i + "_prev", oldValue).apply();
                        }
                        prefs.edit().putString("temperature_" + i, String.valueOf(temperatures[0])).apply();
                        prefs.edit().putString("temperature_" + i + "_switch", String.valueOf(temperatures[1])).apply();
                        Log.i(TAG, "Temperature is: " + String.valueOf(temperatures[0]));
                    }
                }
            }
        } else {
            syncResult.stats.numIoExceptions++;
        }

        DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss", Locale.getDefault());
        String date = df.format(Calendar.getInstance().getTime());
        Log.i(TAG, date + " Network synchronization complete");
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    private HttpURLConnection makeRequest(
            final URL url,
            final Map<String, String> requestParams,
            Boolean isPost
    ) throws IOException {
        String postData = "";
        Boolean first = true;
        for (Map.Entry<String, String> entry : requestParams.entrySet()) {
            postData += (first ? "" : "&") + URLEncoder.encode(entry.getKey(), "UTF-8");
            postData += "=" + URLEncoder.encode(entry.getValue(), "UTF-8");
            first = false;
        }

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
        urlConnection.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
        urlConnection.setDoInput(true);
        urlConnection.setChunkedStreamingMode(0);
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setInstanceFollowRedirects(false);
        if (isPost) {
            urlConnection.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(postData);
            out.flush();
            out.close();
        }

        return urlConnection;
    }

    @NonNull
    private Boolean authenticateUser()
    {
        String login = this.prefs.getString("login", "--");
        String password = this.prefs.getString("password", "--");
        if (login.isEmpty() || password.isEmpty()) {
            return false;
        }
        try {
            final URL authUrl = new URL(PROVIDER_URL + "/?login=yes");
            Map<String, String> postData = new HashMap<String, String>();

            postData.put("backurl", "/");
            postData.put("AUTH_FORM", "Y");
            postData.put("TYPE", "AUTH");
            postData.put("USER_LOGIN", login);
            postData.put("USER_PASSWORD", password);
            postData.put("Login", "Войти");
            HttpURLConnection urlConnection = makeRequest(authUrl, postData, true);

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            try {
                // Read Server Response
                while((line = reader.readLine()) != null)
                {
                    // Append server response in string
                    sb.append(line).append("\n");
                }
            } finally {
                    reader.close();
            }

            int status = urlConnection.getResponseCode();


            String text = sb.toString();

            if (status>= 300 && status<= 399) {
                String location = urlConnection.getHeaderField("Location");
                return location.matches(PROVIDER_URL + "(:80)?/devices/");
                //text = (String) this.makeRequest(new URL(location), new HashMap<String, String>(), false);
            }
            String stringResult = sb.toString();
        } catch (MalformedURLException e) {
            Log.e(TAG, "Feed URL is malformed", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            return false;
        }

        return true;
    }

    @NonNull
    private String[] readData(String deviceId)
    {
        String[] result = new String[2];
        try {
            final URL apiUrl = new URL(PROVIDER_URL + "/ajax/get_items_data.php");
            Map<String, String> postData = new HashMap<String, String>();

            postData.put("list_id[]", deviceId);
            postData.put("sessid", this.prefs.getString("sessid", ""));

            HttpURLConnection urlConnectionApi = makeRequest(apiUrl, postData, true);
            BufferedReader readerApi = new BufferedReader(new InputStreamReader(urlConnectionApi.getInputStream()));
            StringBuilder sbApi = new StringBuilder();
            String lineApi = null;

            try {
                // Read Server Response
                while((lineApi = readerApi.readLine()) != null)
                {
                    // Append server response in string
                    sbApi.append(lineApi).append("\n");
                }
            } finally {
                readerApi.close();
            }

            int statusApi = urlConnectionApi.getResponseCode();

            String textApi = sbApi.toString();
            JSONArray jsonReader = new JSONArray(textApi);
            JSONObject values  = jsonReader.getJSONObject(0);
            result[0] = values.getString("PROPERTY_T_U_VALUE");
            result[1] = values.getString("PROPERTY_SET_TEMPERATURE_VALUE");
        } catch (MalformedURLException e) {
            Log.e(TAG, "Feed URL is malformed", e);
            return result;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            return result;
        } catch (JSONException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            return result;
        }

        return result;
    }

    private boolean getDevices()
    {
        Map<String, String> devices = new HashMap<String, String>();

        try {
            final URL sessidUrl = new URL(PROVIDER_URL + "/devices/");
            HttpURLConnection urlConnection = makeRequest(sessidUrl, new HashMap<String, String>(), false);

            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line = null;

            try {
                // Read Server Response
                while((line = reader.readLine()) != null)
                {
                    // Append server response in string
                    sb.append(line).append("\n");
                }
            } finally {
                reader.close();
            }

            int status = urlConnection.getResponseCode();

            String text = sb.toString();
            String sessid = "";
            Pattern pattern = Pattern.compile("sessid:\"([a-z0-9]+)\"");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                sessid = matcher.group(1);
            }

            if (sessid.isEmpty()) {
                return false;
            }
            this.prefs.edit().putString("sessid", sessid).apply();
            pattern = Pattern.compile("data-device-id=\"([0-9]+)\"");
            matcher = pattern.matcher(text);
            int i = 0;
            while (matcher.find()) {
                i++;
                this.prefs.edit().putString("device_id_" + i, matcher.group(1)).apply();
                devices.put(matcher.group(1), "" + i);
            }
            this.prefs.edit().putInt("device_count", i).apply();
            Document doc = Jsoup.parse(text);
            Elements elements = doc.select("[data-device-id] td a[href~=/devices/]");
            for (Element element: elements) {
                String id = element.parent().parent().attr("data-device-id");
                this.prefs.edit().putString("device_label_" + devices.get(id), element.text()).apply();
            }

        } catch (MalformedURLException e) {
            Log.e(TAG, "Feed URL is malformed", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            return false;
        }
        return true;
    }
}
