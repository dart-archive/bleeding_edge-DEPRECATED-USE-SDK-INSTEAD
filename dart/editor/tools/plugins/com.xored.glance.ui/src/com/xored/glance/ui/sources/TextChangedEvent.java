/**
 * 
 */
package com.xored.glance.ui.sources;

/**
 * @author Yuri Strot
 */
public class TextChangedEvent {

  /** start offset of the new text */
  private int start;

  /** length of the new text */
  private int length;

  /** replaced text or empty string if no text was replaced */
  private String replacedText;

  public TextChangedEvent(int start, int length, String replacedText) {
    this.start = start;
    this.length = length;
    this.replacedText = replacedText;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

  /**
   * @return the replacedText
   */
  public String getReplacedText() {
    return replacedText;
  }

  /**
   * @return the start
   */
  public int getStart() {
    return start;
  }

}
