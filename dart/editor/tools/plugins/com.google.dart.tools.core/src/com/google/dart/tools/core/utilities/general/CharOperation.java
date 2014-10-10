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

package com.google.dart.tools.core.utilities.general;

/**
 * @coverage dart.engine.utilities
 */
public class CharOperation {
  /**
   * Return true if the pattern matches the given name using CamelCase rules, or false otherwise.
   * char[] CamelCase matching does NOT accept explicit wild-cards '*' and '?' and is inherently
   * case sensitive.
   * <p>
   * CamelCase denotes the convention of writing compound names without spaces, and capitalizing
   * every term. This function recognizes both upper and lower CamelCase, depending whether the
   * leading character is capitalized or not. The leading part of an upper CamelCase pattern is
   * assumed to contain a sequence of capitals which are appearing in the matching name; e.g. 'NPE'
   * will match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern uses a
   * lowercase first character. In Java, type names follow the upper CamelCase convention, whereas
   * method or field names follow the lower CamelCase convention.
   * <p>
   * The pattern may contain lowercase characters, which will be matched in a case sensitive way.
   * These characters must appear in sequence in the name. For instance, 'NPExcep' will match
   * 'NullPointerException', but not 'NullPointerExCEPTION' or 'NuPoEx' will match
   * 'NullPointerException', but not 'NoPointerException'.
   * <p>
   * Digit characters are treated in a special way. They can be used in the pattern but are not
   * always considered as leading character. For instance, both 'UTF16DSS' and 'UTFDSS' patterns
   * will match 'UTF16DocumentScannerSupport'.
   * <p>
   * Using this method allows matching names to have more parts than the specified pattern (see
   * {@link #camelCaseMatch(char[], char[], boolean)}).<br>
   * For instance, 'HM' , 'HaMa' and 'HMap' patterns will match 'HashMap', 'HatMapper' <b>and
   * also</b> 'HashMapEntry'.
   * <p>
   * 
   * <pre>
   * Examples:<ol>
   * <li> pattern = "NPE".toCharArray()
   * name = "NullPointerException".toCharArray()
   * result => true</li>
   * <li> pattern = "NPE".toCharArray()
   * name = "NoPermissionException".toCharArray()
   * result => true</li>
   * <li> pattern = "NuPoEx".toCharArray()
   * name = "NullPointerException".toCharArray()
   * result => true</li>
   * <li> pattern = "NuPoEx".toCharArray()
   * name = "NoPermissionException".toCharArray()
   * result => false</li>
   * <li> pattern = "npe".toCharArray()
   * name = "NullPointerException".toCharArray()
   * result => false</li>
   * <li> pattern = "IPL3".toCharArray()
   * name = "IPerspectiveListener3".toCharArray()
   * result => true</li>
   * <li> pattern = "HM".toCharArray()
   * name = "HashMapEntry".toCharArray()
   * result => true</li>
   * </ol></pre>
   * 
   * @param pattern the given pattern
   * @param name the given name
   * @return true if the pattern matches the given name, false otherwise
   */
  public static final boolean camelCaseMatch(char[] pattern, char[] name) {
    // null pattern is equivalent to '*'
    if (pattern == null) {
      return true;
    }

    // null name cannot match
    if (name == null) {
      return false;
    }

    return camelCaseMatch(pattern, 0, pattern.length, name, 0, name.length, false);
  }

  /**
   * Return true if the pattern matches the given name using CamelCase rules, or false otherwise.
   * char[] CamelCase matching does NOT accept explicit wild-cards '*' and '?' and is inherently
   * case sensitive.
   * <p>
   * CamelCase denotes the convention of writing compound names without spaces, and capitalizing
   * every term. This function recognizes both upper and lower CamelCase, depending whether the
   * leading character is capitalized or not. The leading part of an upper CamelCase pattern is
   * assumed to contain a sequence of capitals which are appearing in the matching name; e.g. 'NPE'
   * will match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern uses a
   * lowercase first character. In Java, type names follow the upper CamelCase convention, whereas
   * method or field names follow the lower CamelCase convention.
   * <p>
   * The pattern may contain lowercase characters, which will be matched in a case sensitive way.
   * These characters must appear in sequence in the name. For instance, 'NPExcep' will match
   * 'NullPointerException', but not 'NullPointerExCEPTION' or 'NuPoEx' will match
   * 'NullPointerException', but not 'NoPointerException'.
   * <p>
   * Digit characters are treated in a special way. They can be used in the pattern but are not
   * always considered as leading character. For instance, both 'UTF16DSS' and 'UTFDSS' patterns
   * will match 'UTF16DocumentScannerSupport'.
   * <p>
   * CamelCase can be restricted to match only the same count of parts. When this restriction is
   * specified the given pattern and the given name must have <b>exactly</b> the same number of
   * parts (i.e. the same number of uppercase characters).<br>
   * For instance, 'HM' , 'HaMa' and 'HMap' patterns will match 'HashMap' and 'HatMapper' <b>but
   * not</b> 'HashMapEntry'.
   * <p>
   * 
   * <pre>
   * Examples:<ol>
   * <li> pattern = "NPE".toCharArray()
   * name = "NullPointerException".toCharArray()
   * result => true</li>
   * <li> pattern = "NPE".toCharArray()
   * name = "NoPermissionException".toCharArray()
   * result => true</li>
   * <li> pattern = "NuPoEx".toCharArray()
   * name = "NullPointerException".toCharArray()
   * result => true</li>
   * <li> pattern = "NuPoEx".toCharArray()
   * name = "NoPermissionException".toCharArray()
   * result => false</li>
   * <li> pattern = "npe".toCharArray()
   * name = "NullPointerException".toCharArray()
   * result => false</li>
   * <li> pattern = "IPL3".toCharArray()
   * name = "IPerspectiveListener3".toCharArray()
   * result => true</li>
   * <li> pattern = "HM".toCharArray()
   * name = "HashMapEntry".toCharArray()
   * result => (samePartCount == false)</li>
   * </ol></pre>
   * 
   * @param pattern the given pattern
   * @param name the given name
   * @param samePartCount flag telling whether the pattern and the name should have the same count
   *          of parts or not.<br>
   *          &nbsp;&nbsp;For example:
   *          <ul>
   *          <li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types, but not 
   *          'HashMapEntry'</li> <li>'HMap' type string pattern will still match previous 'HashMap'
   *          and 'HtmlMapper' types, but not 'HighMagnitude'</li>
   *          </ul>
   * @return true if the pattern matches the given name, false otherwise
   */
  public static final boolean camelCaseMatch(char[] pattern, char[] name, boolean samePartCount) {
    // null pattern is equivalent to '*'
    if (pattern == null) {
      return true;
    }

    // null name cannot match
    if (name == null) {
      return false;
    }

    return camelCaseMatch(pattern, 0, pattern.length, name, 0, name.length, samePartCount);
  }

  /**
   * Return true if a sub-pattern matches the sub-part of the given name using CamelCase rules, or
   * false otherwise. char[] CamelCase matching does NOT accept explicit wild-cards '*' and '?' and
   * is inherently case sensitive. Can match only subset of name/pattern, considering end positions
   * as non-inclusive. The sub-pattern is defined by the patternStart and patternEnd positions.
   * <p>
   * CamelCase denotes the convention of writing compound names without spaces, and capitalizing
   * every term. This function recognizes both upper and lower CamelCase, depending whether the
   * leading character is capitalized or not. The leading part of an upper CamelCase pattern is
   * assumed to contain a sequence of capitals which are appearing in the matching name; e.g. 'NPE'
   * will match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern uses a
   * lowercase first character. In Java, type names follow the upper CamelCase convention, whereas
   * method or field names follow the lower CamelCase convention.
   * <p>
   * The pattern may contain lowercase characters, which will be matched in a case sensitive way.
   * These characters must appear in sequence in the name. For instance, 'NPExcep' will match
   * 'NullPointerException', but not 'NullPointerExCEPTION' or 'NuPoEx' will match
   * 'NullPointerException', but not 'NoPointerException'.
   * <p>
   * Digit characters are treated in a special way. They can be used in the pattern but are not
   * always considered as leading character. For instance, both 'UTF16DSS' and 'UTFDSS' patterns
   * will match 'UTF16DocumentScannerSupport'.
   * <p>
   * Digit characters are treated in a special way. They can be used in the pattern but are not
   * always considered as leading character. For instance, both 'UTF16DSS' and 'UTFDSS' patterns
   * will match 'UTF16DocumentScannerSupport'.
   * <p>
   * Using this method allows matching names to have more parts than the specified pattern (see
   * {@link #camelCaseMatch(char[], int, int, char[], int, int, boolean)}).<br>
   * For instance, 'HM' , 'HaMa' and 'HMap' patterns will match 'HashMap', 'HatMapper' <b>and
   * also</b> 'HashMapEntry'.
   * <p>
   * Examples:
   * <ol>
   * <li>pattern = "NPE".toCharArray() patternStart = 0 patternEnd = 3 name =
   * "NullPointerException".toCharArray() nameStart = 0 nameEnd = 20 result => true</li>
   * <li>pattern = "NPE".toCharArray() patternStart = 0 patternEnd = 3 name =
   * "NoPermissionException".toCharArray() nameStart = 0 nameEnd = 21 result => true</li>
   * <li>pattern = "NuPoEx".toCharArray() patternStart = 0 patternEnd = 6 name =
   * "NullPointerException".toCharArray() nameStart = 0 nameEnd = 20 result => true</li>
   * <li>pattern = "NuPoEx".toCharArray() patternStart = 0 patternEnd = 6 name =
   * "NoPermissionException".toCharArray() nameStart = 0 nameEnd = 21 result => false</li>
   * <li>pattern = "npe".toCharArray() patternStart = 0 patternEnd = 3 name =
   * "NullPointerException".toCharArray() nameStart = 0 nameEnd = 20 result => false</li>
   * <li>pattern = "IPL3".toCharArray() patternStart = 0 patternEnd = 4 name =
   * "IPerspectiveListener3".toCharArray() nameStart = 0 nameEnd = 21 result => true</li>
   * <li>pattern = "HM".toCharArray() patternStart = 0 patternEnd = 2 name =
   * "HashMapEntry".toCharArray() nameStart = 0 nameEnd = 12 result => true</li>
   * </ol>
   * 
   * @param pattern the given pattern
   * @param patternStart the start index of the pattern, inclusive
   * @param patternEnd the end index of the pattern, exclusive
   * @param name the given name
   * @param nameStart the start index of the name, inclusive
   * @param nameEnd the end index of the name, exclusive
   * @return true if a sub-pattern matches the sub-part of the given name, false otherwise
   */
  public static final boolean camelCaseMatch(char[] pattern, int patternStart, int patternEnd,
      char[] name, int nameStart, int nameEnd) {
    return camelCaseMatch(pattern, patternStart, patternEnd, name, nameStart, nameEnd, false);
  }

  /**
   * Return true if a sub-pattern matches the sub-part of the given name using CamelCase rules, or
   * false otherwise. char[] CamelCase matching does NOT accept explicit wild-cards '*' and '?' and
   * is inherently case sensitive. Can match only subset of name/pattern, considering end positions
   * as non-inclusive. The sub-pattern is defined by the patternStart and patternEnd positions.
   * <p>
   * CamelCase denotes the convention of writing compound names without spaces, and capitalizing
   * every term. This function recognizes both upper and lower CamelCase, depending whether the
   * leading character is capitalized or not. The leading part of an upper CamelCase pattern is
   * assumed to contain a sequence of capitals which are appearing in the matching name; e.g. 'NPE'
   * will match 'NullPointerException', but not 'NewPerfData'. A lower CamelCase pattern uses a
   * lowercase first character. In Java, type names follow the upper CamelCase convention, whereas
   * method or field names follow the lower CamelCase convention.
   * <p>
   * The pattern may contain lowercase characters, which will be matched in a case sensitive way.
   * These characters must appear in sequence in the name. For instance, 'NPExcep' will match
   * 'NullPointerException', but not 'NullPointerExCEPTION' or 'NuPoEx' will match
   * 'NullPointerException', but not 'NoPointerException'.
   * <p>
   * Digit characters are treated in a special way. They can be used in the pattern but are not
   * always considered as leading character. For instance, both 'UTF16DSS' and 'UTFDSS' patterns
   * will match 'UTF16DocumentScannerSupport'.
   * <p>
   * CamelCase can be restricted to match only the same count of parts. When this restriction is
   * specified the given pattern and the given name must have <b>exactly</b> the same number of
   * parts (i.e. the same number of uppercase characters).<br>
   * For instance, 'HM' , 'HaMa' and 'HMap' patterns will match 'HashMap' and 'HatMapper' <b>but
   * not</b> 'HashMapEntry'.
   * <p>
   * 
   * <pre>
   * Examples:
   * <ol>
   * <li> pattern = "NPE".toCharArray()
   * patternStart = 0
   * patternEnd = 3
   * name = "NullPointerException".toCharArray()
   * nameStart = 0
   * nameEnd = 20
   * result => true</li>
   * <li> pattern = "NPE".toCharArray()
   * patternStart = 0
   * patternEnd = 3
   * name = "NoPermissionException".toCharArray()
   * nameStart = 0
   * nameEnd = 21
   * result => true</li>
   * <li> pattern = "NuPoEx".toCharArray()
   * patternStart = 0
   * patternEnd = 6
   * name = "NullPointerException".toCharArray()
   * nameStart = 0
   * nameEnd = 20
   * result => true</li>
   * <li> pattern = "NuPoEx".toCharArray()
   * patternStart = 0
   * patternEnd = 6
   * name = "NoPermissionException".toCharArray()
   * nameStart = 0
   * nameEnd = 21
   * result => false</li>
   * <li> pattern = "npe".toCharArray()
   * patternStart = 0
   * patternEnd = 3
   * name = "NullPointerException".toCharArray()
   * nameStart = 0
   * nameEnd = 20
   * result => false</li>
   * <li> pattern = "IPL3".toCharArray()
   * patternStart = 0
   * patternEnd = 4
   * name = "IPerspectiveListener3".toCharArray()
   * nameStart = 0
   * nameEnd = 21
   * result => true</li>
   * <li> pattern = "HM".toCharArray()
   * patternStart = 0
   * patternEnd = 2
   * name = "HashMapEntry".toCharArray()
   * nameStart = 0
   * nameEnd = 12
   * result => (samePartCount == false)</li>
   * </ol>
   * </pre>
   * 
   * @param pattern the given pattern
   * @param patternStart the start index of the pattern, inclusive
   * @param patternEnd the end index of the pattern, exclusive
   * @param name the given name
   * @param nameStart the start index of the name, inclusive
   * @param nameEnd the end index of the name, exclusive
   * @param samePartCount flag telling whether the pattern and the name should have the same count
   *          of parts or not.<br>
   *          &nbsp;&nbsp;For example:
   *          <ul>
   *          <li>'HM' type string pattern will match 'HashMap' and 'HtmlMapper' types, but not 
   *          'HashMapEntry'</li> <li>'HMap' type string pattern will still match previous 'HashMap'
   *          and 'HtmlMapper' types, but not 'HighMagnitude'</li>
   *          </ul>
   * @return true if a sub-pattern matches the sub-part of the given name, false otherwise
   */
  public static final boolean camelCaseMatch(char[] pattern, int patternStart, int patternEnd,
      char[] name, int nameStart, int nameEnd, boolean samePartCount) {
    // null name cannot match
    if (name == null) {
      return false;
    }

    // null pattern is equivalent to '*'
    if (pattern == null) {
      return true;
    }
    if (patternEnd < 0) {
      patternEnd = pattern.length;
    }
    if (nameEnd < 0) {
      nameEnd = name.length;
    }

    if (patternEnd <= patternStart) {
      return nameEnd <= nameStart;
    }
    if (nameEnd <= nameStart) {
      return false;
    }
    // check first pattern char
    if (name[nameStart] != pattern[patternStart]) {
      // first char must strictly match (upper/lower)
      return false;
    }

    char patternChar, nameChar;
    int iPattern = patternStart;
    int iName = nameStart;

    // Main loop is on pattern characters
    while (true) {

      iPattern++;
      iName++;

      if (iPattern == patternEnd) { // we have exhausted pattern...
        // it's a match if the name can have additional parts (i.e. uppercase
        // characters) or is also exhausted
        if (!samePartCount || iName == nameEnd) {
          return true;
        }

        // otherwise it's a match only if the name has no more uppercase characters
        while (true) {
          if (iName == nameEnd) {
            // we have exhausted the name, so it's a match
            return true;
          }
          nameChar = name[iName];
          // test if the name character is uppercase
          if (!Character.isJavaIdentifierPart(nameChar) || Character.isUpperCase(nameChar)) {
            return false;
          }
          iName++;
        }
      }

      if (iName == nameEnd) {
        // We have exhausted the name (and not the pattern), so it's not a match
        return false;
      }

      // For as long as we're exactly matching, bring it on (even if it's a lower case character)
      if ((patternChar = pattern[iPattern]) == name[iName]) {
        continue;
      }

      // If characters are not equals, then it's not a match if patternChar is lowercase
      if (Character.isJavaIdentifierPart(patternChar) && !Character.isUpperCase(patternChar)
          && !Character.isDigit(patternChar)) {
        return false;
      }

      // patternChar is uppercase, so let's find the next uppercase in name
      while (true) {
        if (iName == nameEnd) {
          // We have exhausted name (and not pattern), so it's not a match
          return false;
        }

        nameChar = name[iName];
        if (Character.isJavaIdentifierPart(nameChar) && !Character.isUpperCase(nameChar)
            && !Character.isDigit(nameChar)) {
          iName++;
        } else if (Character.isDigit(nameChar)) {
          if (patternChar == nameChar) {
            break;
          }
          iName++;
        } else if (patternChar != nameChar) {
          return false;
        } else {
          break;
        }
      }
      // At this point, either name has been exhausted, or it is at an uppercase
      // letter.
      // Since pattern is also at an uppercase letter
    }
  }

  /**
   * Return true if the pattern matches the given name, false otherwise. This char[] pattern
   * matching accepts wild-cards '*' and '?'. When not case sensitive, the pattern is assumed to
   * already be lowercased, the name will be lowercased character per character as comparing. If
   * name is null, the answer is false. If pattern is null, the answer is true if name is not null. <br>
   * <br>
   * For example:
   * <ol>
   * <li>
   * 
   * <pre>
   *    pattern = { '?', 'b', '*' }
   *    name = { 'a', 'b', 'c' , 'd' }
   *    isCaseSensitive = true
   *    result => true
   * </pre>
   * </li>
   * <li>
   * 
   * <pre>
   *    pattern = { '?', 'b', '?' }
   *    name = { 'a', 'b', 'c' , 'd' }
   *    isCaseSensitive = true
   *    result => false
   * </pre>
   * </li>
   * <li>
   * 
   * <pre>
   *    pattern = { 'b', '*' }
   *    name = { 'a', 'b', 'c' , 'd' }
   *    isCaseSensitive = true
   *    result => false
   * </pre>
   * </li>
   * </ol>
   * 
   * @param pattern the given pattern
   * @param name the given name
   * @param isCaseSensitive flag to know whether or not the matching should be case sensitive
   * @return true if the pattern matches the given name, false otherwise
   */
  public static final boolean match(char[] pattern, char[] name, boolean isCaseSensitive) {

    if (name == null) {
      return false; // null name cannot match
    }
    if (pattern == null) {
      return true; // null pattern is equivalent to '*'
    }

    return match(pattern, 0, pattern.length, name, 0, name.length, isCaseSensitive);
  }

  /**
   * Return true if a sub-pattern matches the subpart of the given name, false otherwise. char[]
   * pattern matching, accepting wild-cards '*' and '?'. Can match only subset of name/pattern. end
   * positions are non-inclusive. The subpattern is defined by the patternStart and pattternEnd
   * positions. When not case sensitive, the pattern is assumed to already be lowercased, the name
   * will be lowercased character per character as comparing. <br>
   * <br>
   * For example:
   * <ol>
   * <li>
   * 
   * <pre>
   *    pattern = { '?', 'b', '*' }
   *    patternStart = 1
   *    patternEnd = 3
   *    name = { 'a', 'b', 'c' , 'd' }
   *    nameStart = 1
   *    nameEnd = 4
   *    isCaseSensitive = true
   *    result => true
   * </pre>
   * </li>
   * <li>
   * 
   * <pre>
   *    pattern = { '?', 'b', '*' }
   *    patternStart = 1
   *    patternEnd = 2
   *    name = { 'a', 'b', 'c' , 'd' }
   *    nameStart = 1
   *    nameEnd = 2
   *    isCaseSensitive = true
   *    result => false
   * </pre>
   * </li>
   * </ol>
   * 
   * @param pattern the given pattern
   * @param patternStart the given pattern start
   * @param patternEnd the given pattern end
   * @param name the given name
   * @param nameStart the given name start
   * @param nameEnd the given name end
   * @param isCaseSensitive flag to know if the matching should be case sensitive
   * @return true if a sub-pattern matches the subpart of the given name, false otherwise
   */
  public static final boolean match(char[] pattern, int patternStart, int patternEnd, char[] name,
      int nameStart, int nameEnd, boolean isCaseSensitive) {

    if (name == null) {
      return false; // null name cannot match
    }
    if (pattern == null) {
      return true; // null pattern is equivalent to '*'
    }
    int iPattern = patternStart;
    int iName = nameStart;

    if (patternEnd < 0) {
      patternEnd = pattern.length;
    }
    if (nameEnd < 0) {
      nameEnd = name.length;
    }

    /* check first segment */
    char patternChar = 0;
    while ((iPattern < patternEnd) && (patternChar = pattern[iPattern]) != '*') {
      if (iName == nameEnd) {
        return false;
      }
      if (patternChar != (isCaseSensitive ? name[iName] : toLowerCase(name[iName]))
          && patternChar != '?') {
        return false;
      }
      iName++;
      iPattern++;
    }
    /* check sequence of star+segment */
    int segmentStart;
    if (patternChar == '*') {
      segmentStart = ++iPattern; // skip star
    } else {
      segmentStart = 0; // force iName check
    }
    int prefixStart = iName;
    checkSegment : while (iName < nameEnd) {
      if (iPattern == patternEnd) {
        iPattern = segmentStart; // mismatch - restart current segment
        iName = ++prefixStart;
        continue checkSegment;
      }
      /* segment is ending */
      if ((patternChar = pattern[iPattern]) == '*') {
        segmentStart = ++iPattern; // skip start
        if (segmentStart == patternEnd) {
          return true;
        }
        prefixStart = iName;
        continue checkSegment;
      }
      /* check current name character */
      if ((isCaseSensitive ? name[iName] : toLowerCase(name[iName])) != patternChar
          && patternChar != '?') {
        iPattern = segmentStart; // mismatch - restart current segment
        iName = ++prefixStart;
        continue checkSegment;
      }
      iName++;
      iPattern++;
    }

    return (segmentStart == patternEnd) || (iName == nameEnd && iPattern == patternEnd)
        || (iPattern == patternEnd - 1 && pattern[iPattern] == '*');
  }

  private static char toLowerCase(char c) {
    return Character.toLowerCase(c);
  }
}
