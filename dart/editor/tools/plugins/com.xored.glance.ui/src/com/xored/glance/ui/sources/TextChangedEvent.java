/**
 * 
 */
package com.xored.glance.ui.sources;

/**
 * @author Yuri Strot
 */
public class TextChangedEvent {

  public TextChangedEvent(int start, int length, String replacedText) {
    this.start = start;
    this.length = length;
    this.replacedText = replacedText;
  }

  /**
   * @return the start
   */
  public int getStart() {
    return start;
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

  /** start offset of the new text */
  private int start;
  /** length of the new text */
  private int length;
  /** replaced text or empty string if no text was replaced */
  private String replacedText;

}
