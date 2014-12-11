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
package com.google.dart.tools.core.pub;

import com.google.common.collect.Lists;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.pub.DependencyObject.Type;
import com.google.dart.tools.core.utilities.yaml.PubYamlUtils;

import org.eclipse.core.resources.IFile;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model representing the pubspec.
 * 
 * @coverage dart.tools.core.pub
 */
public class PubspecModel {

  public static final String PUBSPEC_MARKER = DartCore.PLUGIN_ID + ".pubspecIssue";

  private static String EMPTY_STRING = "";

  private static final Comparator<String> fieldComparator = new Comparator<String>() {

    Random random = new Random();

    List<String> fieldNames = Arrays.asList(
        "name",
        "version",
        "author",
        "authors",
        "description",
        "homepage",
        "environment",
        "sdk",
        "documentation",
        "dependencies",
        "dev_dependencies",
        "dependency_overrides",
        "transformers");

    @Override
    public int compare(String o1, String o2) {
      // make sure unknown fields are written at the end
      // TODO(keertip): sort unknowns
      int index1 = fieldNames.indexOf(o1) != -1 ? fieldNames.indexOf(o1) : random.nextInt(50) + 11;
      int index2 = fieldNames.indexOf(o2) != -1 ? fieldNames.indexOf(o2) : random.nextInt(50) + 11;
      return index1 - index2;
    }

  };

  private ArrayList<IModelListener> modelListeners;

  private String name = EMPTY_STRING;
  private String version = EMPTY_STRING;
  private String description = EMPTY_STRING;
  private String author = EMPTY_STRING;
  private String homepage = EMPTY_STRING;
  private String sdkVersion = EMPTY_STRING;

  private String documentation = EMPTY_STRING;

  private boolean errorOnParse = false;

  private ArrayList<DependencyObject> dependencies;

  private ArrayList<Object> transformers;

  private String comments;

  private boolean isDirty = false;

  private IFile file;

  private Map<String, Object> yamlMap = new HashMap<String, Object>();

  private final List<PubspecException> exceptions = Lists.newArrayList();

  public PubspecModel(IFile file, String contents) {
    this.file = file;
    initModel(contents);
  }

  public PubspecModel(String contents) {
    initModel(contents);
  }

  public void add(DependencyObject[] objs, String eventType) {
    if (objs.length > 0) {
      for (int i = 0; i < objs.length; i++) {
        if (objs[i] != null) {
          dependencies.add(objs[i]);
          objs[i].setModel(this);
        }
      }
      fireModelChanged(objs, eventType);
    }
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
    // append comments at end of pubspec
    updateMapValues();
    TreeMap<String, Object> map = new TreeMap<String, Object>(fieldComparator);
    map.putAll(yamlMap);
    return PubYamlUtils.buildYamlString(map) + comments;
  }

  public Object[] getDependecies() {
    return dependencies.toArray();
  }

  public String getDescription() {
    return description;
  }

  public String getDocumentation() {
    return documentation;
  }

  public List<PubspecException> getExceptions() {
    return exceptions;
  }

  public String getHomepage() {
    return homepage;
  }

  public String getName() {
    return name;
  }

  public String getSdkVersion() {
    return sdkVersion;
  }

  public ArrayList<Object> getTransformers() {
    return transformers;
  }

  public String getVersion() {
    return version;
  }

  public boolean isDirty() {
    return isDirty;
  }

  public boolean isErrorOnParse() {
    return errorOnParse;
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

  /**
   * Saves the content of this model to its original {@link IFile}.
   */
  public void save() throws Exception {
    if (file == null) {
      new IllegalStateException("This model doesn't have a file.");
    }
    String contents = getContents();
    byte[] contentBytes = contents.getBytes("UTF-8");
    file.setContents(new ByteArrayInputStream(contentBytes), true, true, null);
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

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  public void setHomepage(String homepage) {
    this.homepage = homepage;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSdkVersion(String sdkVersion) {
    this.sdkVersion = sdkVersion;
  }

  /**
   * Initialize the model with the values in the yaml string
   */
  public void setValuesFromString(String yamlString) {
    if (yamlString != null) {
      comments = getComments(yamlString);
      if (!yamlString.isEmpty()) {
        try {
          exceptions.clear();
          errorOnParse = false;
          yamlMap = PubYamlUtils.parsePubspecYamlToMap(yamlString);
          setValuesFromMap(yamlMap);
        } catch (MarkedYAMLException exception) {
          errorOnParse = true;
          Mark mark = exception.getProblemMark();
          exceptions.add(new PubspecException(
              "Invalid syntax: " + exception.getProblem(),
              mark.getLine(),
              mark.getIndex(),
              mark.getIndex() + 2));
        }
      }
    }
  }

  public void setVersion(String version) {
    this.version = version;
  }

  private void clearModelFields() {
    isDirty = false;
    name = version = description = homepage = author = sdkVersion = comments = documentation = EMPTY_STRING;
    dependencies.clear();
  }

  // search for comments and store them so that we don't lose it altogether
  // TODO(keertip): remove when we can do micro edits
  private String getComments(String yamlString) {
    Matcher m = Pattern.compile("(?m)^(?:(?!--|').|'(?:''|[^'])*')*(#.*)$").matcher(yamlString);
    StringBuilder builder = new StringBuilder();
    while (m.find()) {
      builder.append("\n");
      builder.append(m.group(1));
    }
    return builder.toString();
  }

  private void initModel(String contents) {
    modelListeners = new ArrayList<IModelListener>();
    dependencies = new ArrayList<DependencyObject>();
    setValuesFromString(contents);
  }

  // Support for dependencies hosted on pub.dartlang.org and git. 
  // TODO(keertip): Add support for hosted on other than pub.dartlang.org
  @SuppressWarnings("unchecked")
  private DependencyObject[] processDependencies(Map<String, Object> yamlDep, boolean isDev) {
    List<DependencyObject> deps = new ArrayList<DependencyObject>();
    if (yamlDep != null) {
      for (String name : yamlDep.keySet()) {
        DependencyObject d = new DependencyObject(name);
        d.setForDevelopment(isDev);
        Object value = yamlDep.get(name);
        if (value instanceof String) {
          d.setVersion((String) value);
        } else if (value instanceof Map) {
          Map<String, Object> values = (Map<String, Object>) value;
          for (String key : values.keySet()) {
            if (key.equals(PubspecConstants.VERSION)) {
              d.setVersion((String) values.get(key));
            }
            if (key.equals(PubspecConstants.PATH)) {
              d.setPath((String) values.get(key));
              d.setType(Type.PATH);
            }
            if (key.equals(PubspecConstants.GIT)) {
              d.setType(Type.GIT);
              Object fields = values.get(key);
              if (fields instanceof String) {
                d.setPath((String) fields);
              }
              if (fields instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) fields;
                for (String mapKey : map.keySet()) {
                  if (mapKey.equals(PubspecConstants.URL)) {
                    d.setPath((String) map.get(mapKey));
                  }
                  if (mapKey.equals(PubspecConstants.REF)) {
                    d.setGitRef((String) map.get(mapKey));
                  }
                }
              }
            }
            if (key.endsWith(PubspecConstants.HOSTED)) {
              Object fields = values.get(key);
              if (fields instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) fields;
                for (String mapKey : map.keySet()) {
                  if (mapKey.equals(PubspecConstants.URL)) {
                    d.setPath((String) map.get(mapKey));
                  }
                }
              }
            }
          }
        }
        deps.add(d);
      }
    }
    return deps.toArray(new DependencyObject[deps.size()]);
  }

  @SuppressWarnings("unchecked")
  private ArrayList<Object> safelyAsArray(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value == null) {
      return new ArrayList<Object>();
    }
    if (value instanceof ArrayList) {
      return (ArrayList<Object>) value;
    }
    return new ArrayList<Object>();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> safelyAsMap(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value == null) {
      return new HashMap<String, Object>();
    }
    if (value instanceof Map) {
      return (Map<String, Object>) value;
    }
    return new HashMap<String, Object>();
  }

  private String safelyAsString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    if (value == null) {
      return EMPTY_STRING;
    }
    if (value instanceof String) {
      return (String) value;
    }

    return EMPTY_STRING;
  }

  @SuppressWarnings("unchecked")
  private void setValuesFromMap(Map<String, Object> pubspecMap) {
    if (pubspecMap != null) {
      clearModelFields();
      name = safelyAsString(pubspecMap, PubspecConstants.NAME);
      version = safelyAsString(pubspecMap, PubspecConstants.VERSION);
      author = safelyAsString(pubspecMap, PubspecConstants.AUTHOR);
      if (pubspecMap.get(PubspecConstants.AUTHORS) != null) {
        Object o = pubspecMap.get(PubspecConstants.AUTHORS);
        if (o instanceof List) {
          List<String> authors = (List<String>) pubspecMap.get(PubspecConstants.AUTHORS);
          author = authors.get(0);
          for (int i = 1; i < authors.size(); i++) {
            author += ", " + authors.get(i);
          }
        } else {
          author = (String) o;
        }
      }
      if (pubspecMap.get(PubspecConstants.ENVIRONMENT) != null) {
        Map<String, Object> env = safelyAsMap(pubspecMap, PubspecConstants.ENVIRONMENT);
        sdkVersion = safelyAsString(env, PubspecConstants.SDK_VERSION);
      } else {
        sdkVersion = EMPTY_STRING;
      }

      description = safelyAsString(pubspecMap, PubspecConstants.DESCRIPTION);
      homepage = safelyAsString(pubspecMap, PubspecConstants.HOMEPAGE);
      documentation = safelyAsString(pubspecMap, PubspecConstants.DOCUMENTATION);
      transformers = safelyAsArray(pubspecMap, PubspecConstants.TRANSFORMERS);

      add(
          processDependencies(safelyAsMap(pubspecMap, PubspecConstants.DEPENDENCIES), false),
          IModelListener.REFRESH);
      add(
          processDependencies(safelyAsMap(pubspecMap, PubspecConstants.DEV_DEPENDENCIES), true),
          IModelListener.REFRESH);
      errorOnParse = false;
    } else {
      errorOnParse = true;
    }
  }

  // update the map of the original contents with the values from the model
  private void updateMapValues() {
    if (yamlMap != null) {
      yamlMap.put(PubspecConstants.NAME, name);
      yamlMap.remove(PubspecConstants.VERSION);
      if (!version.isEmpty()) {
        yamlMap.put(PubspecConstants.VERSION, version);
      }
      yamlMap.remove(PubspecConstants.DESCRIPTION);
      if (!description.isEmpty()) {
        yamlMap.put(PubspecConstants.DESCRIPTION, description);
      }
      yamlMap.remove(PubspecConstants.AUTHOR);
      yamlMap.remove(PubspecConstants.AUTHORS);
      if (!author.isEmpty()) {
        if (author.indexOf(',') != -1) { // comma separated list
          String[] strings = author.split(",");
          for (int i = 0; i < strings.length; i++) {
            strings[i] = strings[i].trim();
          }
          yamlMap.put(PubspecConstants.AUTHORS, Arrays.asList(strings));
        } else {
          yamlMap.put(PubspecConstants.AUTHOR, author);
        }
      }
      yamlMap.remove(PubspecConstants.HOMEPAGE);
      if (!homepage.isEmpty()) {
        yamlMap.put(PubspecConstants.HOMEPAGE, homepage);
      }
      yamlMap.remove(PubspecConstants.DOCUMENTATION);
      if (!documentation.isEmpty()) {
        yamlMap.put(PubspecConstants.DOCUMENTATION, documentation);
      }
      yamlMap.remove(PubspecConstants.ENVIRONMENT);
      if (!sdkVersion.isEmpty()) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(PubspecConstants.SDK_VERSION, sdkVersion);
        yamlMap.put(PubspecConstants.ENVIRONMENT, map);
      }
      Map<String, Object> dependenciesMap = new HashMap<String, Object>();
      Map<String, Object> devDependenciesMap = new HashMap<String, Object>();
      for (DependencyObject dep : dependencies) {
        if (dep.getType().equals(Type.HOSTED)) {
          if (dep.getPath() != null && !dep.getPath().isEmpty()) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(PubspecConstants.NAME, dep.getName());
            map.put(PubspecConstants.URL, dep.getPath());
            Map<String, Object> dMap = new HashMap<String, Object>();
            dMap.put(PubspecConstants.HOSTED, map);
            if (!dep.getVersion().isEmpty()) {
              dMap.put(PubspecConstants.VERSION, dep.getVersion());
            }
            if (dep.isForDevelopment()) {
              devDependenciesMap.put(dep.getName(), dMap);
            } else {
              dependenciesMap.put(dep.getName(), dMap);
            }
          } else {
            if (dep.getVersion().isEmpty()) {
              if (dep.isForDevelopment()) {
                devDependenciesMap.put(dep.getName(), PubspecConstants.ANY);
              } else {
                dependenciesMap.put(dep.getName(), PubspecConstants.ANY);
              }
            } else {
              if (dep.isForDevelopment()) {
                devDependenciesMap.put(dep.getName(), dep.getVersion());
              } else {
                dependenciesMap.put(dep.getName(), dep.getVersion());
              }
            }
          }
        } else if (dep.getType().equals(Type.GIT)) {
          Map<String, Object> gitMap = new HashMap<String, Object>();
          if (dep.getGitRef() != null && !dep.getGitRef().isEmpty()) {
            Map<String, String> map = new HashMap<String, String>();
            map.put(PubspecConstants.REF, dep.getGitRef());
            map.put(PubspecConstants.URL, dep.getPath());
            gitMap.put(PubspecConstants.GIT, map);
          } else {
            gitMap.put(PubspecConstants.GIT, dep.getPath());
          }
          if (!dep.getVersion().equals(PubspecConstants.ANY) && !dep.getVersion().isEmpty()) {
            gitMap.put(PubspecConstants.VERSION, dep.getVersion());
          }
          if (dep.isForDevelopment()) {
            devDependenciesMap.put(dep.getName(), gitMap);
          } else {
            dependenciesMap.put(dep.getName(), gitMap);
          }
        } else {
          Map<String, Object> pathMap = new HashMap<String, Object>();
          pathMap.put(PubspecConstants.PATH, dep.getPath());
          if (dep.isForDevelopment()) {
            devDependenciesMap.put(dep.getName(), pathMap);
          } else {
            dependenciesMap.put(dep.getName(), pathMap);
          }
        }
      }
      yamlMap.remove(PubspecConstants.DEPENDENCIES);
      if (!dependenciesMap.isEmpty()) {
        yamlMap.put(PubspecConstants.DEPENDENCIES, new TreeMap<String, Object>(dependenciesMap));
      }
      yamlMap.remove(PubspecConstants.DEV_DEPENDENCIES);
      if (!devDependenciesMap.isEmpty()) {
        yamlMap.put(PubspecConstants.DEV_DEPENDENCIES, new TreeMap<String, Object>(
            devDependenciesMap));
      }
      yamlMap.remove(PubspecConstants.TRANSFORMERS);
      if (transformers != null && !transformers.isEmpty()) {
        yamlMap.put(PubspecConstants.TRANSFORMERS, transformers);
      }
    }
  }

}
