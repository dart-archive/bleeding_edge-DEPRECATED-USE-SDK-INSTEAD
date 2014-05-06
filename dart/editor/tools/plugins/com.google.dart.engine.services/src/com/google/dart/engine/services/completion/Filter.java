package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.utilities.translation.DartBlockBody;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

class Filter {
  String prefix;
  String originalPrefix;
  Pattern pattern;

  Filter(SimpleIdentifier ident, int loc) {
    this(ident.getName(), ident.getOffset(), loc);
  }

  Filter(String name, int pos, int loc) {
    int len = loc - pos;
    if (len > 0) {
      if (len <= name.length()) {
        prefix = name.substring(0, len);
      } else {
        prefix = name;
      }
    } else {
      prefix = "";
    }
    originalPrefix = prefix;
    prefix = prefix.toLowerCase();
  }

  /**
   * @return {@code true} if the given name starts with the same prefix as used for filter.
   */
  boolean isSameCasePrefix(String name) {
    return name.startsWith(originalPrefix);
  }

  @DartBlockBody({"// TODO(scheglov) translate it", "return null;"})
  String makePattern() {
    String source = originalPrefix;
    if (source == null || source.length() < 2) {
      return "*";
    }
    int index = 0;
    StringBuffer regex = new StringBuffer();
    StringBuffer pattern = new StringBuffer();
    regex.append(source.charAt(index));
    pattern.append(source.charAt(index++));
    while (index < source.length()) {
      char ch = source.charAt(index++);
      if (Character.isUpperCase(ch)) {
        pattern.append('*');
        regex.append("\\p{javaLowerCase}*");
      }
      pattern.append(ch);
      regex.append(ch);
    }
    pattern.append('*');
    regex.append("\\p{javaLowerCase}*");
    String result = pattern.toString();
    this.pattern = Pattern.compile(regex.toString(), 0);
    return result;
  }

  boolean match(Element elem) {
    return match(elem.getDisplayName());
  }

  boolean match(String name) {
    // Return true if the filter passes.
    if (name.toLowerCase().startsWith(prefix)) {
      return true;
    }
    return matchPattern(name);
  }

  void removeNotMatching(List<Element> elements) {
    for (Iterator<Element> I = elements.iterator(); I.hasNext();) {
      Element element = I.next();
      if (!match(element)) {
        I.remove();
      }
    }
  }

  @DartBlockBody({"// TODO(scheglov) translate it", "return false;"})
  private boolean matchPattern(String name) {
    return pattern != null && pattern.matcher(name).matches();
  }
}
