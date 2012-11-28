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
package com.google.dart.tools.core.internal.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Instances of the class <code>LibraryReferenceFinder</code> scan a file for potential references
 * to one or more Dart libraries.
 */
public class LibraryReferenceFinder {
  private static final String COMMENT_START = "<!--";
  private static final int COMMENT_START_LENGTH = COMMENT_START.length();

  private static final String COMMENT_END = "-->";
  private static final int COMMENT_END_LENGTH = COMMENT_END.length();

  private static final String TAG_NAME_SCRIPT = "script";
  private static final int TAG_NAME_SCRIPT_LENGTH = TAG_NAME_SCRIPT.length();

  private static final String TAG_END_SCRIPT = "</script>";
  private static final int TAG_END_SCRIPT_LENGTH = TAG_END_SCRIPT.length();

  /**
   * Find all of the potential library references that are contained in the given HTML source. Note
   * that there is no validation done to ensure that the name of a library actually references a
   * library within the workspace.
   * 
   * @param htmlSource the HTML source to be searched
   * @return the potential library references that were found
   */
  public static List<String> findInHTML(String htmlSource) {
    LibraryReferenceFinder finder = new LibraryReferenceFinder();
    finder.processHTML(htmlSource);
    return finder.getLibraryList();
  }

  /**
   * A list containing the paths to the potential libraries that were found.
   */
  public List<String> libraryList = new ArrayList<String>();

  /**
   * Initialize a newly created library reference finder.
   */
  public LibraryReferenceFinder() {
    super();
  }

  /**
   * Return a list containing the paths to the potential libraries that were found.
   * 
   * @return a list containing the paths to the library names that were found
   */
  public List<String> getLibraryList() {
    return libraryList;
  }

  /**
   * Process the given HTML source, adding any potential library references that are found to the
   * list returned by {@link #getLibraryList()}.
   * 
   * @param htmlSource the HTML source to be searched
   */
  public void processHTML(String htmlSource) {
    int index = htmlSource.indexOf('<');
    while (index >= 0) {
      if (htmlSource.startsWith(COMMENT_START, index)) {
        index = htmlSource.indexOf(COMMENT_END, index + COMMENT_START_LENGTH);
        if (index < 0) {
          return;
        }
        index += COMMENT_END_LENGTH;
      } else {
        index = skipWhitespace(htmlSource, index + 1);
        if (htmlSource.startsWith(TAG_NAME_SCRIPT, index)) {
          index += TAG_NAME_SCRIPT_LENGTH;
          int endIndex = htmlSource.indexOf('>', index);
          if (endIndex < 0) {
            return;
          }
          boolean isDartScript = processScriptTagAttributes(htmlSource.substring(index, endIndex));
          if (isDartScript && htmlSource.charAt(endIndex - 1) != '/') {
            // If the script tag is well-formed, then it has a body that we also need to process.
            endIndex++;
            index = htmlSource.indexOf(TAG_END_SCRIPT, endIndex);
            if (index < 0) {
              return;
            }
            processScriptBody(htmlSource.substring(endIndex, index));
            index += TAG_END_SCRIPT_LENGTH;
          } else {
            index = endIndex + 1;
          }
        }
      }
      index = htmlSource.indexOf('<', index);
    }
  }

  /**
   * Extract the name of the file being referenced from the given src location string.
   * 
   * @param srcLocation the value of the src attribute in a script tag
   * @return the name of the file being referenced
   */
  private String extractFileNameFromSrc(String srcLocation) {
    int index = srcLocation.indexOf('/');
    if (index >= 0) {
      return srcLocation.substring(index + 1);
    }
    index = srcLocation.indexOf(':');
    if (index >= 0) {
      return srcLocation.substring(index + 1);
    }
    return srcLocation;
  }

  /**
   * Return the value of the attribute in the given tag body that starts at the given index and is
   * terminated by the given character.
   * 
   * @param tagBody the body of the tag being scanned
   * @param startIndex the index of the first character of the attribute value
   * @param closingQuote the quote character used to terminate the attribute value
   * @return the value of the given attribute
   */
  private String getAttributeValue(String tagBody, int startIndex, char closingQuote) {
    int endIndex = tagBody.indexOf(closingQuote, startIndex);
    if (endIndex < 0) {
      return null;
    }
    return tagBody.substring(startIndex, endIndex);
  }

  /**
   * Return the value of the attribute in the given tag body with the given name, or
   * <code>null</code> if there is no attribute with the given name.
   * 
   * @param tagBody the body of the tag being scanned
   * @param attributeName the name of the attribute whose value is to be returned
   * @return the value of the given attribute
   */
  private String getAttributeValue(String tagBody, String attributeName) {
    int index = tagBody.indexOf(attributeName);
    if (index < 0) {
      return null;
    }
    int length = tagBody.length();
    index = skipWhitespace(tagBody, index + attributeName.length());
    if (index >= length || tagBody.charAt(index) != '=') {
      return null;
    }
    index = skipWhitespace(tagBody, index + 1);
    if (index >= length) {
      return null;
    }
    if (tagBody.charAt(index) == '"') {
      return getAttributeValue(tagBody, index + 1, '"');
    } else if (tagBody.charAt(index) == '\'') {
      return getAttributeValue(tagBody, index + 1, '\'');
    }
    int endIndex = skipToWhitespace(tagBody, index + 1);
    return tagBody.substring(index, endIndex);
  }

  /**
   * Examine the content of a script tag (the portion between the start tag and the end tag) to
   * locate any libraries that are being imported, and add them to the list of libraries.
   * 
   * @param scriptBody the content of the script tag to be processed
   */
  private void processScriptBody(String scriptBody) {
    // TODO(brianwilkerson) Implement this to look for #import directives (once the format is more
    // settled).
  }

  /**
   * Examine the attributes from a script tag (the portion between the "<code>&lt;script</code>" and
   * "<code>&gt;</code>") to locate the library being referenced, and add it to the list of
   * libraries.
   * 
   * @param scriptTag the script tag to be processed
   * @return <code>true</code> if the script is a dart script and the body needs to be examined
   */
  private boolean processScriptTagAttributes(String scriptTag) {
    String srcLocation = getAttributeValue(scriptTag, "src");
    if (srcLocation != null) {
      String scriptType = getAttributeValue(scriptTag, "type");
      if (scriptType == null || scriptType.equals("text/javascript")
          || scriptType.equals("application/javascript")) {
        // Match the "Generate Optimized Javascript" default extension
        if (srcLocation.endsWith(".dart.js")) {
          String fileName = extractFileNameFromSrc(srcLocation);
          fileName = fileName.substring(0, fileName.length() - 3);
          libraryList.add(fileName);
        }
      } else if (scriptType.equals("application/dart")) {
        libraryList.add(srcLocation);
        return true;
      }
    }
    return false;
  }

  /**
   * Return the index in the given string of the first whitespace character at or after the given
   * start index, or the length of the string if there is no whitespace character.
   * 
   * @param string the string being scanned
   * @param start the first character to be examined
   * @return the index of the first whitespace character
   */
  private int skipToWhitespace(String string, int startIndex) {
    int length = string.length();
    int index = startIndex;
    while (index < length && !Character.isWhitespace(string.charAt(index))) {
      index++;
    }
    return index;
  }

  /**
   * Return the index in the given string of the first non-whitespace character at or after the
   * given start index, or the length of the string if there is no non-whitespace character.
   * 
   * @param string the string being scanned
   * @param startIndex the first character to be examined
   * @return the index of the first non-whitespace character
   */
  private int skipWhitespace(String string, int startIndex) {
    int length = string.length();
    int index = startIndex;
    while (index < length && Character.isWhitespace(string.charAt(index))) {
      index++;
    }
    return index;
  }
}
