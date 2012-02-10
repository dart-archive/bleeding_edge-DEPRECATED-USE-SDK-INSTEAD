Notes about Dart Editor workspace setup, development, build, and deploy.

====================================
  Installation
====================================

* Install Eclipse 3.7  "Eclipse for RCP and RAP Developers" ("Classic" should work as well)
    http://eclipse.org/downloads/

* Install SVN support into Eclipse 3.7
    Subclipse SVN tools 
      from http://subclipse.tigris.org/update_1.6.x

* Install the depot_tools
    http://dev.chromium.org/developers/how-tos/depottools
    (username is full email address)

* Install SWTBot (optional -- needed for UI tests)
    Install from the Eclipse SWTBot download page
      http://www.eclipse.org/swtbot/downloads.php
      http://download.eclipse.org/technology/swtbot/helios/dev-build/update-site
    the following features
      SWTBot Eclipse Features
      SWTBot IDE Support
      SWTBot JUnit 4.x Headless Execution
      SWTBot SWT Features

====================================
  Get the Source
====================================

* See http://code.google.com/p/dart/wiki/GettingTheSource to check out the Dart source.
   
====================================
  Workspace Configuration
====================================

Macintosh users: Note that on the Macintosh version of Eclipse, "Preferences"
is under the "Eclipse" menu, not under "Window".

------------- Linked Resources ------------

Create a "Path Variable" in the "General > Workspace > Linked Resources" preference page
    called "DART_TRUNK" that points to the SVN dart/trunk/dart directory
    that you checked out as specified in the instructions above.

-------------Classpath Variables -----------

Create a "Classpath Variable" in "Java > Build Path > Classpath Variables" preference page
    called "DART_TRUNK" that points to the same directory as the "DART_TRUNK" path variable above

------------- Text Editors ----------------

Window->Preferences->General->Editors->Text Editors
Make sure that "Displayed Tab Width" is set to 2
Enable "Insert Spaces for Tabs"
Enable "Show Print Margin" and set "Print Margin Column" to 100

------------- XML Files -------------------

Window->Preferences->Web and XML->XML Files->Source
(or Window->Preferences->XML Files->Editor, if you can't find it there)
Set "Line Width" 100
Enable "Split Multiple Attributes Each of a New Line"
Enable "Indent Using Spaces" with an Indentation Size of 4

------------- Ant Build Files -------------

Window->Preferences->Ant->Editor->Formatter
Set "Tab Size" to 4
Disable "Use Tabs Instead of Spaces"
Set "Maximum Line Width" to 80
Enable "Wrap Long Element Tags"

---------------- Spelling -----------------

Window->Preferences->General->Editors->Text Editors->Spelling
Enable spell checking
Use <DART_TRUNK>/editor/docs/english.dictionary".

----------- Code Templates ----------------

Window->Preferences->Java->Code Style->Code Templates

Comments->Files template should look like this:

/*
 * Copyright (c) ${year}, the Dart project authors.
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

Comments->Types

Remove the @author tag

---------- Save Actions -------------------

Window->Preferences->Java->Editor->Save Actions

Enable "Perform the Selected Actions on Save"
Enable "Format Source Code" (format all lines)
Enable "Organize Imports"
Enable "Additional Actions"
Click "Configure", and make sure that all actions are disabled except:
- "Remove trailing white spaces on all lines"
- "Sort members excluding fields, enum constants, and initializers"
- "Convert control statement bodies to block"
- "Add missing '@Override' annotations"
- "Add missing '@Override' annotations to implementations of interface methods"
- "Add missing '@Deprecated' annotations"
- "Remove unnecessary casts"

---------- Code style/formatting ----------

Window->Preferences->Java->Code Style->Formatter->Import...
  <DART_TRUNK>/editor/docs/dart-format.xml

----------- Import organization -----------

Window->Preferences->Java->Code Style->Organize Imports->Import...
  <DART_TRUNK>/editor/docs/dart.importorder

------------ Member sort order ------------

Window->Preferences->Java->Appearance->Members Sort Order
There is no import here, so make your settings match:
  settings/code-style/dart-sort-order.png

First, members should be sorted by category.
1) Types
2) Static Fields
3) Static Initializers
4) Static Methods
5) Fields
6) Initializers
7) Constructors
8) Methods

Second, members in the same category should be sorted by visibility.
1) Public
2) Protected
3) Default
4) Private

Third, within a category/visibility combination, members should be sorted
alphabetically.

------------ Java > Compiler > Errors/Warnings ------------

Set to ignore:

- Potential programming problems > Serializable class w/o serialVersionUID
- Unnecessary Code > Parameter is never read

====================================
  Importing the Eclipse projects
====================================

File -> Import -> General -> Existing Projects into Workspace

Import the existing projects in <DART_TRUNK>/editor
  including the "docs" project containing this README.txt file

====================================
  Building Dart Libraries
====================================

For development, we require that the Eclipse installation directory have a "libraries" directory
containing the bundled dart:<name> libraries (e.g. dart:core). To build this directory...

1) Setup and run the RCP build locally using the directions at dart/editor/build/README.txt.
2) In Eclipse, right click on editor/build/build_rcp.xml
3) Select "Run As > Ant Build..."
4) Click the "Targets" tab and make sure only the "setupDevWorkspace" target is checked
5) Click the "JRE" tab and select "Run in the same JRE as the workspace"
6) Click "Run"

Alternately, you can run the Ant script outside Eclipse
by defining the "eclipse.home" property to point to your Eclipse installation

Finally, if you have any of the com.google.dart.library.* projects in your
workspace (you may have these projects if you setup the workspace before
libraries were built), remove or close them.

====================================
  Launching the Dart Editor
====================================

Once your projects have been imported, go to the Package Explorer and open the 
dart_feature.product file in the com.google.dart.tools.deploy project. Under the
Testing section, click on the 'Launch an Eclipse Application' link. Another instance of
Eclipse should launch, running the Dart Editor!

====================================
  Dart Editor Options
====================================

Dart Editor has some internal options for adjusting what information is logged
and what technology is used when compiling and launching Dart source.
See dart/editor/tools/plugins/com.google.dart.tools.core/.options

====================================
  Building the Dart Editor
====================================

In the dart/editor/tools/features/com.google.dart.tools.deploy.feature_releng/build-settings
directory, copy the user.properties file to <username>.properties, where <username> is your
login name. Adjust the two properties in that file to point to:

  -the Dart Editor source directory (dart/editor)
  -a build output directory

Run the build_rcp.xml ant script in the com.google.dart.tools.deploy.feature_releng project
(ant -f build_rcp.xml). It will create Windows, Linux, and Mac builds in the 'out' directory
of the build directory specified above.

====================================
Running SWTBot UI tests
====================================

Install SWTBot (see optional installation step above)
Import com.google.dart.tools.ui.swtbot_test (if not already imported)

    <Bug platform="Mac">
      Bug Description: (Mac only): 
        There is a bug when running SWTBot in Eclipse 3.7.0 on Mac 10.6.8 (and others?)
        Key events posted with Display.post() do not honor Shift key state
        https://bugs.eclipse.org/bugs/show_bug.cgi?id=363309
      Workaround:
        If the SWTBot tests cannot successfully drive the open file dialog
          (fails to auto-type uppercase characters),
        then ...
          Open the "Plug-ins" view
          Select "org.eclipse.swt.cocoa.macosx.x86_64"
          Import As > Source Project
          Edit org.eclipse.swt.internal.cocoa.OS.java
            Replace
              public static final int kCGSessionEventTap = 1;
            with
              public static final int kCGHIDEventTap = 0;
          Edit org.eclipse.swt.widgets.Display
            Replace
              OS.CGEventPost(OS.kCGSessionEventTap, eventRef);
            with
              OS.CGEventPost(OS.kCGHIDEventTap, eventRef);
     </Bug>

Right click on src/com.google.dart.tools.ui.swtbot.DartEditorUiTest
Select Run > Run Configurations...
Select SWTBot Test > DartEditorUiTest
Click on "Main" tab
In "Run a product:" select "com.google.dart.tools.deploy.product"
Click on "Arguments" tab
Change "VM Arguments" to "-Xms128m -Xmx1024m"
Click on "Plugins" tab
Change "Launch with" to "Plugins selected below only"
Uncheck all "Target Platform" plugins
Check the workspace com...swtbot_test plugin only
Click "Add required plugins"
Click Run
