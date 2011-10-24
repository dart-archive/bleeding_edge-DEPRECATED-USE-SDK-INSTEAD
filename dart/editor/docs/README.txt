Notes about Dart Editor workspace setup, development, build, and deploy.

====================================
  Installation
====================================

* Install Eclipse 3.7
    http://eclipse.org/downloads/

* Install SVN support into Eclipse 3.7
    Subclipse SVN tools 
      from http://subclipse.tigris.org/update_1.6.x

* Install the depot_tools
    http://dev.chromium.org/developers/how-tos/depottools
    (username is full email address)

====================================
  Get the Source
====================================

* See http://code.google.com/p/dart/wiki/GettingTheSource to check out the Dart source.
   
====================================
  Workspace Configuration
====================================

Macintosh users: Note that on the Macintosh version of Eclipse, "Preferences"
is under the "Eclipse" menu, not under "Window".

------------- Dependent Plugins -----------

Install the Chrome developer tools...
   Update Site: dart/third_party/chromesdk/0.3.0
   You just need the ChromeDevTools SDK feature.

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
  (including the "docs" project containing this README.txt file)

Import the project in <DART_TRUNK>/third_party/closure_compiler_src

====================================
  Building Closure
====================================

We require the closure compiler to build dartc. To build it, right click on closure-compiler/build.xml
and choose Run As > Ant Build (the default option, not the Ant Build... option). This will create
the closure-compiler/build/compiler.jar library, which will be picked up by the 
com.google.dart.compiler.js project.

Alternatively, run ant from the third_party/closure_compiler directory.

Refresh the closure-compiler and com.google.dart.compiler.js projects for Eclipse to see the new
files.

====================================
  Launching the Dart Editor
====================================

Once your projects have been imported, go to the Package Explorer and open the 
dart_feature.product file in the com.google.dart.tools.deploy project. Under the
Testing section, click on the 'Launch an Eclipse Application' link. Another instance of
Eclipse should launch, running the Dart Editor!

====================================
  Building the Dart Editor
====================================

See the dart/editor/build/README.txt file for build instructions.
