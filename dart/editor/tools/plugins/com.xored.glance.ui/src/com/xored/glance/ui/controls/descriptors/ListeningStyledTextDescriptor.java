/**
 * 
 */
package com.xored.glance.ui.controls.descriptors;

import com.xored.glance.ui.controls.text.styled.ListeningStyledTextSource;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceDescriptor;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;

/**
 * @author Yuri Strot
 */
public class ListeningStyledTextDescriptor implements ITextSourceDescriptor {

  static final int LineGetStyle = 3002;

  @Override
  public ITextSource createSource(Control control) {
    return new ListeningStyledTextSource((StyledText) control);
  }

  @Override
  public boolean isValid(Control control) {
    if (control instanceof StyledText) {
      StyledText text = (StyledText) control;
      return text.isListening(LineGetStyle);
    }
    return false;
  }

}
