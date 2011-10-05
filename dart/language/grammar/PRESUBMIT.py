# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import os
import subprocess

LICENSE_RE = ("Copyright \(c\) 2011, the Dart project authors.  " +
              "Please see the AUTHORS file")

def AllTextFiles(x):
  return x.IsTextFile()


def NotXmlFiles(x):
  name = x.LocalPath()
  return not (name.endswith(".xml") or name.endswith(".xsl"))


def CannedChecks(results, input_api, output_api):
  can = input_api.canned_checks
  results.extend(can.CheckChangeHasNoStrayWhitespace(input_api, output_api,
                                                     AllTextFiles))
  results.extend(can.CheckChangeHasNoCrAndHasOnlyOneEol(input_api, output_api,
                                                        AllTextFiles))
  results.extend(can.CheckChangeHasNoTabs(input_api, output_api, AllTextFiles))
  results.extend(can.CheckLongLines(input_api, output_api, 80, NotXmlFiles))
  results.extend(can.CheckLicense(input_api, output_api, LICENSE_RE,
                                  AllTextFiles))


def RunTests(results, input_api, output_api):
  if os.system("make -q || make") != 0:
    results.append(output_api.PresubmitError("Make failed"))
    return

  child = subprocess.Popen(["./doit.sh"], stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT)
  (stdout, stderr) = child.communicate()
  if child.returncode != 0:
    for line in stdout.split("\n"):
      if line.find(": warning: ") == -1:
        print line
    results.append(output_api.PresubmitError("Grammar tests failed"))


def CheckChange(input_api, output_api, committing):
  results = []
  RunTests(results, input_api, output_api)
  CannedChecks(results, input_api, output_api)
  return results


def CheckChangeOnUpload(input_api, output_api):
  return CheckChange(input_api, output_api, False)


def CheckChangeOnCommit(input_api, output_api):
  return CheckChange(input_api, output_api, True)
