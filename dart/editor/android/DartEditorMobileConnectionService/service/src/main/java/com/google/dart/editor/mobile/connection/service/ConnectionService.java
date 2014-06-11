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
        log("Test mobile connection to developer machine - UI");
        URL url = getUrlToTest(intent);
        prefix = intent.getStringExtra("prefix");
        log("Prefix " + prefix);
        if (prefix == null) {
            prefix = "com.google.dart.editor.mobile.connection.service.msg";
        }
        if (url != null) {
            log("Test connection: " + url);
            new AsyncTask<URL, Void, String>() {
                @Override
                protected String doInBackground(URL... urls) {
                    URL url = urls[0];
                    log("Test connection in background: " + url);
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
        log("UriToTest: " + uri);
        if (uri == null) {
            return null;
        }
        try {
            return new URL(uri.toString());
        } catch (MalformedURLException e) {
            logError(e);
            return null;
        }
    }

    private URLConnection makeRequest(URL url) {
        log("OpenConnection: " + url);
        try {
            return url.openConnection();
        } catch (IOException e) {
            logError(e);
            return null;
        }
    }

    private String getResponse(URLConnection connection) {
        if (connection == null) {
            return null;
        }
        log("Processing response");
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (Throwable e) {
            logError(e);
            return null;
        }
        if (inputStream == null) {
            log("response stream is null");
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder str = new StringBuilder();
        while (true) {
            String line = null;
            try {
                line = reader.readLine();
            } catch (IOException e) {
                logError(e);
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
            logError(e);
        }
        return str.toString();
    }

    /**
     * Forward the exception to the client via stdout.
     */
    private void logError(Throwable e) {
        log("Error: " + e.toString());
    }

    /**
     * Forward the message to the client via stdout.
     */
    private void log(String message) {
        System.out.println(prefix + ": " + message);
    }
}
