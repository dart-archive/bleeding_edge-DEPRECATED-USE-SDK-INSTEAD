/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.text.IColorManager;
import com.google.dart.tools.ui.text.IColorManagerExtension;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Java color manager.
 */
public class DartColorManager implements IColorManager, IColorManagerExtension {

  protected Map fKeyTable = new HashMap(10);
  protected Map fDisplayTable = new HashMap(2);

  /**
   * Flag which tells if the colors are automatically disposed when the current display gets
   * disposed.
   */
  private boolean fAutoDisposeOnDisplayDispose;

  /**
   * Creates a new Java color manager which automatically disposes the allocated colors when the
   * current display gets disposed.
   */
  public DartColorManager() {
    this(true);
  }

  /**
   * Creates a new Java color manager.
   *
   * @param autoDisposeOnDisplayDispose if <code>true</code> the color manager automatically
   *          disposes all managed colors when the current display gets disposed and all calls to
   *          {@link org.eclipse.jface.text.source.ISharedTextColors#dispose()} are ignored.
   */
  public DartColorManager(boolean autoDisposeOnDisplayDispose) {
    fAutoDisposeOnDisplayDispose = autoDisposeOnDisplayDispose;
  }

  /*
   * @see IColorManagerExtension#bindColor(String, RGB)
   */
  @Override
  public void bindColor(String key, RGB rgb) {
    Object value = fKeyTable.get(key);
    if (value != null) {
      throw new UnsupportedOperationException();
    }

    fKeyTable.put(key, rgb);
  }

  /*
   * @see IColorManager#dispose
   */
  @Override
  public void dispose() {
    if (!fAutoDisposeOnDisplayDispose) {
      dispose(Display.getCurrent());
    }
  }

  public void dispose(Display display) {
    Map colorTable = (Map) fDisplayTable.get(display);
    if (colorTable != null) {
      Iterator e = colorTable.values().iterator();
      while (e.hasNext()) {
        Color color = (Color) e.next();
        if (color != null && !color.isDisposed()) {
          color.dispose();
        }
      }
    }
  }

  /*
   * @see IColorManager#getColor(RGB)
   */
  @Override
  public Color getColor(RGB rgb) {

    if (rgb == null) {
      return null;
    }

    final Display display = Display.getCurrent();
    Map colorTable = (Map) fDisplayTable.get(display);
    if (colorTable == null) {
      colorTable = new HashMap(10);
      fDisplayTable.put(display, colorTable);
      if (fAutoDisposeOnDisplayDispose) {
        display.disposeExec(new Runnable() {
          @Override
          public void run() {
            dispose(display);
          }
        });
      }
    }

    Color color = (Color) colorTable.get(rgb);
    if (color == null) {
      color = new Color(Display.getCurrent(), rgb);
      colorTable.put(rgb, color);
    }

    return color;
  }

  /*
   * @see IColorManager#getColor(String)
   */
  @Override
  public Color getColor(String key) {

    if (key == null) {
      return null;
    }

    RGB rgb = (RGB) fKeyTable.get(key);
    return getColor(rgb);
  }

  /*
   * @see IColorManagerExtension#unbindColor(String)
   */
  @Override
  public void unbindColor(String key) {
    fKeyTable.remove(key);
  }
}
