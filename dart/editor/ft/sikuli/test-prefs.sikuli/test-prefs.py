import os

# Add the parent path to the sys path so the script can be run from the dart repo
base_path = os.path.dirname(getBundlePath())
script_path = os.path.normpath(base_path + "/../util.sikuli")
if not script_path in sys.path: sys.path.append(script_path)

import util
reload(util) # Needed during development to avoid restarting Sikuli IDE
from util import p

def firstPrefPane(region, title):
  "Click in the text pane to the left of the region and wait for title to show up."
  r=region.left(100).right(100)
  r.click()
  type(Key.TAB)
  region.wait(p(title))

def nextPrefPane(region, title):
  "Select the next item down in the list and wait for title to show up in the region."
  type(Key.DOWN)
  wait(0.5)
  region.wait(p(title), 10)

def selectEditorPrefPane():
  "Select the Editor preferences pane by name."
  _selectPrefPane("EditorPaneTitle.png")

def selectFontsPrefPane():
  "Select the Fonts preferences pane by name."
  _selectPrefPane("FontsPaneTitle.png")

def selectHintsPrefPane():
  "Select the Hints preferences pane by name."
  _selectPrefPane("HintsPaneTitle.png")

def selectKeyBindingsPane():
  "Select the Key Bindings preferences pane by name."
  _selectPrefPane("KeyBindingsPaneTitle.png")

def selectRunandDebugPrefPane():
  "Select the Run and Debug preferences pane by name."
  _selectPrefPane("RunandDebugPaneTitle.png")

def selectUpdatePrefPane():
  "Select the Update preferences pane by name."
  _selectPrefPane("UpdatePaneTitle.png")

def selectVisualThemePrefPane():
  "Select the Visual Theme preferences pane by name."
  _selectPrefPane("VisualThemePaneTitle.png")

def testPrefPaneNav():
  "Test arrow-key navigaiton of preference panes."
  util.open_preferences()
  t=_prefTitleRegion()
  firstPrefPane(t, "EditorPaneTitle.png")
  nextPrefPane(t, "FontsPaneTitle.png")
  nextPrefPane(t, "HintsPaneTitle.png")
  nextPrefPane(t, "KeyBindingsPaneTitle.png")
  nextPrefPane(t, "RunandDebugPaneTitle.png")
  nextPrefPane(t, "UpdatePaneTitle.png")
  nextPrefPane(t, "VisualThemePaneTitle.png")
  type(Key.ESC)

def testPrefPaneSelection():
  "Test preference pane selection by name."
  util.open_preferences()
  wait(5) # In case we start with Visual Theme, which we do
  selectEditorPrefPane() # Still in size large panes
  type(Key.ESC)
  util.open_preferences() # Back to size small panes
  selectFontsPrefPane()
  selectHintsPrefPane()
  selectKeyBindingsPane()
  selectRunandDebugPrefPane() # Medium size
  selectUpdatePrefPane()
  selectVisualThemePrefPane() # Large size
  selectEditorPrefPane() # Start with small size next time
  type(Key.ESC)

def _prefTitleRegion():
  return find(p("PreferencesTitle.png")).below(40).right(1).left(350)

def _selectPrefPane(title):
  t = _prefTitleRegion()
  i = t.left(1).right(50)
  i.click()
  util.select_all()
  util.delete_text()
  type(title[0])
  type(title[1]) # Not needed
  wait(0.5)
  type(Key.DOWN)
  wait(0.5)
  type(Key.DOWN)
  wait(0.5)
  t.wait(p(title))


util.kill_editor() # Kill any currently running editor
util.init_dart_editor() # Clear the workspace and start the editor
testPrefPaneNav() # Test
testPrefPaneSelection() # Test
util.quit_app() # Exit editor
