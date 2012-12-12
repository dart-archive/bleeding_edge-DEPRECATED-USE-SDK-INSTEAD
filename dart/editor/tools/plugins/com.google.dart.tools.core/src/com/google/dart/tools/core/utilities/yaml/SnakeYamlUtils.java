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

import java.beans.IntrospectionException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for using Snake YAML parser
 */
public class SnakeYamlUtils {

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
   * Parse the pubspec.yaml string contents to an Map
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> parsePubspecYamlToMap(String contents) {
    Yaml yaml = new Yaml();
    try {
      Map<String, Object> map = (Map<String, Object>) yaml.load(contents);
      return map;
    } catch (Exception e) {
      DartCore.logError(e);
      return null;
    }

  }

  /**
   * Parse the pubspec.yaml string contents to an {@link PubYamlObject}
   */
  public static PubYamlObject parsePubspecYamlToObject(String contents) {
    PubYamlObject pubYamlObject = null;
    Constructor constructor = new Constructor(PubYamlObject.class);
    Yaml yaml = new Yaml(constructor);
    try {
      pubYamlObject = (PubYamlObject) yaml.load(contents);
      return pubYamlObject;
    } catch (Exception e) {
      DartCore.logError(e);
      return null;
    }

  }

}
