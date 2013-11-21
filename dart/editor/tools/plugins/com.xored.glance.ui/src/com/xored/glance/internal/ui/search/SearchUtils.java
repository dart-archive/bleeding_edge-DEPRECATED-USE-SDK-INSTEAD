/**
 * 
 */
package com.xored.glance.internal.ui.search;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Yuri Strot
 */
public class SearchUtils {

  private static class CamelCaseBuilder extends RegExpBulder {

    private boolean wordPrev = false;

    @Override
    public String asRegExp(char ch) {
      String regExp = super.asRegExp(ch);
      if (regExp != null) {
        wordPrev = false;
      } else {
        boolean word = isWord(ch);
        if (word && wordPrev) {
          regExp = CAMEL_CASE_SKIP + ch;
        }
        wordPrev = word;
      }
      return regExp;
    }

    private boolean isWord(char ch) {
      return ch == '_' || Character.isLetterOrDigit(ch);
    }

  }

  private static class RegExpBulder {

    public String asRegExp(char ch) {
      switch (ch) {
      // the backslash
        case '\\':
          return "\\\\"; //$NON-NLS-1$
          // characters that need to be escaped in the regex.
        case '(':
        case ')':
        case '{':
        case '}':
        case '.':
        case '[':
        case ']':
        case '$':
        case '^':
        case '+':
        case '|':
        case '?':
        case '*':
          StringBuffer buffer = new StringBuffer();
          buffer.append('\\');
          buffer.append(ch);
          return buffer.toString();
        default:
          return null;
      }
    }

  }

  private static final String CAMEL_CASE_SKIP = "\\w*";

  public static Pattern createPattern(String pattern, boolean caseSensitive, boolean regExSearch,
      boolean wordPrefix, boolean camelCase) {
    if (pattern == null || pattern.length() == 0) {
      return null;
    }

    int patternFlags = 0;

    if (!caseSensitive) {
      patternFlags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    }

    if (regExSearch) {
      patternFlags |= Pattern.MULTILINE;
      pattern = substituteLinebreak(pattern);
    } else {
      RegExpBulder builder = camelCase ? new CamelCaseBuilder() : new RegExpBulder();
      pattern = asRegPattern(pattern, builder);
      if (wordPrefix) {
        pattern = "\\b" + pattern; //$NON-NLS-1$
      }
    }

    return Pattern.compile(pattern, patternFlags);
  }

  /**
   * Converts a non-regex string to a pattern that can be used with the regex search engine.
   * 
   * @param string the non-regex pattern
   * @return the string converted to a regex pattern
   */
  private static String asRegPattern(String string, RegExpBulder builder) {
    StringBuffer out = new StringBuffer(string.length());
    boolean quoting = false;

    for (int i = 0, length = string.length(); i < length; i++) {
      char ch = string.charAt(i);
      String re = builder.asRegExp(ch);
      if (re != null) {
        if (quoting) {
          out.append("\\E"); //$NON-NLS-1$
          quoting = false;
        }
        out.append(re);
        continue;
      }
      if (!quoting) {
        out.append("\\Q"); //$NON-NLS-1$
        quoting = true;
      }
      out.append(ch);
    }
    if (quoting) {
      out.append("\\E"); //$NON-NLS-1$
    }

    return out.toString();
  }

  /**
   * Substitutes \R in a regex find pattern with (?>\r\n?|\n)
   * 
   * @param findString the original find pattern
   * @return the transformed find pattern
   * @throws PatternSyntaxException if \R is added at an illegal position (e.g. in a character set)
   */
  private static String substituteLinebreak(String findString) throws PatternSyntaxException {
    int length = findString.length();
    StringBuffer buf = new StringBuffer(length);

    int inCharGroup = 0;
    int inBraces = 0;
    boolean inQuote = false;
    for (int i = 0; i < length; i++) {
      char ch = findString.charAt(i);
      switch (ch) {
        case '[':
          buf.append(ch);
          if (!inQuote) {
            inCharGroup++;
          }
          break;

        case ']':
          buf.append(ch);
          if (!inQuote) {
            inCharGroup--;
          }
          break;

        case '{':
          buf.append(ch);
          if (!inQuote && inCharGroup == 0) {
            inBraces++;
          }
          break;

        case '}':
          buf.append(ch);
          if (!inQuote && inCharGroup == 0) {
            inBraces--;
          }
          break;

        case '\\':
          if (i + 1 < length) {
            char ch1 = findString.charAt(i + 1);
            if (inQuote) {
              if (ch1 == 'E') {
                inQuote = false;
              }
              buf.append(ch).append(ch1);
              i++;

            } else if (ch1 == 'R') {
              if (inCharGroup > 0 || inBraces > 0) {
                throw new PatternSyntaxException("Illegal position for \\R", findString, i);
              }
              buf.append("(?>\\r\\n?|\\n)"); //$NON-NLS-1$
              i++;

            } else {
              if (ch1 == 'Q') {
                inQuote = true;
              }
              buf.append(ch).append(ch1);
              i++;
            }
          } else {
            buf.append(ch);
          }
          break;

        default:
          buf.append(ch);
          break;
      }

    }
    return buf.toString();
  }

}
