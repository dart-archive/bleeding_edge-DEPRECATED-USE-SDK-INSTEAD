/**
 * 
 */
package com.xored.glance.ui.sources;

/**
 * @author Yuri Strot
 */
public class Match implements Comparable<Match> {

  public static final Match[] EMPTY = new Match[0];

  public Match(ITextBlock block, int offset, int length) {
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
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

  public int compareTo(Match match) {
    if (block != null && match.block != null) {
      int diff = block.compareTo(match.block);
      if (diff != 0)
        return diff;
    }
    return offset - match.offset;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((block == null) ? 0 : block.hashCode());
    result = prime * result + length;
    result = prime * result + offset;
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
    Match other = (Match) obj;
    if (block == null) {
      if (other.block != null)
        return false;
    } else if (!block.equals(other.block))
      return false;
    if (length != other.length)
      return false;
    if (offset != other.offset)
      return false;
    return true;
  }

  private ITextBlock block;
  private int offset;
  private int length;

}
