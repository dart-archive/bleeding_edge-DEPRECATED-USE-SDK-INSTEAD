/**
 * 
 */
package com.xored.glance.ui.controls.text.styled;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.sources.TextChangedEvent;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;

/**
 * @author Yuri Strot
 */
public class StyledTextBlock implements ITextBlock, ExtendedModifyListener {

  private StyledText text;

  private ListenerList listeners;

  public StyledTextBlock(StyledText text) {
    this.text = text;
    listeners = new ListenerList();
    text.addExtendedModifyListener(this);
  }

  @Override
  public void addTextBlockListener(ITextBlockListener listener) {
    listeners.add(listener);
  }

  @Override
  public int compareTo(ITextBlock o) {
    //style text support only one text block
    return 0;
  }

  @Override
  public String getText() {
    return text.getText();
  }

  @Override
  public void modifyText(ExtendedModifyEvent event) {
    Object[] objects = listeners.getListeners();
    TextChangedEvent textEvent = new TextChangedEvent(event.start, event.length, event.replacedText);
    for (Object object : objects) {
      ITextBlockListener listener = (ITextBlockListener) object;
      listener.textChanged(textEvent);
    }
  }

  @Override
  public void removeTextBlockListener(ITextBlockListener listener) {
    listeners.remove(listener);
  }

}
