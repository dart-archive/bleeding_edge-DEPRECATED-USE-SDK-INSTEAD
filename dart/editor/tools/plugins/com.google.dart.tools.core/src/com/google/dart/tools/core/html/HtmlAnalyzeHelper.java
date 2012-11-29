/*
 * Copyright 2012 Dart project authors.
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

package com.google.dart.tools.core.html;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.analysis.AnalysisServer;
import com.google.dart.tools.core.internal.model.PackageLibraryManagerProvider;

import org.eclipse.core.resources.IFile;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class to analyze Dart code inside of HTML files.
 */
public class HtmlAnalyzeHelper {
  private static final Map<File, File> dartToHtmlMap = Maps.newConcurrentMap();

  /**
   * This method should be called from builder to notify that HTML file was changed and should be
   * analyzed. Note that this is long operation, don't call it from UI thread.
   */
  public static void analyze(IFile htmlResource) {
    File htmlFile = htmlResource.getLocation().toFile();
    try {
      analyzeEx(htmlFile);
    } catch (Throwable e) {
      DartCore.logError(e);
    }
  }

  /**
   * @return the HTML {@link File} from which given Dart {@link File} was generated, may be same
   *         {@link File} if it was not generated from HTML.
   */
  public static File getSourceHtmlFile(File dartFile) {
    File htmlFile = dartToHtmlMap.get(dartFile);
    if (htmlFile != null) {
      return htmlFile;
    }
    return dartFile;

  }

  /**
   * Implementation of {@link #analyze(IFile)} which can throw exceptions.
   */
  private static void analyzeEx(File htmlFile) throws Exception {
    String content = Files.toString(htmlFile, Charsets.UTF_8);
    // extract <script type="application/dart"> scripts
    int searchStart = 0;
    Pattern pattern = Pattern.compile("\\<\\s*script\\s+type=['\"]application/dart['\"]\\s*\\>\\s*(.*)");
    Matcher matcher = pattern.matcher(content);
    while (true) {
      // find next script
      if (!matcher.find(searchStart)) {
        break;
      }
      // prepare script start/end
      int scriptStart = matcher.start(1);
      int scriptEnd = content.indexOf("</script>", scriptStart);
      if (scriptEnd == -1) {
        break;
      }
      searchStart = scriptEnd;
      // extract script
      String script = content.substring(scriptStart, scriptEnd);
      // prepare prefix to place script at the same line/offset as it was in HTML
      String scriptPrefix;
      {
        String contentPrefix = content.substring(0, scriptStart);
        scriptPrefix = CharMatcher.anyOf("\r\n").negate().replaceFrom(contentPrefix, ' ');
      }
      // create temporary File with Dart code
      File scriptFile = File.createTempFile("dartInHtml", ".dart");
      Files.write(scriptPrefix + script, scriptFile, Charsets.UTF_8);
      dartToHtmlMap.put(scriptFile, htmlFile);
      // ask AnalysisServer to analyze Dart file
      {
        AnalysisServer server = PackageLibraryManagerProvider.getDefaultAnalysisServer();
        server.getSavedContext().resolve(scriptFile, 30 * 1000);
        // clean up
        scriptFile.delete();
        // in theory, we should remove mapping, but practically it may be requested later
//        dartToHtmlMap.remove(scriptFile);
      }
    }
  }
}
