/*******************************************************************************
 * Copyright (c) 2012 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and implementation (Yuri Strot)
 ******************************************************************************/
package com.xored.glance.ui.controls.decor;

public abstract class Cell {

  private int column;

  public Cell(int column) {
    this.column = column;
  }

  @Override
  public boolean equals(Object obj) {
    Cell cell = (Cell) obj;
    return cell.getElement().equals(this.getElement()) && cell.column == column;
  }

  public int getColumn() {
    return column;
  }

  @Override
  public int hashCode() {
    return getElement().hashCode() ^ column;
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("(");
    buffer.append(getElement());
    buffer.append(", ");
    buffer.append(column);
    buffer.append(")");
    return buffer.toString();
  }

  protected abstract Object getElement();

}
