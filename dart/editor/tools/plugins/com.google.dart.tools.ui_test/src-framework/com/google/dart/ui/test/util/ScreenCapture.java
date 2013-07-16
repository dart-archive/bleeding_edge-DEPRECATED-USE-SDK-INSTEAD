/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.ui.test.util;

import com.google.dart.tools.ui.Activator;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

/**
 * Dumps a <em>png</em> of the current screen in the standard "captures" output directory (as
 * defined by {@link ScreenCapture#getOutputLocation()}).
 * <p>
 * Since taking screen shots consumes considerable resources, heap limits may be exceeded during a
 * capture. To address this, {@link OutOfMemoryError}s are caught and the capture retried after a
 * short interval ( {@link ScreenCapture#CAPTURE_RETRY_INTERVAL}). The number of retries is bounded
 * by {@link ScreenCapture#MAX_CAPTURE_RETRIES}.
 */
public class ScreenCapture {

  public static final long CAPTURE_RETRY_INTERVAL = TimeUnit.SECONDS.toMillis(3);
  public static final int MAX_CAPTURE_RETRIES = 5;

  public static final String OUTPUT_DIR = "captures";

  private static final String BASE_IMAGE_NAME = "screenshot";
  private static final String IMAGE_EXT = "png";
  private static final String PATH_DELIM = System.getProperty("file.separator");

  // Keep a static Robot around to do the screencapture
  private static Robot robot;

  // Increment counter for unique screenshot file names on a per run basis
  private static int counter = 0;

  static {
    try {
      robot = new Robot();
    } catch (AWTException e) {
      Activator.logError(e);
    }
  }

  private static BufferedImage captureScreen() {

    Toolkit toolkit = Toolkit.getDefaultToolkit();
    Dimension screenSize = toolkit.getScreenSize();
    Rectangle screenRect = new Rectangle(screenSize);

    for (int i = 0; i < MAX_CAPTURE_RETRIES; ++i) {
      try {
        return robot.createScreenCapture(screenRect);
      } catch (OutOfMemoryError e) {
        Activator.logError("OutOfMemoryError caught in screen capture (attempt [" + i + "])");
        try {
          Thread.sleep(CAPTURE_RETRY_INTERVAL);
        } catch (InterruptedException ie) {
          //Just continue
        }
      }
    }
    Activator.logError("Screen Capture failed");
    return null;
  }

  private static File createScreenCaptureFile(BufferedImage image, String name) {
    File file;

    try {
      ensureOutputDirExists();
      file = writeFile(image, name);
    } catch (IOException e) {
      Activator.logError(e);
      return null;
    }

    return file;
  }

  private static void ensureOutputDirExists() {
    File dir = new File(OUTPUT_DIR);
    if (!dir.exists()) {
      dir.mkdir();
    }
  }

  private static String getOutputLocation() {
    return OUTPUT_DIR + PATH_DELIM;
  }

  private static File writeFile(BufferedImage image, String name) throws IOException {
    String path = getOutputLocation() + name + "_" + BASE_IMAGE_NAME + "_" + counter++ + "."
        + IMAGE_EXT;
    File file = new File(path);
    ImageIO.write(image, IMAGE_EXT, file);
    return file;
  }

  /**
   * Save the screen pixels as a PNG image file in the {@value #OUTPUT_DIR} directory. Existing
   * screen capture files will be overwritten. The name parameter will be used as a prefix for the
   * name of the produced image.
   * 
   * @return the file into which the image was stored, or <code>null</code> if the image could not
   *         be stored.
   */
  public File createScreenCapture(String name) {
    BufferedImage image = captureScreen();
    if (image == null) {
      return null;
    }
    return createScreenCaptureFile(image, name);
  }

}
