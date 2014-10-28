/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.pub;

import com.google.common.reflect.TypeToken;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Query pub.dartlang.org and get the information for the packages
 */
public class PubPackageManager {

  /**
   * Save package information from pub.dartlang to disk
   */
  private class PubPackageListWriter implements IPubPackageListener {

    @Override
    public void pubPackagesChanged(List<PubPackageObject> pubPackages) {
      Gson gson = new GsonBuilder().create();
      Type collectionType = new TypeToken<Collection<PubPackageObject>>() {
      }.getType();
      String json = gson.toJson(pubPackages, collectionType);
      File file = getPackagesFile();
      PrintWriter printWriter = null;
      try {
        printWriter = new PrintWriter(new FileWriter(file));
        printWriter.write(json);
      } catch (IOException e) {
        DartCore.logInformation("Exception while saving pub package info", e);
      } finally {
        if (printWriter != null) {
          printWriter.close();
        }
      }
    }
  }

  private static final PubPackageManager INSTANCE = new PubPackageManager();

  public static final PubPackageManager getInstance() {
    return INSTANCE;
  }

  private List<String> packagesList = new ArrayList<String>();

  private List<PubPackageObject> pubPackages = new ArrayList<PubPackageObject>();

  private PubPackageListWriter writer = new PubPackageListWriter();
  private final ListenerList listeners = new ListenerList();

  /**
   * Used to synchronize access to webPackages
   */
  private Object lock = new Object();

  private Job job;

  public PubPackageManager() {
    listeners.add(writer);
  }

  public void addListener(IPubPackageListener listener) {
    listeners.add(listener);
  }

  /**
   * Return a list containing the names of the packages on pub
   */
  public Collection<String> getPackageList() {
    if (packagesList.isEmpty()) {
      initialize();
    }
    synchronized (lock) {
      return new ArrayList<String>(packagesList);
    }
  }

  /**
   * Return an array containing the names of the packages on pub
   */
  public String[] getPackageListArray() {
    Collection<String> copy = getPackageList();
    return copy.toArray(new String[copy.size()]);
  }

  /**
   * Return an array of {@link PubPackageObject}, the packages on pub
   */
  public List<PubPackageObject> getPubPackages() {
    if (pubPackages.isEmpty()) {
      initialize();
    }
    synchronized (lock) {
      return new ArrayList<PubPackageObject>(pubPackages);
    }
  }

  public void initialize() {
    readPackagesFromFile();
    startPackageListFromPubJob();
  }

  public void notifyListeners(List<PubPackageObject> packages) {
    for (Object listener : listeners.getListeners()) {
      ((IPubPackageListener) listener).pubPackagesChanged(packages);
    }
  }

  public void removeListener(IPubPackageListener listener) {
    listeners.remove(listener);
  }

  public void startPackageListFromPubJob() {
    if (job == null || job.getState() == Job.NONE) {
      job = new Job("Get package list from pub") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            return fillPackageList(monitor);
          } catch (Exception e) {
            DartCore.logError(e);
          }
          return Status.OK_STATUS;
        }

      };
      job.setSystem(true);
      job.schedule(6000);
    }
  }

  public void stop() {
    if (job != null && !job.cancel()) {
      try {
        job.join();
      } catch (InterruptedException e) {
        // do nothing
      }
    }
  }

  /**
   * Get the data from pub.dartlang.org
   */
  private IStatus fillPackageList(IProgressMonitor monitor) throws Exception {

    int pageCount = 1;
    String line = null;
    JSONArray jsonArray = new JSONArray();

    for (int page = 1; page <= pageCount; page++) {
      URLConnection connection = getApiUrl2(page);
      InputStream is;
      try {
        is = connection.getInputStream();
      } catch (UnknownHostException e) {
        // No internet connection... just exit
        break;
      } catch (IOException e) {
        // server error ... just exit
        break;
      }
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      try {
        while ((line = br.readLine()) != null && !monitor.isCanceled()) {
          try {
            JSONObject object = new JSONObject(line);
            if (object != null) {
              pageCount = object.getInt("pages");
              JSONArray packages = (JSONArray) object.get("packages");
              jsonArray.put(packages);
            }
          } catch (JSONException e) {
            DartCore.logError(e);
          }
        }
      } finally {
        br.close();
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
    }
    return processData(jsonArray, monitor);
  }

  /**
   * pub.dartlang apiv2 - returns more info for each package
   */
  private URLConnection getApiUrl2(int page) throws IOException, MalformedURLException {
    return new URL("https://pub.dartlang.org/api/packages?page=" + page).openConnection();
  }

  private File getPackagesFile() {
    File file = DartCore.getPlugin().getStateLocation().append("pub_packages.json").toFile();
    return file;
  }

  // {"new_version_url":"http://pub.dartlang.org/api/packages/mongo_dart_query/versions/new",
  //  "name":"mongo_dart_query","uploaders_url":"http://pub.dartlang.org/api/packages/mongo_dart_query/uploaders",
  //  "latest":{"new_dartdoc_url":"http://pub.dartlang.org/api/packages/mongo_dart_query/versions/0.1.8/new_dartdoc",
  //            "pubspec":{"author":"Vadim Tsushko <vadimtsushko@gmail.com>","dev_dependencies":{"unittest":"any","browser":"any"},
  //                       "dependencies":{"bson":">=0.1.7 <2.0.0"},"description":"Query builder for mongo_dart and objectory",
  //                       "name":"mongo_dart_query","homepage":"https://github.com/vadimtsushko/mongo_dart_query","version":"0.1.8"},
  //            "archive_url":"http://pub.dartlang.org/packages/mongo_dart_query/versions/0.1.8.tar.gz",
  //            "package_url":"http://pub.dartlang.org/api/packages/mongo_dart_query",
  //            "url":"http://pub.dartlang.org/api/packages/mongo_dart_query/versions/0.1.8","version":"0.1.8"},
  //   "version_url":"http://pub.dartlang.org/api/packages/mongo_dart_query/versions/{version}",
  //   "url":"http://pub.dartlang.org/api/packages/mongo_dart_query"}
  //
  private IStatus processData(JSONArray jsonArray, IProgressMonitor monitor) {

    List<PubPackageObject> packageObjectList = new ArrayList<PubPackageObject>();
    List<String> packageNames = new ArrayList<String>();

    for (int j = 0; j < jsonArray.length(); j++) {
      JSONArray packages;
      String name = null;
      try {
        packages = jsonArray.getJSONArray(j);
        for (int i = 0; i < packages.length(); i++) {
          JSONObject o = new JSONObject(packages.getString(i));
          name = o.getString(PubspecConstants.NAME);
          Map<String, Object> pubspec = PubYamlUtils.parsePubspecYamlToMap(o.getJSONObject("latest").getString(
              "pubspec"));

          PubPackageObject obj = new PubPackageObject(
              name,
              (String) pubspec.get(PubspecConstants.DESCRIPTION),
              (String) pubspec.get(PubspecConstants.VERSION),
              o.getString("url"));
          packageObjectList.add(obj);
          packageNames.add(name);
        }
      } catch (JSONException e) {
        DartCore.logError(
            "Failed to process pub list response:  name=" + name + "\n" + jsonArray,
            e);
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
    }
    synchronized (lock) {
      pubPackages = packageObjectList;
      packagesList = packageNames;
    }
    notifyListeners(getPubPackages());
    return Status.OK_STATUS;
  }

  /**
   * Reads the packages information stored in metadata
   */
  private void readPackagesFromFile() {
    File file = getPackagesFile();

    if (file.exists()) {
      Gson gson = new GsonBuilder().create();
      Type collectionType = new TypeToken<Collection<PubPackageObject>>() {
      }.getType();
      Reader reader = null;
      try {
        reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        List<PubPackageObject> packages = gson.fromJson(reader, collectionType);
        List<String> packageNames = new ArrayList<String>();
        for (PubPackageObject p : packages) {
          packageNames.add(p.getName());
        }
        if (!packages.isEmpty()) {
          synchronized (lock) {
            pubPackages = packages;
            packagesList = packageNames;
          }
        }
      } catch (UnsupportedEncodingException e) {

      } catch (FileNotFoundException e) {

      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {

          }
        }
      }
    }

  }

}
