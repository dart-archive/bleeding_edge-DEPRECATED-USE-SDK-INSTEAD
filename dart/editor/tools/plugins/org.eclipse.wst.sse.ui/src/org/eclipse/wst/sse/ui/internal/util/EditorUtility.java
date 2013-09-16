/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.util;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * Helpful methods to be used with the Source Editor
 */
public class EditorUtility {

  /**
   * Appends to modifier string of the given SWT modifier bit to the given modifierString.
   * 
   * @param modifierString the modifier string
   * @param modifier an int with SWT modifier bit
   * @return the concatenated modifier string
   */
  private static String appendModifierString(String modifierString, int modifier) {
    if (modifierString == null)
      modifierString = ""; //$NON-NLS-1$
    String newModifierString = Action.findModifierString(modifier);
    if (modifierString.length() == 0)
      return newModifierString;
    return modifierString + " + " + newModifierString; //$NON-NLS-1$
  }

  /**
   * Computes the state mask for the given modifier string.
   * 
   * @param modifiers the string with the modifiers, separated by '+', '-', ';', ',' or '.'
   * @return the state mask or -1 if the input is invalid
   */
  public static int computeStateMask(String modifiers) {
    if (modifiers == null)
      return -1;

    if (modifiers.length() == 0)
      return SWT.NONE;

    int stateMask = 0;
    StringTokenizer modifierTokenizer = new StringTokenizer(modifiers, ",;.:+-* "); //$NON-NLS-1$
    while (modifierTokenizer.hasMoreTokens()) {
      int modifier = EditorUtility.findLocalizedModifier(modifierTokenizer.nextToken());
      if (modifier == 0 || (stateMask & modifier) == modifier)
        return -1;
      stateMask = stateMask | modifier;
    }
    return stateMask;
  }

  /**
   * Maps the localized modifier name to a code in the same manner as #findModifier.
   * 
   * @return the SWT modifier bit, or <code>0</code> if no match was found
   * @see findModifier
   */
  public static int findLocalizedModifier(String token) {
    if (token == null)
      return 0;

    if (token.equalsIgnoreCase(Action.findModifierString(SWT.CTRL)))
      return SWT.CTRL;
    if (token.equalsIgnoreCase(Action.findModifierString(SWT.SHIFT)))
      return SWT.SHIFT;
    if (token.equalsIgnoreCase(Action.findModifierString(SWT.ALT)))
      return SWT.ALT;
    if (token.equalsIgnoreCase(Action.findModifierString(SWT.COMMAND)))
      return SWT.COMMAND;

    return 0;
  }

  /**
   * Temporary method to help migrate from using StructuredTextColors to using base ColorRegistry.
   * Instead of using symbolic names in the color registry, we are currently mapping the
   * rgb.toString value to corresponding color.
   * 
   * @param rgb
   * @return Color
   */
  public static Color getColor(RGB rgb) {
    if (rgb == null)
      return null;

    // get the color from the platform color registry
    Color color = JFaceResources.getColorRegistry().get(rgb.toString());

    // if the platform color registry does not have this color yet, add to
    // the registry
    if (color == null) {
      JFaceResources.getColorRegistry().put(rgb.toString(), rgb);
      color = JFaceResources.getColorRegistry().get(rgb.toString());
    }
    return color;
  }

  /**
   * Returns the modifier string for the given SWT modifier modifier bits.
   * 
   * @param stateMask the SWT modifier bits
   * @return the modifier string
   */
  public static String getModifierString(int stateMask) {
    String modifierString = ""; //$NON-NLS-1$
    if ((stateMask & SWT.CTRL) == SWT.CTRL)
      modifierString = appendModifierString(modifierString, SWT.CTRL);
    if ((stateMask & SWT.ALT) == SWT.ALT)
      modifierString = appendModifierString(modifierString, SWT.ALT);
    if ((stateMask & SWT.SHIFT) == SWT.SHIFT)
      modifierString = appendModifierString(modifierString, SWT.SHIFT);
    if ((stateMask & SWT.COMMAND) == SWT.COMMAND)
      modifierString = appendModifierString(modifierString, SWT.COMMAND);

    return modifierString;
  }
}
