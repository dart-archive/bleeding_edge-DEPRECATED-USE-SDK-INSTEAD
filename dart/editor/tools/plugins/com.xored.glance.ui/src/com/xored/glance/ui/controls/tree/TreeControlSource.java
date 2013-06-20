/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.tree;

import org.eclipse.swt.widgets.Tree;

public class TreeControlSource extends TreeStructSource {

  public TreeControlSource(Tree tree) {
    super(tree);
  }

  @Override
  protected TreeContent createContent() {
    return new TreeControlContent(getControl());
  }

}
