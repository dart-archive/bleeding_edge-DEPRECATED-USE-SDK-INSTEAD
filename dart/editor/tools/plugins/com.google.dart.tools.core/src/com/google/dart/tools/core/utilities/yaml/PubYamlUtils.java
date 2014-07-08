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
package com.google.dart.tools.core.utilities.yaml;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.pub.PubspecConstants;
import com.google.dart.tools.core.utilities.resource.IFileUtilities;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for using Snake YAML parser and other yaml utility methods.
 * 
 * @coverage dart.tools.core.utilities
 */
public class PubYamlUtils {

  /**
   * Resolver to avoid parsing to implicit types, instead parse as string
   */
  private static class CustomResolver extends Resolver {

    /*
     * do not resolve float and timestamp, boolean and int
     */
    @Override
    protected void addImplicitResolvers() {
      addImplicitResolver(Tag.BOOL, BOOL, "yYnNtTfFoO");
      addImplicitResolver(Tag.NULL, NULL, "~nN\0");
      addImplicitResolver(Tag.NULL, EMPTY, null);
      addImplicitResolver(Tag.VALUE, VALUE, "=");
      addImplicitResolver(Tag.MERGE, MERGE, "<");
    }
  }

  /**
   * To skip empty and null values in the {@link PubYamlObject} while writing out the yaml string
   */
  private static class SkipEmptyRepresenter extends Representer {
    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property,
        Object propertyValue, Tag customTag) {
      NodeTuple tuple = super.representJavaBeanProperty(
          javaBean,
          property,
          propertyValue,
          customTag);
      Node valueNode = tuple.getValueNode();
      if (Tag.NULL.equals(valueNode.getTag())) {
        return null;// skip 'null' values
      }
      if (valueNode instanceof CollectionNode) {
        if (Tag.SEQ.equals(valueNode.getTag())) {
          SequenceNode seq = (SequenceNode) valueNode;
          if (seq.getValue().isEmpty()) {
            return null;// skip empty lists
          }
        }
        if (Tag.MAP.equals(valueNode.getTag())) {
          MappingNode seq = (MappingNode) valueNode;
          if (seq.getValue().isEmpty()) {
            return null;// skip empty maps
          }
        }
      }
      return tuple;
    }
  }

  /**
   * Class to preserve the order of fields as declared in {@link PubYamlObject} while writing out
   * the yaml string
   */
  private static class UnsortedPropertyUtils extends PropertyUtils {
    @Override
    protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess)
        throws IntrospectionException {
      Set<Property> result = new LinkedHashSet<Property>(
          getPropertiesMap(type, BeanAccess.FIELD).values());
      return result;
    }
  }

  /**
   * Represents a pub package semantic version. A version is of the form major.minor.patch and can
   * be followed by a prerelease or build string eg. 0.4.8+2, 2.34.56-hotfix.issue, 0.54.9+build.2,
   * 1.23.5
   */
  private static class Version implements Comparable<Version> {

    int major;
    int minor;
    int patch;
    String preRelease;
    String build;

    public Version(String string) {

      String[] strings = string.split("\\.");
      major = Integer.parseInt(strings[0]);
      minor = Integer.parseInt(strings[1]);
      if (strings[2].contains("-")) {
        String[] s = strings[2].split("-");
        patch = Integer.parseInt(s[0]);
        preRelease = s[1];
        if (strings.length > 3) {
          for (int i = 3; i < strings.length; i++) {
            preRelease += "." + strings[i];
          }
        }
      } else if (strings[2].contains("+")) {
        String[] s = strings[2].split("\\+");
        patch = Integer.parseInt(s[0]);
        build = s[1];
        if (strings.length > 3) {
          for (int i = 3; i <= strings.length; i++) {
            build += "." + strings[i];
          }
        }
      } else {
        patch = Integer.parseInt(strings[2]);
      }
    }

    @Override
    public int compareTo(Version other) {
      if (major != other.major) {
        return new Integer(major).compareTo(new Integer(other.major));
      }
      if (minor != other.minor) {
        return new Integer(minor).compareTo(new Integer(other.minor));
      }
      if (patch != other.patch) {
        return new Integer(patch).compareTo(new Integer(other.patch));
      }

      if (preRelease != other.preRelease) {
        // Pre-releases always come before no pre-release string.
        if (preRelease == null) {
          return 1;
        }
        if (other.preRelease == null) {
          return -1;
        }

        return compareStrings(preRelease, other.preRelease);
      }

      if (build != other.build) {
        // Builds always come after no build string.
        if (build == null) {
          return -1;
        }
        if (other.build == null) {
          return 1;
        }

        return compareStrings(build, other.build);

      }
      return 0;

    }

    @Override
    public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append(major).append(".");
      buffer.append(minor).append(".");
      buffer.append(patch);
      if (preRelease != null) {
        buffer.append("-").append(preRelease);
      }
      if (build != null) {
        buffer.append("+").append(build);
      }
      return buffer.toString();
    }

    /// Compares the string part of two versions. This is used for the pre-release
    /// and build version parts. This follows Rule 12. of the Semantic Versioning
    /// spec.
    int compareStrings(String a, String b) {

      Object[] aParts = splitParts(a);
      Object[] bParts = splitParts(b);

      for (int i = 0; i < Math.max(aParts.length, bParts.length); i++) {
        Object aPart = (i < aParts.length) ? aParts[i] : null;
        Object bPart = (i < bParts.length) ? bParts[i] : null;

        if (aPart != bPart) {
          // Missing parts come before present ones.
          if (aPart == null) {
            return -1;
          }
          if (bPart == null) {
            return 1;
          }

          if (aPart instanceof Integer) {
            if (bPart instanceof Integer) {
              // Compare two numbers.
              return ((Integer) aPart).compareTo((Integer) bPart);
            } else {
              // Numbers come before strings.
              return -1;
            }
          } else {
            if (bPart instanceof Integer) {
              // Strings come after numbers.
              return 1;
            } else {
              // Compare two strings.
              return ((String) aPart).compareTo((String) bPart);
            }
          }
        }
      }
      return 0;
    }

    /// Splits a string of dot-delimited identifiers into their component parts.
    /// Identifiers that are numeric are converted to numbers.
    Object[] splitParts(String text) {
      List<Object> list = new ArrayList<Object>();
      String[] objects = text.split("\\.");
      for (String o : objects) {
        try {
          Integer i = Integer.parseInt(o);
          list.add(i);
        } catch (NumberFormatException e) {
          list.add(o);
        }
      }
      return list.toArray(new Object[list.size()]);
    }

  }

  public static String PACKAGE_VERSION_EXPRESSION = "(\\d+\\.){2}\\d+([\\+-]([\\.a-zA-Z0-9-\\+])*)?";
  public static String PATTERN_PUBSPEC_NAME_LINE = "(?m)^(?:(?!--|').|'(?:''|[^'])*')*(name:.*)$";

  public static String VERSION_CONTSTRAINTS_EXPRESSION = "([=]{0,1}[<>]?)|([<>]?[=]{0,1})(\\d+\\.){2}\\d+([\\+-]([\\.a-zA-Z0-9-])*)?";

  /**
   * Return a yaml string for the given Map
   * 
   * @param pubYamlObject bean for pubspec
   * @return String
   */
  public static String buildYamlString(Map<String, Object> yamlMap) {

    try {
      SkipEmptyRepresenter repr = new SkipEmptyRepresenter();
      repr.setPropertyUtils(new UnsortedPropertyUtils());
      Yaml yaml = new Yaml(repr);
      String yamlString = yaml.dumpAsMap(yamlMap);
      return yamlString;
    } catch (Exception e) {
      DartCore.logError(e);
      return null;
    }

  }

  /**
   * Return a yaml string for the given {@link PubYamlObject}
   * 
   * @param pubYamlObject bean for pubspec
   * @return String
   */
  public static String buildYamlString(PubYamlObject pubYamlObject) {

    try {
      SkipEmptyRepresenter repr = new SkipEmptyRepresenter();
      repr.setPropertyUtils(new UnsortedPropertyUtils());
      Yaml yaml = new Yaml(repr);
      String yamlString = yaml.dumpAsMap(pubYamlObject);
      return yamlString;
    } catch (Exception e) {
      DartCore.logError(e);
      return null;
    }

  }

  /**
   * Return a list of names of the dependencies specified in the pubspec
   * 
   * @param contents String contents of pubspec.yaml
   * @return List<String> names of the packages specified as dependencies
   */
  @SuppressWarnings("unchecked")
  public static List<String> getNamesOfDependencies(String contents) {
    Map<String, Object> map = null;
    try {
      map = parsePubspecYamlToMap(contents);
    } catch (ScannerException e) {
      DartCore.logError(e);
    }
    if (map != null) {
      Map<String, Object> dependecies = (Map<String, Object>) map.get(PubspecConstants.DEPENDENCIES);
      if (dependecies != null && !dependecies.isEmpty()) {
        return new ArrayList<String>(dependecies.keySet());
      }
    }
    return null;
  }

  /**
   * Returns a map of installed packages to the respective version number.
   * 
   * @param lockFile the pubspec.lock file
   * @return Map<String,String> Map<packageName,versionNumber>
   */
  public static Map<String, String> getPackageVersionMap(IResource lockFile) {
    try {
      return getPackageVersionMap(IFileUtilities.getContents((IFile) lockFile));
    } catch (CoreException exception) {
      DartCore.logError(exception);
    } catch (IOException exception) {
      DartCore.logError(exception);
    }
    return null;
  }

  /**
   * Returns a map of installed packages to the respective version number.
   * 
   * @param lockFileContents string contents of pubspec.lock file
   * @return Map<String,String> Map<packageName,versionNumber>
   */
  @SuppressWarnings("unchecked")
  public static Map<String, String> getPackageVersionMap(String lockFileContents) {

    Map<String, String> versionMap = new HashMap<String, String>();
    Map<String, Object> map = null;
    try {
      map = PubYamlUtils.parsePubspecYamlToMap(lockFileContents);
    } catch (ScannerException e) {
      DartCore.logError(e);
    }
    if (map != null) {
      Map<String, Object> packagesMap = (Map<String, Object>) map.get("packages");
      if (packagesMap != null) {
        for (String key : packagesMap.keySet()) {
          Map<String, Object> attrMap = (Map<String, Object>) packagesMap.get(key);
          String version = (String) attrMap.get(PubspecConstants.VERSION);
          if (version != null) {
            versionMap.put(key, version);
          }
        }
      }
    }
    return versionMap;
  }

  /**
   * Return the name of the package as specified in pubspec.yaml (name: sample)
   * 
   * @param contents string contents of the pubspec.yaml file
   * @return String package name
   */
  public static String getPubspecName(String contents) {
    Matcher m = Pattern.compile(PubYamlUtils.PATTERN_PUBSPEC_NAME_LINE).matcher(contents);
    if (m.find()) {
      String[] strings = m.group(1).split(":");
      if (strings.length == 2) {
        return strings[1].replaceAll(" ", "");
      }
    }
    return null;
  }

  /**
   * Checks if the string has a valid version constraint format ">=1.2.3 <2.0.0", "1.0.0", "<1.5.0"
   */
  public static boolean isValidVersionConstraintString(String version) {
    if (!version.equals(PubspecConstants.ANY) && !version.isEmpty()) {
      String[] versions = version.split(" ");
      if (versions.length > 2) {
        return false;
      } else {
        for (String ver : versions) {
          if (!isValidVersionConstraint(ver)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Parse the pubspec.yaml string contents to an Map
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parsePubspecYamlToMap(String contents) throws ScannerException {
    Yaml yaml = new Yaml(
        new Constructor(),
        new Representer(),
        new DumperOptions(),
        new CustomResolver());
    Object o = yaml.load(contents);
    Map<String, Object> map = new HashMap<String, Object>();
    if (o instanceof Map) {
      map.putAll((Map<String, Object>) o);
    }
    return map;
  }

  public static String[] sortVersionArray(String[] versionList) {

    List<Version> versions = new ArrayList<PubYamlUtils.Version>();
    for (Object o : versionList) {
      versions.add(new Version(o.toString()));
    }
    Collections.sort(versions);
    List<String> strings = new ArrayList<String>();
    for (Version version : versions) {
      strings.add(version.toString());
    }

    return strings.toArray(new String[strings.size()]);
  }

  private static boolean isValidVersionConstraint(String string) {

    int index = 0;
    while (index + 1 <= string.length() && !string.substring(index, index + 1).matches("[0-9]")) {
      index++;
    }
    if (index == string.length()) {
      return false;
    }
    if (index > 2) { // can be single [<>] or two char length [=][<>] 
      return false;
    }
    if (index == 1) {
      String substring = string.substring(0, 1);
      if (!substring.equals("<") && !substring.equals(">")) {
        return false;
      }
    }
    if (index == 2) {
      String substring = string.substring(0, 2);
      if (!substring.equals(">=") && !substring.equals("<=") && !substring.equals("=>")
          && !substring.equals("=<")) {
        return false;
      }
    }
    if (string.substring(index).matches(PACKAGE_VERSION_EXPRESSION)) {
      return true;
    }
    return false;
  }
}
