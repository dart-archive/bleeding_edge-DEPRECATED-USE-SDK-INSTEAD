/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.table;

import com.xored.glance.ui.controls.decor.StructCell;
import com.xored.glance.ui.controls.decor.StructSource;
import com.xored.glance.ui.sources.ITextBlock;
import com.xored.glance.ui.sources.SourceSelection;

import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class TableStructSource extends StructSource {

  public TableStructSource(Table table) {
    super(table);
    table.addSelectionListener(this);
  }

  @Override
  public void dispose() {
    super.dispose();
    getControl().removeSelectionListener(this);
  }

  @Override
  public Table getControl() {
    return (Table) super.getControl();
  }

  @Override
  protected StructCell createCell(Item item, int column) {
    return new TableCell((TableItem) item, column);
  }

  @Override
  protected TableContent createContent() {
    return new TableContent(getControl());
  }

  @Override
  protected SourceSelection getSourceSelection() {
    TableItem[] items = getControl().getSelection();
    if (items.length > 0) {
      ITextBlock block = content.getContent(createCell(items[0], 0));
      if (block != null) {
        return new SourceSelection(block, 0, block.getText().length());
      }
    }
    return null;
  }

}
