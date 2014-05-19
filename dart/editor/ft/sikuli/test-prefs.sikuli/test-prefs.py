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
  _selectPrefPane(p("EditorPaneTitle.png"))

def selectFontsPrefPane():
  "Select the Fonts preferences pane by name."
  _selectPrefPane(p("FontsPaneTitle.png"))

def selectHintsPrefPane():
  "Select the Hints preferences pane by name."
  _selectPrefPane(p("HintsPaneTitle.png"))

def selectKeyBindingsPane():
  "Select the Key Bindings preferences pane by name."
  _selectPrefPane(p("KeyBindingsPaneTitle.png"))

def selectRunandDebugPrefPane():
  "Select the Run and Debug preferences pane by name."
  _selectPrefPane(p("RunandDebugPaneTitle.png"))

def selectUpdatePrefPane():
  "Select the Update preferences pane by name."
  _selectPrefPane(p("UpdatePaneTitle.png"))

def selectVisualThemePrefPane():
  "Select the Visual Theme preferences pane by name."
  _selectPrefPane(p("VisualThemePaneTitle.png"))

def testPrefPaneNav():
  "Test arrow-key navigaiton of preference panes."
  util.open_preferences()
  t=_prefTitleRegion()
  firstPrefPane(t, p("EditorPaneTitle.png"))
  nextPrefPane(t, p("FontsPaneTitle.png"))
  nextPrefPane(t, p("HintsPaneTitle.png"))
  nextPrefPane(t, p("KeyBindingsPaneTitle.png"))
  nextPrefPane(t, p("RunandDebugPaneTitle.png"))
  nextPrefPane(t, p("UpdatePaneTitle.png"))
  nextPrefPane(t, p("VisualThemePaneTitle.png"))
  wait(2)
  util.dismiss_dialog()

def testPrefPaneSelection():
  "Test preference pane selection by name."
  util.open_preferences()
  wait(5) # In case we start with Visual Theme, which we do
  selectEditorPrefPane() # Still in size large panes
  util.dismiss_dialog()
  util.open_preferences() # Back to size small panes
  selectFontsPrefPane()
  selectHintsPrefPane()
  selectKeyBindingsPane()
  selectRunandDebugPrefPane() # Medium size
  selectUpdatePrefPane()
  selectVisualThemePrefPane() # Large size
  util.dismiss_dialog()
  util.open_preferences()
  selectEditorPrefPane() # Start with small size next time
  util.dismiss_dialog()

def _prefTitleRegion():
  return find(p("PreferencesTitle.png")).below(40).right(1).left(350)

def _selectPrefPane(title):
  t = _prefTitleRegion()
  i = t.left(1).left(150)
  i.click()
  t.left(1).left(50).click()
  t.left(1).right(20).click()
  wait(1)
  util.select_all()
  wait(1)
  util.delete_text()
  wait(1)
  type(title[0])
  wait(1)
  type(title[1]) # Not needed
  wait(1)
  type(Key.DOWN)
  wait(1)
  type(Key.DOWN)
  wait(1)
  t.nearby(100).wait(p(title))

util.kill_editor() # Kill any currently running editor
util.init_dart_editor() # Clear the workspace and start the editor
#util.start_dart_editor()
testPrefPaneNav() # Test
testPrefPaneSelection() # Test
util.quit_app() # Exit editor
