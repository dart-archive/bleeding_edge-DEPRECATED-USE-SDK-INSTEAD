/**
 * 
 */
package com.xored.glance.ui.utils;

import org.eclipse.core.runtime.ListenerList;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.sources.TextChangedEvent;

/**
 * @author Yuri Strot
 */
public class UITextBlock implements ITextBlock, ITextBlockListener {

  public UITextBlock(ITextBlock block) {
    this.block = block;
    text = block.getText();
    block.addTextBlockListener(this);
  }

  public void dispose() {
    block.removeTextBlockListener(this);
  }

  /**
   * @return the block
   */
  public ITextBlock getBlock() {
    return block;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextBlock#getText()
   */
  public String getText() {
    return text;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextBlock#addTextBlockListener(com.xored
   * .glance.ui.sources.ITextBlockListener)
   */
  public void addTextBlockListener(ITextBlockListener listener) {
    listeners.add(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextBlock#removeTextBlockListener(com.xored
   * .glance.ui.sources.ITextBlockListener)
   */
  public void removeTextBlockListener(ITextBlockListener listener) {
    listeners.remove(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.xored.glance.ui.sources.ITextBlockListener#textChanged(com.xored.
   * glance.ui.sources.TextChangedEvent)
   */
  public void textChanged(TextChangedEvent event) {
    text = block.getText();
    Object[] objects = listeners.getListeners();
    for (Object object : objects) {
      ITextBlockListener listener = (ITextBlockListener) object;
      listener.textChanged(event);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(ITextBlock block) {
    return this.block.compareTo(((UITextBlock) block).block);
  }

  @Override
  public String toString() {
    return "UI(" + block + ")";
  }

  private ListenerList listeners = new ListenerList();
  private ITextBlock block;
  private String text;

}
