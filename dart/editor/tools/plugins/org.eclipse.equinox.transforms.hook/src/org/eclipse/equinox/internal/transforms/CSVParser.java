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
package org.eclipse.equinox.internal.transforms;

import org.eclipse.osgi.framework.log.FrameworkLogEntry;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This class is used by the transformer hook to parse urls provided by transform developers that
 * specifies the particular transforms that should be utilized for a particular transformer. TODO:
 * factor this out into a new type of service the transformer uses. Then there could be CSV
 * transforms, programatic transforms, etc.
 */
public class CSVParser {

  /**
   * Parse the given url as a CSV file containing transform tuples. The tuples have the form:
   * 
   * <pre>
   * bundleRegex,pathRegex,transformerResource
   * </pre>
   * 
   * @param transformMapURL the map url
   * @return an array of tuples derived from the contents of the file
   * @throws IOException thrown if there are issues parsing the file
   */
  public static TransformTuple[] parse(URL transformMapURL) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(transformMapURL.openStream()));
    String currentLine = null;
    List<TransformTuple> list = new ArrayList<TransformTuple>();
    while ((currentLine = reader.readLine()) != null) {
      if (currentLine.startsWith("#")) { //$NON-NLS-1$
        continue;
      }
      currentLine = currentLine.trim();
      if (currentLine.length() == 0) {
        continue;
      }
      StringTokenizer toker = new StringTokenizer(currentLine, ","); //$NON-NLS-1$
      try {
        String bundlePatternString = toker.nextToken().trim();
        String pathPatternString = toker.nextToken().trim();
        String transformPath = toker.nextToken().trim();
        try {
          Pattern bundlePattern = Pattern.compile(bundlePatternString);
          Pattern pathPattern = Pattern.compile(pathPatternString);
          URL transformerURL = new URL(transformMapURL, transformPath);
          try {
            try {
              transformerURL.openStream();
            } catch (FileNotFoundException ex) {
              if (!transformPath.endsWith(".class")) { //$NON-NLS-1$
                throw ex;
              }
              // Development class paths are in the /bin directory
              String binPath = "/bin" + transformPath; //$NON-NLS-1$
              URL binURL = new URL(transformMapURL, binPath);
              binURL.openStream();
              transformPath = binPath;
              transformerURL = binURL;
            }
            if (transformPath.endsWith(".class") && transformerURL.getProtocol().equals("bundleentry")) { //$NON-NLS-1$ $NON-NLS-2$
              addTuplesForClass(list, bundlePatternString, pathPatternString, transformerURL,
                  transformPath);
            } else {
              TransformTuple tuple = new TransformTuple();
              tuple.bundlePattern = bundlePattern;
              tuple.pathPattern = pathPattern;
              tuple.transformerUrl = transformerURL;
              list.add(tuple);
            }
          } catch (IOException e) {
            TransformerHook.log(FrameworkLogEntry.ERROR,
                "Could not add transform :" + transformerURL.toString(), e); //$NON-NLS-1$
          }
        } catch (PatternSyntaxException e) {
          TransformerHook.log(FrameworkLogEntry.ERROR,
              "Could not add compile transform matching regular expression", e); //$NON-NLS-1$
        }

      } catch (NoSuchElementException e) {
        TransformerHook.log(FrameworkLogEntry.ERROR,
            "Could not parse transform file record :" + currentLine, e); //$NON-NLS-1$
      }
    }
    return list.toArray(new TransformTuple[list.size()]);
  }

  private static void addTuplesForClass(List<TransformTuple> list, String bundlePatternString,
      String pathPatternString, URL transformerURL, String transformPath) {
//    try {
//      BundleURLConnection conn = (BundleURLConnection) transformerURL.openConnection();
    Pattern bundlePattern = Pattern.compile(bundlePatternString);
    Pattern pathPattern = Pattern.compile(pathPatternString);
    TransformTuple tuple = new TransformTuple();
    tuple.bundlePattern = bundlePattern;
    tuple.pathPattern = pathPattern;
    tuple.transformerUrl = transformerURL;
    list.add(tuple);
//    } catch (IOException ex) {
//    }
  }
}
