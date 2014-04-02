/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html Contributors:
 * xored software, Inc. - initial API and Implementation (Yuri Strot)
 *******************************************************************************/
package com.xored.glance.ui.controls.text.styled;

import org.eclipse.swt.custom.StyleRange;

/**
 * @author Yuri Strot
 */
public class RangeGroup {

  private int start;
  private int end;
  private StyleRange[] ranges;

  public RangeGroup(int start, int end, StyleRange[] ranges) {
    this.start = start;
    this.end = end;
    this.ranges = ranges;
  }

  public int getEnd() {
    return end;
  }

  public StyleRange[] getRanges() {
    return ranges;
  }

  public int getStart() {
    return start;
  }

}
