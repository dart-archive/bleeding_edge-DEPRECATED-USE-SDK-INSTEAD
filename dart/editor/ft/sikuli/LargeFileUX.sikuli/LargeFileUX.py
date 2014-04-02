import time
import logging
import os

# Add the parent path to the sys path so the script can be run from the dart repo
scriptPath = os.path.dirname(getBundlePath())
scriptPath = os.path.normpath(scriptPath + "/../util.sikuli")
if not scriptPath in sys.path: sys.path.append(scriptPath)

import util
reload(util) # Needed during development to avoid restarting Sikuli IDE

SIGNAL = "ENDEDIT" # We know the editor has finished editing when this string appears in the source
max_edit_time = 60 # It takes about 22 seconds to add 40 assignments on my Mac
logger = logging.getLogger('sikuli')

def analysis_time(event):
  "Measure the time spent in analysis."
  start_time = time.clock()
  util.wait_for_analysis() # Has a built-in 2s wait
  stop_time = time.clock()
  delta_time = stop_time - start_time
  if delta_time > 3.0:
    logger.info(" Analysis after " + event + " took " + str(delta_time) + " seconds")
  return delta_time

def add_some_code():
  "Type a bunch of random but legal statements."
  util.type_line("int x1 = 1; int a1 = 1;")
  util.type_line("int x2 = 1; int a2 = 1;")
  util.type_line("int x3 = 1; int a3 = 1;")
  util.type_line("int x4 = 1; int a4 = 1;")
  util.type_line("int x5 = 1; int a5 = 1;")
  util.type_line("int x6 = 1; int a6 = 1;")
  util.type_line("int x7 = 1; int a7 = 1;")
  util.type_line("int x8 = 1; int a8 = 1;")
  util.type_line("int x9 = 1; int a9 = 1;")
  util.type_line("int x0 = 1; int a0 = 1;")
  util.type_line("int y1 = 1; int b1 = 1;")
  util.type_line("int y2 = 1; int b2 = 1;")
  util.type_line("int y3 = 1; int b3 = 1;")
  util.type_line("int y4 = 1; int b4 = 1;")
  util.type_line("int y5 = 1; int b5 = 1;")
  util.type_line("int y6 = 1; int b6 = 1;")
  util.type_line("int y7 = 1; int b7 = 1;")
  util.type_line("int y8 = 1; int b8 = 1;")
  util.type_line("int y9 = 1; int b9 = 1;")
  util.type_line("int y0 = 1; int b0 = 1;")
  util.type_line("// " + SIGNAL)

def init_dart_editor():
  "Delete the workspace, start the editor, and open the big file."
  util.init_dart_editor()
  util.disable_code_folding()
  open_big_project()
  open_big_file()

def init_logger():
  "Initialize logging, setting up console logging. Prevent duplicate messages."
  if logger.getEffectiveLevel() <> logging.DEBUG:
    logger.setLevel(logging.DEBUG)
    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    logger.removeHandler(ch)
    logger.addHandler(ch)

def open_big_file():
  "Assuming the project is open, expand the tree and open the file."
  util.top_project_name_region().doubleClick()
  for i in range(4): 
    type(Key.DOWN)
  type(Key.ENTER)
  type(Key.DOWN)
  type(Key.DOWN)
  type(Key.ENTER)
  wait(2) # Need to allow time for the Refactor menu to be added

def open_big_project():
  "Open a big project and wait for its analysis to finish."
  util.files_context_menu_select("Existing")
  util.select_dart_folder_item("big") # May need to install the project
  util.wait_for_analysis()

# T E S T   S C R I P T
# The approach is to open a large file in Dart Editor and make changes to it.
# If the changes appear on-screen in a reasonable amount of time then
# the test passes. If anything else happens the test fails.

init_logger()
#util.start_dart_editor()
init_dart_editor()
util.activate_editor()
util.goto_line(1)
for i in range(6): # Not the same as goto_line(6)
  type(Key.DOWN)
type(Key.ENTER)
#analysis_time("ENTER")
result = util.timeout(analysis_time,args=["ENTER"],kwargs={},timeout_duration=10.0,default="fail")
if result == "fail":
  logger.info("Analysis took too long after initial ENTER")
  logger.info("FAIL")
  exit(0)
edit_begin = time.clock()
add_some_code()
edit_done = exists(SIGNAL, max_edit_time)
edit_end = time.clock()
if edit_done:
  logger.info(" Editing took " + str(edit_end - edit_begin) + " seconds")
  wait(0.5)
  analysis_time("adding some code")
  util.menu_bar_select("File", "Revert")
  analysis_time("revert the changes")
  util.quit_app()
else:
  logger.info("Editing did not complete within " + str(max_edit_time) + " seconds")
  logger.info('FAIL')
  util.kill_editor()
