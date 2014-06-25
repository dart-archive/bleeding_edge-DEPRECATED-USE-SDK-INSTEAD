Notes about Dart Editor workspace setup, development, build, and deploy.

====================================
  Installation
====================================

Recent versions of Mac OSX do not have Java. To get Eclipse 3.8.2 to work with Java7
you may want to edit the Java Info.plist to define more capabilities.
See https://bugs.eclipse.org/bugs/show_bug.cgi?id=411361

* Install Eclipse 3.8  "Eclipse for RCP and RAP Developers" ("Classic" should work as well)
    http://eclipse.org/downloads/
  or, a direct link:
    http://archive.eclipse.org/eclipse/downloads/drops/R-3.8.2-201301310800/

* Select "Help > Install New Software" to install
    Eclipse Web Developer Tools
    Eclipse XML Editors and Tools (optional)
    SWT Designer (optional)

* Install SVN support into Eclipse
    Subclipse SVN tools 
      from http://subclipse.tigris.org/update_1.6.x

* Install the depot_tools
    http://dev.chromium.org/developers/how-tos/depottools
    (username is full email address)

* Install SWTBot (deprecated -- used for deprecated UI tests)
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

Create a "Path Variable" in the "Window > Preferences > General > Workspace > Linked Resources" preference page
    called "DART_TRUNK" that points to the SVN dart/trunk/dart directory
    that you checked out as specified in the instructions above.

-------------Classpath Variables -----------

Create a "Classpath Variable" in "Window > Preferences > Java > Build Path > Classpath Variables" preference page
    called "DART_TRUNK" that points to the same directory as the "DART_TRUNK" path variable above

------------- Text Editors ----------------

Window->Preferences->General->Editors->Text Editors
Make sure that "Displayed Tab Width" is set to 2
Enable "Insert Spaces for Tabs"
Enable "Show Print Margin" and set "Print Margin Column" to 100

------------- XML Files -------------------

Window->Preferences->Web and XML->XML Files->Source
(or Window->Preferences->XML->XML Files->Editor, if you can't find it there)
Set "Line Width" 100
Enable "Split Multiple Attributes Each on a New Line"
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
Set "User defined dictionary" to
"<DART_TRUNK>/editor/docs/english.dictionary".

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

Remove the @author tag so that "Pattern" looks like:

/**
 * ${tags}
 */

---------- Save Actions -------------------

Window->Preferences->Java->Editor->Save Actions

Enable "Perform the Selected Actions on Save"
Enable "Format Source Code" (format all lines)
Enable "Organize Imports"
Enable "Additional Actions"
Click "Configure", and make sure that all actions are disabled except:
- "Remove trailing white spaces on all lines"
- "Sort members excluding fields, enum constants, and initializers": via
  Code Organizing->Sort members->Ignore fields and enum constants
- "Convert control statement bodies to block": via
  Code Style->Control statements->Use blocks in if/while/for/do statements->Always
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
There is no import here, so make your settings match this screen shot:
  editor/docs/dart-sort-order.png

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

------------ Java > Compiler > Errors/Warnings ------------

Set to ignore:

- Potential programming problems > Serializable class w/o serialVersionUID
- Unnecessary Code > Value of parameter is not used

------------ Plug-in Development > API Baselines

Set to ignore:

- Missing API baseline

====================================
  Importing the Eclipse projects
====================================

File -> Import -> General -> Existing Projects into Workspace

Import the existing projects in <DART_TRUNK>/editor
including the "docs" project containing this README.txt file: choose
"Select root directory" and enter "<DART_TRUNK>/editor". This will import
*many* projects (53 as of this writing).

====================================
  Installing Dart SDK and Dartium
====================================

For development, we require that the Eclipse installation directory (on
Linux, the directory where you unpacked the prebuilt Eclipse)
have a "dart-sdk" directory
containing the bundled dart:<name> libraries (e.g. dart:core).

1) In the dart/editor/tools/features/com.google.dart.tools.deploy.feature_releng/build-settings
directory, copy the user.properties file to <username>.properties, where <username> is your
login name. Adjust (the defaults should work) the two properties in that file to point to:

  -the Dart Editor source directory (dart/editor)
  -a build output directory

2) In Eclipse, right click on build_rcp.xml in com.google.dart.tools.deploy.feature_releng
3) Select "Run As > Ant Build..."
4) Click the "Targets" tab and make sure only the "setupDevWorkspace" target is checked
5) Click the "JRE" tab and select "Run in the same JRE as the workspace"
6) Click "Run"

(Alternately, you can run the Ant script outside Eclipse
by defining the "eclipse.home" property to point to your Eclipse installation)

Next, install Dartium (Chromium with an embedded Dart VM) into the
Eclipse installation directory: http://www.dartlang.org/tools/dartium/

Linux:
  # URL for 32 bit version is different!
  wget http://storage.googleapis.com/dart-archive/channels/stable/release/latest/dartium/dartium-linux-x64-release.zip
  unzip dartium-linux-x64-release.zip
  # The unzip produces a directory named "dartium-<version>".
  # Rename that directory to "chromium" in the Eclipse install dir:
  mv dartium-<version> <eclipse-install-dir>/chromium

Mac:
  <eclipse-install-dir>/chromium/Chromium.app

Windows
  <eclipse-install-dir>/chromium/chromium.exe

====================================
  Launching the Dart Editor
====================================

Once your projects have been imported, pull down the "Run" menu and select "Run Configurations..."
Click on "Eclipse Application" on the left hand side, 
then click the "New Launch Configuration" button above that list.
On the "Main" page, change "Run a product" to "com.google.dart.tools.deploy.product"
On the "Plug-ins" page ...
    Change "Launch with:" to "plug-ins selected below only".
    Click "Deselect all"
    Select "com.google.dart.tools.ui"
    Click "Add Required Plug-ins".
    Click "Validate plug-ins automatically prior to launching".
Click "Run" and another instance of Eclipse should launch, running the Dart Editor!

====================================
  Dart Editor Options
====================================

Dart Editor has some internal options for adjusting what information is logged
and what technology is used when compiling and launching Dart source.
See dart/editor/tools/plugins/com.google.dart.tools.core/.options

====================================
  Building the Dart Editor
====================================

Setup your <username>.properties file as described above in "Installing Dart SDK and Dartium"

In a similar fashion create a new <username>.<build-os>.properties file in the same directory
(dart/editor/tools/features/com.google.dart.tools.deploy.feature_releng/build-settings)
as the <username>.properties file with content copied from chrome-bot.<build-os>.properties

Run the build_rcp.xml ant script in the
dart/editor/tools/features/com.google.dart.tools.deploy.feature_releng project

    ant -f build_rcp.xml -Dbuild.os=<os>
  where <os> is one of
    linux
    macos
    win32

It will create Windows, Linux, and Mac builds in the 'out' directory
of the build directory specified above.

====================================
  Build/Run Dart Editor Tests
====================================

After building the Dart editor as described above,
run the buildTests.xml ant script in the com.google.dart.tools.tests.feature_releng project

    ant -f buildTests.xml -Dbuild.os=<os>
  where <os> is one of
    linux
    macos
    win32

====================================
  Running SWTBot UI tests (deprecated)
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
Uncheck all plugins
Check the "Workspace" com...swtbot_test plugin
Check the "Target Platform" org.apache.ant plugin 
Click "Add required plugins"
Click on "Environment" tab
Click "New..." to add a new "DART_TRUNK" environment variable that points to your SVN root
Click Run
