/**
 * 
 */
package com.xored.glance.ui.sources;

import org.eclipse.swt.widgets.Control;

/**
 * @author Yuri Strot
 */
public interface ITextSourceDescriptor {

  /**
   * Creates text source for specified control
   * 
   * @param control
   * @return
   */
  public ITextSource createSource(Control control);

  /**
   * Return a boolean indicating whether text source can be created for this control
   * 
   * @return <code>true</code> if the text source can be created, and <code>false</code> otherwise
   */
  public boolean isValid(Control control);

}
