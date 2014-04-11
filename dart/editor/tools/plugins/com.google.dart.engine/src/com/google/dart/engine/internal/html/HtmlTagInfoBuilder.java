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

import com.google.dart.engine.html.ast.HtmlScriptTagNode;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.XmlAttributeNode;
import com.google.dart.engine.html.ast.XmlTagNode;
import com.google.dart.engine.html.ast.visitor.XmlVisitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Instances of the class {@code HtmlTagInfoBuilder} gather information about the tags used in one
 * or more HTML structures.
 */
public class HtmlTagInfoBuilder implements XmlVisitor<Void> {
  /**
   * The name of the 'id' attribute.
   */
  private static final String ID_ATTRIBUTE = "id";

  /**
   * The name of the 'class' attribute.
   */
  private static final String ID_CLASS = "class";

  /**
   * A set containing all of the tag names used in the HTML.
   */
  private HashSet<String> tagSet = new HashSet<String>();

  /**
   * A table mapping the id's that are defined to the tag name with that id.
   */
  private HashMap<String, String> idMap = new HashMap<String, String>();

  /**
   * A table mapping the classes that are defined to a set of the tag names with that class.
   */
  private HashMap<String, HashSet<String>> classMap = new HashMap<String, HashSet<String>>();

  /**
   * Initialize a newly created HTML tag info builder.
   */
  public HtmlTagInfoBuilder() {
    super();
  }

  /**
   * Create a tag information holder holding all of the information gathered about the tags in the
   * HTML structures that were visited.
   * 
   * @return the information gathered about the tags in the visited HTML structures
   */
  public HtmlTagInfo getTagInfo() {
    String[] allTags = tagSet.toArray(new String[tagSet.size()]);
    HashMap<String, String[]> classToTagsMap = new HashMap<String, String[]>(idMap.size());
    for (Map.Entry<String, HashSet<String>> entry : classMap.entrySet()) {
      HashSet<String> tags = entry.getValue();
      classToTagsMap.put(entry.getKey(), tags.toArray(new String[tags.size()]));
    }
    return new HtmlTagInfo(allTags, idMap, classToTagsMap);
  }

  @Override
  public Void visitHtmlScriptTagNode(HtmlScriptTagNode node) {
    return visitXmlTagNode(node);
  }

  @Override
  public Void visitHtmlUnit(HtmlUnit node) {
    node.visitChildren(this);
    return null;
  }

  @Override
  public Void visitXmlAttributeNode(XmlAttributeNode node) {
    return null;
  }

  @Override
  public Void visitXmlTagNode(XmlTagNode node) {
    node.visitChildren(this);
    String tagName = node.getTag();
    tagSet.add(tagName);
    for (XmlAttributeNode attribute : node.getAttributes()) {
      String attributeName = attribute.getName();
      if (attributeName.equals(ID_ATTRIBUTE)) {
        String attributeValue = attribute.getText();
        if (attributeValue != null) {
          String tag = idMap.get(attributeValue);
          if (tag == null) {
            idMap.put(attributeValue, tagName);
          } else {
//            reportError(HtmlWarningCode.MULTIPLY_DEFINED_ID, valueToken);
          }
        }
      } else if (attributeName.equals(ID_CLASS)) {
        String attributeValue = attribute.getText();
        if (attributeValue != null) {
          HashSet<String> tagList = classMap.get(attributeValue);
          if (tagList == null) {
            tagList = new HashSet<String>();
            classMap.put(attributeValue, tagList);
          } else {
//            reportError(HtmlWarningCode.MULTIPLY_DEFINED_ID, valueToken);
          }
          tagList.add(tagName);
        }
      }
    }
    return null;
  }

//  /**
//   * Report an error with the given error code at the given location. Use the given arguments to
//   * compose the error message.
//   * 
//   * @param errorCode the error code of the error to be reported
//   * @param offset the offset of the first character to be highlighted
//   * @param length the number of characters to be highlighted
//   * @param arguments the arguments used to compose the error message
//   */
//  private void reportError(ErrorCode errorCode, Token token, Object... arguments) {
//    errorListener.onError(new AnalysisError(
//        htmlElement.getSource(),
//        token.getOffset(),
//        token.getLength(),
//        errorCode,
//        arguments));
//  }
//
//  /**
//   * Report an error with the given error code at the given location. Use the given arguments to
//   * compose the error message.
//   * 
//   * @param errorCode the error code of the error to be reported
//   * @param offset the offset of the first character to be highlighted
//   * @param length the number of characters to be highlighted
//   * @param arguments the arguments used to compose the error message
//   */
//  private void reportError(ErrorCode errorCode, int offset, int length, Object... arguments) {
//    errorListener.onError(new AnalysisError(
//        htmlElement.getSource(),
//        offset,
//        length,
//        errorCode,
//        arguments));
//  }
}
