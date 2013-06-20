/**
 * 
 */
package com.xored.glance.internal.ui.search;

import java.util.regex.Pattern;

import org.eclipse.jface.preference.IPreferenceStore;

import com.xored.glance.internal.ui.GlancePlugin;
import com.xored.glance.internal.ui.preferences.IPreferenceConstants;

/**
 * @author Yuri Strot
 */
public class SearchRule implements IPreferenceConstants {

  /**
   * @param text
   * @param caseSensitive
   * @param prefix
   */
  public SearchRule(String text) {
    this.text = text;
    loadFromPref();
  }

  /**
   * @param text
   * @param caseSensitive
   * @param prefix
   */
  public SearchRule(String text, boolean caseSensitive, boolean camelCase, boolean wordPrefix,
      boolean regExp) {
    this.text = text;
    this.caseSensitive = caseSensitive;
    this.camelCase = camelCase;
    this.wordPrefix = wordPrefix;
    this.regExp = regExp;
  }

  /**
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * @return the caseSensitive
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  /**
   * @return the regExp
   */
  public boolean isRegExp() {
    return regExp;
  }

  /**
   * @return the prefix
   */
  public boolean isWordPrefix() {
    return wordPrefix;
  }

  /**
   * @return the camelCase
   */
  public boolean isCamelCase() {
    return camelCase;
  }

  /**
   * @return the pattern
   */
  public Pattern getPattern() {
    if (pattern == null) {
      pattern = SearchUtils.createPattern(text, caseSensitive, regExp, wordPrefix, camelCase);
    }
    return pattern;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    result = prime * result + (caseSensitive ? 1231 : 1237);
    result = prime * result + (regExp ? 1231 : 1237);
    result = prime * result + (wordPrefix ? 1231 : 1237);
    result = prime * result + (camelCase ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SearchRule other = (SearchRule) obj;
    if (!isSettingsEqual(other))
      return false;
    return isTextEquals(other);
  }

  public boolean isSettingsEqual(SearchRule other) {
    if (other == null)
      return false;
    if (caseSensitive != other.caseSensitive)
      return false;
    if (regExp != other.regExp)
      return false;
    if (wordPrefix != other.wordPrefix)
      return false;
    if (camelCase != other.camelCase)
      return false;
    return true;
  }

  public boolean isTextEquals(SearchRule other) {
    if (other == null)
      return false;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    return true;
  }

  private void loadFromPref() {
    IPreferenceStore preferences = GlancePlugin.getDefault().getPreferenceStore();
    caseSensitive = preferences.getBoolean(SEARCH_CASE_SENSITIVE);
    regExp = preferences.getBoolean(SEARCH_REGEXP);
    wordPrefix = preferences.getBoolean(SEARCH_WORD_PREFIX);
    camelCase = preferences.getBoolean(SEARCH_CAMEL_CASE);
  }

  private Pattern pattern;
  private String text;
  private boolean caseSensitive;
  private boolean regExp;
  private boolean wordPrefix;
  private boolean camelCase;

}
