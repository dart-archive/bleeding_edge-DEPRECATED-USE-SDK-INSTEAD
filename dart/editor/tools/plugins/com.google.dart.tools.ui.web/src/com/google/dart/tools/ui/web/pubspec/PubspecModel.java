/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.web.pubspec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model representing the pubspec
 */
public class PubspecModel {

  private static String EMPTY_STRING = "";

  private ArrayList<IModelListener> modelListeners;

  private String name = EMPTY_STRING;
  private String version;
  private String description;
  private String author;
  private String homepage;
  private ArrayList<DependencyObject> dependencies;

  private boolean isDirty = false;

  public PubspecModel(String contents) {
    modelListeners = new ArrayList<IModelListener>();
    dependencies = new ArrayList<DependencyObject>();
    initialize(contents);
  }

  public void add(DependencyObject[] objs, String eventType) {
    for (int i = 0; i < objs.length; i++) {
      dependencies.add(objs[i]);
      objs[i].setModel(this);
    }

    fireModelChanged(objs, eventType);
  }

  public void addModelListener(IModelListener listener) {
    if (!modelListeners.contains(listener)) {
      modelListeners.add(listener);
    }
  }

  public void fireModelChanged(Object[] objects, String type) {
    for (int i = 0; i < modelListeners.size(); i++) {
      modelListeners.get(i).modelChanged(objects, type);
    }
  }

  public String getAuthor() {
    return author;
  }

  /**
   * Return the model contents as a yaml string
   */
  public String getContents() {
    return SnakeYamlUtils.buildYamlString(convertModelToObject());
  }

  public Object[] getDependecies() {
    return dependencies.toArray();
  }

  public String getDescription() {
    return description;
  }

  public String getHomepage() {
    return homepage;
  }

  public String getName() {
    return name;
  }

  public String getVersion() {
    return version;
  }

  /**
   * Clear and initialize the model with the values in the yaml string
   */
  public void initialize(String yamlString) {
    if (yamlString != null) {
      clearModelFields();
      setValuesFromObject(SnakeYamlUtils.parsePubspecYamlToObject(yamlString));
    }
  }

  public boolean isDirty() {
    return isDirty;
  }

  public void remove(DependencyObject[] dependencyObjects, boolean notify) {
    for (int i = 0; i < dependencyObjects.length; i++) {
      dependencies.remove(dependencyObjects[i]);
      dependencyObjects[i].setModel(null);
    }
    if (notify) {
      fireModelChanged(dependencyObjects, IModelListener.REMOVED);
    }
  }

  public void removeModelListener(IModelListener listener) {
    modelListeners.remove(listener);
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setDirty(boolean isDirty) {
    this.isDirty = isDirty;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  private void clearModelFields() {
    isDirty = false;
    name = version = description = homepage = author = EMPTY_STRING;
    dependencies.clear();
  }

  /**
   * Convert the model contents to {@link PubYamlObject}
   */
  private PubYamlObject convertModelToObject() {
    PubYamlObject pubYamlObject = new PubYamlObject();

    pubYamlObject.name = name;
    if (!version.isEmpty()) {
      pubYamlObject.version = version;
    }
    if (!description.isEmpty()) {
      pubYamlObject.description = description;
    }
    if (!author.isEmpty()) {
      if (author.indexOf(',') != -1) { // comma separated list
        pubYamlObject.authors = Arrays.asList(author.split(","));
      } else {
        pubYamlObject.author = author;
      }
    }
    if (!homepage.isEmpty()) {
      pubYamlObject.homepage = homepage;
    }
    Map<String, Object> dependenciesMap = new HashMap<String, Object>();
    for (DependencyObject dep : dependencies) {
      if (!dep.isGitDependency()) {
        dependenciesMap.put(dep.getName(), dep.getVersion());
      } else {
        Map<String, Object> gitMap = new HashMap<String, Object>();
        if (dep.getGitRef() != null && !dep.getGitRef().isEmpty()) {
          Map<String, String> map = new HashMap<String, String>();
          map.put("ref", dep.getGitRef());
          map.put("url", dep.getPath());
          gitMap.put("git", map);
        } else {
          gitMap.put("git", dep.getPath());
        }
        if (!dep.getVersion().equals("any")) {
          gitMap.put("version", dep.getVersion());
        }
        dependenciesMap.put(dep.getName(), gitMap);
      }
    }
    pubYamlObject.dependencies = dependenciesMap;
    return pubYamlObject;
  }

  // Support for dependencies hosted on pub.dartlang.org and git. 
  // TODO(keertip): Add support for hosted on other than pub.dartlang.org
  @SuppressWarnings("unchecked")
  private DependencyObject[] processDependencies(Map<String, Object> yamlDep) {
    List<DependencyObject> deps = new ArrayList<DependencyObject>();

    for (String name : yamlDep.keySet()) {
      DependencyObject d = new DependencyObject(name);
      Object value = yamlDep.get(name);
      if (value instanceof String) {
        d.setVersion((String) value);
      } else if (value instanceof Map) {
        Map<String, Object> values = (Map<String, Object>) value;
        for (String key : values.keySet()) {
          if (key.equals("version")) {
            d.setVersion((String) values.get(key));
          }
          if (key.equals("git")) {
            d.setGitDependency(true);
            Object fields = values.get(key);
            if (fields instanceof String) {
              d.setPath((String) fields);
            }
            if (fields instanceof Map) {
              Map<String, Object> map = (Map<String, Object>) fields;
              for (String mapKey : map.keySet()) {
                if (mapKey.equals("url")) {
                  d.setPath((String) map.get(mapKey));
                }
                if (mapKey.equals("ref")) {
                  d.setGitRef((String) map.get(mapKey));
                }
              }
            }
          }
        }
      }
      deps.add(d);
    }
    return deps.toArray(new DependencyObject[deps.size()]);
  }

  private void setValuesFromObject(PubYamlObject object) {
    if (object != null) {
      name = object.name;
      version = (object.version != null) ? object.version : EMPTY_STRING;
      author = (object.author != null) ? object.author : EMPTY_STRING;
      if (object.authors != null) {
        author = object.authors.get(0);
        for (int i = 1; i < object.authors.size(); i++) {
          author += "," + object.authors.get(i);
        }
      }
      description = (object.description != null) ? object.description : EMPTY_STRING;
      homepage = (object.homepage != null) ? object.homepage : EMPTY_STRING;
      add(processDependencies(object.dependencies), IModelListener.REFRESH);
    }
  }

}
