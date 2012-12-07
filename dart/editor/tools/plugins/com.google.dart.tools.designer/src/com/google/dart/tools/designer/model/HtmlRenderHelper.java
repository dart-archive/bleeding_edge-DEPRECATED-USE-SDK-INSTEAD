/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.designer.model;

import com.google.dart.tools.designer.DartDesignerPlugin;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wb.internal.core.utils.IOUtils2;
import org.osgi.framework.Bundle;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;

public class HtmlRenderHelper {
  private static Process process;
  private static OutputStream processOutputStream;
  private static InputStream processInputStream;
  private static BufferedReader processReader;

  /**
   * @return the image of given HTML content, may be <code>null</code>.
   */
  public static Image renderImage(final String content) {
    try {
      File tempFile = File.createTempFile("htmlRender", ".html");
      try {
        IOUtils2.writeBytes(tempFile, content.getBytes());
        // start DumpRenderTree
        if (processOutputStream == null) {
          String path;
          {
            Bundle bundle = DartDesignerPlugin.getDefault().getBundle();
            URL url = bundle.getEntry("lib/DumpRenderTree.app/Contents/MacOS/DumpRenderTree");
            path = FileLocator.toFileURL(url).getPath();
          }
//        ProcessBuilder builder = new ProcessBuilder(path, "-p", tempFile.getAbsolutePath());
          ProcessBuilder builder = new ProcessBuilder(path, "-p", "-");
          builder.redirectErrorStream(true);
          Process process = builder.start();
          processOutputStream = process.getOutputStream();
          processInputStream = process.getInputStream();
          processReader = new BufferedReader(new InputStreamReader(processInputStream));
        }
        long start = System.nanoTime();
        // XXX
//        processOutputStream.write((tempFile.getAbsolutePath() + "\n").getBytes());
        processOutputStream.write(("http://127.0.0.1:3030/Users/scheglov/dart/dwc_first/web/out/dwc_first.html\n").getBytes());
        processOutputStream.flush();
        // read tree
        while (true) {
          String line = processReader.readLine();
          System.out.println(line);
          if (line.isEmpty()) {
            break;
          }
        }
        // read image
        {
          processReader.readLine(); // ActualHash:
          processReader.readLine(); // Content-Type: image/png
          String lengthLine = processReader.readLine(); // Content-Length: 5546
          int pngLength = Integer.parseInt(StringUtils.removeStart(lengthLine, "Content-Length: "));
//          System.out.println("pngLength: " + pngLength);
          char[] pngChars = new char[pngLength];
          readFully(processReader, pngChars);
          byte[] pngBytes = new String(pngChars).getBytes();
          Image image = new Image(null, new ByteArrayInputStream(pngBytes));
          System.out.println("imageTime: " + (System.nanoTime() - start) / 1000000.0);
          return image;
        }
        //
//        {
//          SessionInputBuffer buffer = new AbstractSessionInputBuffer() {
//            {
//              init(processInputStream, 1024, new BasicHttpParams());
//            }
//
//            @Override
//            public boolean isDataAvailable(int timeout) throws IOException {
//              return false;
//            }
//          };
//          LineParser lineParser = new BasicLineParser(new ProtocolVersion("HTTP", 1, 1));
//          HttpMessageParser<HttpResponse> parser = new DefaultHttpResponseParser(
//              buffer,
//              lineParser,
//              new DefaultHttpResponseFactory(),
//              new BasicHttpParams());
//          HttpResponse response = parser.parse();
//          System.out.println(response);
//          HttpParams params = new BasicHttpParams();
//          SessionInputBuffer inbuffer = new SessionInputBufferMockup(s, "US-ASCII", params);
//          HttpMessageParser<BasicHttpResponse> parser = new DefaultResponseParser(
//              inbuffer,
//              BasicLineParser.DEFAULT,
//              new DefaultHttpResponseFactory(),
//              params);
//
//          HttpResponse response = parser.parse();
//        }
//        while (true) {
//          String line = processReader.readLine();
//          System.out.println(line);
//        }
        //
//        byte[] bytes = IOUtils2.readBytes(processInputStream);
//        int exitValue = process.exitValue();
//        System.out.println("bytes: " + bytes.length);
//        System.out.println("bytesTime: " + (System.nanoTime() - start) / 1000000.0);
//        String output = new String(bytes);
//        System.out.println(StringUtils.substring(output, -10, 0));
////        System.out.println(output);
//        //
//        int pngOffset = output.indexOf("Content-Type: image/png");
//        pngOffset = output.indexOf('\n', pngOffset) + 1;
//        pngOffset = output.indexOf('\n', pngOffset) + 1;
//        Image image = new Image(null, new ByteArrayInputStream(bytes, pngOffset, bytes.length
//            - pngOffset));
//        System.out.println("imageTime: " + (System.nanoTime() - start) / 1000000.0);
//        return image;
      } finally {
        tempFile.delete();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Read the requested number of characters or fail if there are not enough left.
   * <p>
   * TODO(scheglov) update to modern Commons-IO which has this method.
   */
  private static void readFully(Reader input, char[] buffer) throws IOException {
    int length = buffer.length;
    int remaining = length;
    while (remaining > 0) {
      int location = length - remaining;
      int count = input.read(buffer, location, remaining);
      if (count == -1) {
        throw new EOFException("Length to read: " + length + " actual: " + (length - remaining));
      }
      remaining -= count;
    }
  }
}
