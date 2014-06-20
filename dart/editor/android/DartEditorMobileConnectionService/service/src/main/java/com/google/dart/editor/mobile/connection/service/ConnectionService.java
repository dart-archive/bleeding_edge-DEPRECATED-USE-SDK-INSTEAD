package com.google.dart.editor.mobile.connection.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ConnectionService extends Service {
    private String prefix;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        URL url = getUrlToTest(intent);
        prefix = intent.getStringExtra("prefix");
        if (prefix == null) {
            prefix = "com.google.dart.editor.mobile.connection.service.msg";
        }
        if (url != null) {
            new AsyncTask<URL, Void, String>() {
                @Override
                protected String doInBackground(URL... urls) {
                    URL url = urls[0];
                    String content = getResponse(makeRequest(url));
                    if (content != null) {
                        log(content);
                        log("Success");
                    }
                    return null;
                }
            }.execute(url);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException();
    }

    private URL getUrlToTest(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            return null;
        }
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            logError(null, e);
            return null;
        }
    }

    private URLConnection makeRequest(URL url) {
        log("Test connection: " + url);
        try {
          URLConnection connection = url.openConnection();
          connection.setRequestProperty
                                ("User-Agent", "com.google.dart.editor.mobile.connection.service");
          return connection;
        } catch (IOException e) {
            logError(null, e);
            return null;
        }
    }

    private String getResponse(URLConnection connection) {
        if (connection == null) {
            return null;
        }
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            logError("No response from server", e);
            return null;
        } catch (Throwable e) {
            logError(null, e);
            return null;
        }
        if (inputStream == null) {
            logError("Response stream is null", null);
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder str = new StringBuilder();
        while (true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                logError("Failed to get server response", e);
                break;
            }
            if (line == null) {
                break;
            }
            str.append(line);
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            logError("Failed to close response stream", e);
        }
        return str.toString();
    }

    private void logError(String message, Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error:");
        if (message != null) {
            sb.append(" ");
            sb.append(message);
            if (e != null) {
                sb.append(":");
            }
        }
        if (e != null) {
            sb.append(" ");
            sb.append(e.toString());
        }
        log(sb.toString());
    }

    private void log(String message) {
        System.out.println(prefix + ": " + message);
    }
}
