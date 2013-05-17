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
package com.google.dart.engine.internal.html;

import java.util.HashMap;

/**
 * Instances of the class {@code HtmlTagInfo} record information about the tags used in an HTML
 * file.
 */
public class HtmlTagInfo {
  /**
   * An array containing all of the tags used in the HTML file.
   */
  private String[] allTags;

  /**
   * A table mapping the id's defined in the HTML file to an array containing the names of tags with
   * that identifier.
   */
  private HashMap<String, String> idToTagMap;

  /**
   * A table mapping the classes defined in the HTML file to an array containing the names of tags
   * with that class.
   */
  private HashMap<String, String[]> classToTagsMap;

  /**
   * Initialize a newly created information holder to hold the given information about the tags in
   * an HTML file.
   * 
   * @param allTags an array containing all of the tags used in the HTML file
   * @param idToTagMap a table mapping the id's defined in the HTML file to an array containing the
   *          names of tags with that identifier
   * @param classToTagsMap a table mapping the classes defined in the HTML file to an array
   *          containing the names of tags with that class
   */
  public HtmlTagInfo(String[] allTags, HashMap<String, String> idToTagMap,
      HashMap<String, String[]> classToTagsMap) {
    this.allTags = allTags;
    this.idToTagMap = idToTagMap;
    this.classToTagsMap = classToTagsMap;
  }

  /**
   * Return an array containing all of the tags used in the HTML file.
   * 
   * @return an array containing all of the tags used in the HTML file
   */
  public String[] getAllTags() {
    return allTags;
  }

  /**
   * Return an array containing the tags that have the given class, or {@code null} if there are no
   * such tags.
   * 
   * @return an array containing the tags that have the given class
   */
  public String[] getTagsWithClass(String identifier) {
    return classToTagsMap.get(identifier);
  }

  /**
   * Return the tag that has the given identifier, or {@code null} if there is no such tag (the
   * identifier is not defined).
   * 
   * @return the tag that has the given identifier
   */
  public String getTagWithId(String identifier) {
    return idToTagMap.get(identifier);
  }
}
