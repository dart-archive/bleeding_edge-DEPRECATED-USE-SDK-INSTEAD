from sikuli import *

import os
import subprocess

# Utility functions that make working with Dart Editor easier.

def activate_editor():
  "Activate the editor, giving it focus."
  #menu_bar_select("Navigate", "Activate Editor")
  _key_cmd(Key.F12)

def close_tab():
  "Close the tab that has focus."
  _key_cmd('w')

def close_all_tabs():
  "Close all Tabs."
  _shift_key_cmd('w')

def copy_text():
  "Copy selected text to the clipboard."
  _key_cmd('c')

def cut_text():
  "Cut the selected text after copying to the clipboard."
  _key_cmd('x')

def delete_text():
  "Delete the selected text, or the next character, without copying to the clipboard."
  type(Key.DELETE)

def disable_code_folding():
  "Uncheck the box for the code folding preference."
  open_preferences()
  f = find("fold-pref-grey.png")
  x = f.right(1).left(150).exists("checked-mark.png", 0)
  if x:
    x.click() # uncheck the check box
    enter()   # shift focus
    enter()   # accept changes
  else:
    escape()  # dismiss

def escape():
  "Type the escape key."
  type(Key.ESC)

def enter():
  "Type the enter key."
  type(Key.ENTER)

def extend_selection_with_next_element():
  "Extend the selection forward to include the text of the element following the current selection."
  type(Key.RIGHT, KeyModifier.SHIFT + KeyModifier.CTRL)

def extend_selection_with_previous_element():
  "Extend the selection backward to include the text of the element preceeding the current selection."
  type(Key.LEFT, KeyModifier.SHIFT + KeyModifier.CTRL)

def files_context_menu_select(selection, location=0):
  "Open the context menu and choose the selection. If no location is given, use the empty part of the Files view."
  if (location == 0):
    s = Region(SCREEN)
    s.setW(300)
    r = s.find("InstalledPackagesLabel.png")
    location = r.below(50)
  rightClick(location)
  menuRegion = _merge(location, location.right(200), location.below(300))
  menuRegion.click(selection)

def files_tab_region():
  "Return the region containing the 'Files' view tab title"
  s = Region(SCREEN)
  s.setW(300)
  s.setH(300)
  r = s.find("Files")
  return r

def goto_line(num):
  "Set the cursor at the beginning of the line numbered num."
  _key_cmd('l')
  wait(0.5)
  type(str(num))
  wait(0.5)
  type(Key.ENTER)

def goto_line_end():
  "Move the cursor to the end of the current line."
  if is_OSX():
    _key_cmd(Key.RIGHT)
  else:
    type(Key.END)

def init_dart_editor():
  "Launch Dart Editor by running the script that initializes its state then starts the app."
  r = _check_bounds()
  s = Region(SCREEN)
  s.setW(120)
  s.setH(120)
  s.doubleClick() # Assumes StartDartEditor icon is here, top-left of Desktop
  _init_editor_window(r)

def is_linux():
  "Return true if running on Linux."
  return False # TODO

def is_OSX():
  "Return true if running on Mac."
  return True # TODO

def kill_chromium():
  "Kill chromium if running."
  subprocess.call(["killall", "Chromium"])
  wait(0.5)

def kill_editor():
  "Kill the editor if running."
  subprocess.call(["killall", "DartEditor"])
  wait(0.5)

def menu_bar_select(menu, selection):
  "From the menu bar, show the menu and choose the selection."
  s = Region(SCREEN)
  s.setH(30)
  s.setW(550)
  r = s.find(menu)
  r.click()
  wait(0.5)
  h = r.left(1).below(300).right(250)
  h.click(selection)

def open_preferences():
  "Open the Preferences dialog."
  _key_cmd(',')
  wait(0.5)

def paste_text():
  "Paste text from the clipboard."
  _key_cmd('v')

def quit_app():
  "Make the top-most application quit."
  if is_OSX():
    _key_cmd('q')
  else:
    type(Key.F4, KeyModifier.ALT)

def revert_file():
  "Revert recent edits."
  menu_bar_select("File", "Revert")

def run_app():
  "Run the selected app without clicking the Run button."
  _key_cmd('r')

def save_file():
  "Save the current editor file."
  _key_cmd('s')

def select_all():
  "Select all text in the editor."
  _key_cmd('a')
  wait(0.5)

def select_dart_folder_item(name):
  "Select the given name from the file picker"
  s = Region(SCREEN);
  s.setH(300)
  title = s.find("DartEditorName.png")
  picker = title.below(50).nearby(325)
  picker.click("dartFolderIcon.png")
  picker.click("dartFolderIcon.png")
  picker.click(name)
  picker.click(name)
  type(Key.ENTER)

def start_dart_editor():
  "Start the editor. If the network connections prompt appears, dismiss it, otherwise click unobtrusively."
  r = _check_bounds()
  #switchApp("DartEditor")
  dartEditor = App(os.path.abspath('Desktop/dart/DartEditor.app'))
  dartEditor.open()
  _init_editor_window(r)
  return dartEditor

def timeout(func, args=(), kwargs={}, timeout_duration=1.0, default=None):
    '''This function will spwan a thread and run the given function using the args, kwargs and 
    return the given default value if the timeout_duration is exceeded 
    ''' 
    import threading
    class InterruptableThread(threading.Thread):
        def __init__(self):
            threading.Thread.__init__(self)
            self.result = default
        def run(self):
            try:
                self.result = func(*args, **kwargs)
            except:
                self.result = default
    it = InterruptableThread()
    it.start()
    it.join(timeout_duration)
    if it.isAlive():
        return default
    else:
        return it.result

def top_project_name_region():
  "Return a region containing the name of the top-most project."
  return files_tab_region().left(20).below(40).left(1).right(200)

def type_line(string):
  "Type a line of text, and add a newline."
  type(string)
  type(Key.ENTER)

def undo():
  "Undo the previous edit."
  _key_cmd('z')

def wait_for_analysis():
  "Wait for analysis to finish"
  if exists("Analyzing.png",2):
    waitVanish("Analyzing.png", FOREVER)

def _check_bounds():
  "Check the screen size is correct and raise an error if not."
  r=getBounds()
  if r.x != 0 or r.y != 0:
    raise RuntimeError, "Bad screen size"
  if r.width != 1440 or r.height != 900:
    raise RuntimeError, "Bad screen size"
  return r

def _init_editor_window(r):
  "If the network connections prompt appears, dismiss it, otherwise click unobtrusively."
  wait("DartEditorName.png",20)
  click(Location(r.width/2+30, r.height/3+55))

def _key_cmd(key):
  "Send meta-key to the editor."
  type(key, KeyModifier.META)

def _merge(region, rtRegion, blRegion):
  "Create a region that represents a context menu region when the given region is clicked."
  x=region.getCenter().getX()
  y=region.getCenter().getY()
  w=rtRegion.getW() + region.getW()
  h=blRegion.getH() + region.getH()
  r=Region(x, y, w, h)
  return r

def _shift_key_cmd(key):
  "Send shift-meta-key to the editor."
  type(key, KeyModifier.META + KeyModifier.SHIFT)
