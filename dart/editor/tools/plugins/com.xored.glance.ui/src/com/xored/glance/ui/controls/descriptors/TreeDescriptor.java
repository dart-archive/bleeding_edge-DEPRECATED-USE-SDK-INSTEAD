/**
 * 
 */
package com.xored.glance.ui.controls.descriptors;

import com.xored.glance.ui.controls.tree.TreeControlSource;
import com.xored.glance.ui.sources.ITextSource;
import com.xored.glance.ui.sources.ITextSourceDescriptor;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;

/**
 * @author Yuri Strot
 */
public class TreeDescriptor implements ITextSourceDescriptor {

  @Override
  public ITextSource createSource(Control control) {
    return new TreeControlSource((Tree) control);
  }

  @Override
  public boolean isValid(Control control) {
    return control instanceof Tree;
  }

}
