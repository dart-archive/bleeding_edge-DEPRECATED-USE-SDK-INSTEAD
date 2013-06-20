/**
 * 
 */
package com.xored.glance.ui.sources;

/**
 * @author Yuri Strot
 */
public interface ITextSourceListener {

  /**
   * Notification about block changing
   * 
   * @param removed
   * @param added
   */
  public void blocksChanged(ITextBlock[] removed, ITextBlock[] added);

  /**
   * Notification about all blocks removed and added abother blocks
   * 
   * @param newBlocks
   */
  public void blocksReplaced(ITextBlock[] newBlocks);

  /**
   * Notification about selection changing
   * 
   * @param selection
   */
  public void selectionChanged(SourceSelection selection);

}
