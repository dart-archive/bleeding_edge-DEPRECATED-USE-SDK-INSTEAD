/**
 * 
 */
package com.xored.glance.ui.sources;

/**
 * @author Yuri Strot
 */
public class SourceSelection {

  private ITextBlock block;

  private int offset;

  private int length;

  public SourceSelection(ITextBlock block, int offset, int length) {
    this.block = block;
    this.offset = offset;
    this.length = length;
  }

  /**
   * @return the block
   */
  public ITextBlock getBlock() {
    return block;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

  /**
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    return block + ": (" + offset + ", " + length + ")";
  }

}
