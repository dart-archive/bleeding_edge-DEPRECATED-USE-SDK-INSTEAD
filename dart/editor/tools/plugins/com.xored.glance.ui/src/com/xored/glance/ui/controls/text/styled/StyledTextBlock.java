/**
 * 
 */
package com.xored.glance.ui.controls.text.styled;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;

import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.ITextBlockListener;
import com.xored.glance.ui.sources.TextChangedEvent;

/**
 * @author Yuri Strot
 */
public class StyledTextBlock implements ITextBlock, ExtendedModifyListener {

  public StyledTextBlock(StyledText text) {
    this.text = text;
    listeners = new ListenerList();
    text.addExtendedModifyListener(this);
  }

  public String getText() {
    return text.getText();
  }

  public void addTextBlockListener(ITextBlockListener listener) {
    listeners.add(listener);
  }

  public void removeTextBlockListener(ITextBlockListener listener) {
    listeners.remove(listener);
  }

  public void modifyText(ExtendedModifyEvent event) {
    Object[] objects = listeners.getListeners();
    TextChangedEvent textEvent = new TextChangedEvent(event.start, event.length, event.replacedText);
    for (Object object : objects) {
      ITextBlockListener listener = (ITextBlockListener) object;
      listener.textChanged(textEvent);
    }
  }

  public int compareTo(ITextBlock o) {
    //style text support only one text block
    return 0;
  }

  private StyledText text;
  private ListenerList listeners;

}
