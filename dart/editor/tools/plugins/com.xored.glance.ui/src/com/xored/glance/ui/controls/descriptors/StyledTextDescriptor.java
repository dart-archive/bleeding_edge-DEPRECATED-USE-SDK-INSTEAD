/**
 * 
 */
package com.xored.glance.ui.controls.descriptors;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;

import com.xored.glance.ui.controls.text.styled.StyledTextSource;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceDescriptor;

/**
 * @author Yuri Strot
 */
public class StyledTextDescriptor implements ITextSourceDescriptor {

  @Override
  public ITextSource createSource(Control control) {
    return new StyledTextSource((StyledText) control);
  }

  @Override
  public boolean isValid(Control control) {
    return control instanceof StyledText;
  }

}
