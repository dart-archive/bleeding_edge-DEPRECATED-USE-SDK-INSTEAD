#!/usr/bin/python

# Copyright (c) 2011 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""Eclipse Dart Editor buildbot steps

Compiles Dart Editor 
"""

import glob
import optparse
import os
import postprocess
import shutil
import string
import subprocess
import sys

def BuildOptions():
  '''
  setup the argument processing for this program
  '''
  result = optparse.OptionParser()
  result.set_default('dest', 'gs://dart-editor-archive-continuous')
  result.add_option("-m", "--mode",
      help='Build variants (comma-separated).',
      metavar='[all,debug,release]',
      default='debug')
  result.add_option("-v", "--verbose",
      help='Verbose output.',
      default=False, action="store")
  result.add_option("-r", "--revision",
      help='SVN Revision.',
      action="store")
  result.add_option("-n", "--name",
      help='builder name.',
      action="store")
  result.add_option("-o", "--out",
      help='Output Directory.',
      action="store")
  result.add_option("--dest",
      help='Output Directory.',
      action="store")
  return result


def main():
  '''
  main entry point for the build program
  '''
  
  if len(sys.argv) == 0:
    print 'Script pathname not known, giving up.'
    return 1

  scriptdir = os.path.dirname(sys.argv[0])
  eclipsepath = os.path.abspath(os.path.join(scriptdir, '..'))
  thirdpartypath = os.path.abspath(os.path.join(scriptdir, 
                                                '..', '..', 
                                                'third_party'))
  antpath = os.path.join(thirdpartypath, 'apache_ant', 'v1_7_1')
  bzip2libpath = os.path.join(thirdpartypath, 'bzip2')
  buildpath = os.path.join(eclipsepath, 'tools', 'features', 
                           'com.google.dart.tools.deploy.feature_releng')
  buildroot = os.path.join(eclipsepath, 'build_root') 
  os.chdir(buildpath)
  
  parser = BuildOptions()
  (options, args) = parser.parse_args()
  # Determine which targets to build. By default we build the "all" target.
  if len(args) > 0:
    print "only options should be passed to this script"
    parser.print_help()
    return 1

  if str(options.revision) == 'None':
    print "missing revision option"
    parser.print_help()
    return 2
  
  if str(options.name) == 'None':
    print "missing builder name"
    parser.print_help()
    return 2
  
  if str(options.out) == 'None':
    print "missing putput directory"
    parser.print_help()
    return 2

  buildout = os.path.join(buildroot, options.out)

  print ('@@@BUILD_STEP dart-ide dart clients: %s@@@' % options.name)
  printSeparator("running the build to produce the Zipped RCP's")
  status = runAnt('.', 'build_rcp.xml', options.revision, options.name, 
                  buildroot, buildout, eclipsepath)
  propertyFile = os.path.join('/var/tmp/' + options.name + '-build.properties')
  properties = readPropertyFile(propertyFile)
  
  if status and properties['build.runtime']:
    #if there is a build.runtime and the status is not zero see if there are any *.log entries 
    printErrorLog(properties['build.runtime'])
  #the ant script writes a property file in a known location so we can read it
#we are currently not using any post processing so this line is commented out
# If the preprocessor needs to be run in the 
#  if not status and properties['build.tmp']:
#    postProcessZips(properties['build.tmp'], buildout)

  if not status:
    #if the build passed run the deploy artifacts
    printSeparator("Deploying the built RCP's to Google Storage")
    status = deployArtifacts(buildout, options.dest, 
                             properties['build.tmp'], options.revision)
    if not status:
      printSeparator("Running the tests")
      status = runAnt('../com.google.dart.tools.tests.feature_releng', 'buildTests.xml', 
                      options.revision, options.name, buildroot, buildout, eclipsepath)
      properties = readPropertyFile(propertyFile)
      if status and properties['build.runtime']:
        #if there is a build.runtime and the status is not zero see if there are any *.log entries 
        printErrorLog(properties['build.runtime'])
  return status

def runAnt(dir, antfile, revision, name, buildroot, buildout, sourcepath):
  '''
  Run the given Ant script from the given directory
  
  Args:
  dir - the directory to run the ant script from
  antfile - the ant file to run
  revision - the SVN revision of this build
  name - the name of the builder
  buildroot - root of the build source tree
  buildout - the location to copy output
  sourcepath - the path to the root of the source 
  '''
  cwd = os.getcwd()
  os.chdir(dir)
  print "cwd = {0}".format(os.getcwd())
  # this is not used untill we run under the Third_party copy of ant
  #create the classpath
#  env = os.environ
#  env['CLASSPATH'] = os.path.join(bzip2libpath, 'bzip2.jar')
  # Build the targets for each requested configuration.
  args = ['/bin/bash',
          '/usr/bin/ant',
          #currently the copy of ant in third_party will not build the 
          #code properly so we are going to use the version of Ant that
          #is installed on the build slave.
          #TODO: figure out why third_party Ant does not work and have 
          #the build use that version of ant (mrrussell@google.com)
#         os.path.join(antpath, 'bin', 'ant'), '-lib', bzip2libpath, 
         '-f', 
         antfile, 
         '-noinput',
         '-Dbuild.revision=' + revision, 
         '-Dbuild.builder=' + name,
         '-Dbuild.root=' + buildroot,
         '-Dbuild.out=' + buildout,
         '-Dbuild.source=' + sourcepath,
         '-nouserlib',
         ]
  extraArgs = os.environ.get('ANT_EXTRA_ARGS')
  if extraArgs != None:
    parsedExtra = extraArgs.split()
    for arg in parsedExtra:
      args.append(arg)
      
  print ' '.join(args)
  
  status = subprocess.call(args)
  
  os.chdir(cwd)
  return status
  
def readPropertyFile(file):
  '''
  read a property file created by the Ant run and return a dict of key/value pares
  
  Args:
  file - the file to read
  '''
  properties = {'key':'test'}
  print 'processing file ' + file
  for line in open(file):
    #ignore comments
    if not line.startswith('#'):
      parts = line.split('=')
      key = str(parts[0]).strip()
      value = str(parts[1]).strip()
      properties[key] = value

  return properties

def postProcessZips(tmpDir, buildout):
  '''
  run the post processor on the zipfiles
  
  Args:
  tmpDir - the location to work on the files
  '''
  #copy the zip files to a new temp directory
  workdir = os.path.join(tmpDir.strip(), 'postprocess')
  os.makedirs(workdir)
  print 'copying zip files from %s to %s' % (buildout, workdir)
  for zip in glob.glob(os.path.join(buildout, '*.zip')):
    shutil.copy(zip, os.path.join(workdir, os.path.basename(zip)))
  #process the zip files to add any files
  postprocess.processZips(workdir)
  #copy the zip files back
  print 'copying zip files from %s to %s' % (workdir, buildout)
  for zip in glob.glob(os.path.join(workdir, '*.zip')):
    shutil.copy(zip, os.path.join(buildout, os.path.basename(zip)))


def printErrorLog(rootDir):
  '''
  print an eclipse error log if one is found
  
  args:
  rootDir - the directory to start from 
  '''
  print "search " + rootDir + " for error logs"
  found = False
  configDir = os.path.join(rootDir, 'eclipse', 'configuration')
  if (os.path.exists(configDir)):
    for logfile in glob.glob(os.path.join(configDir, '*.log')):
      print "Found log file: " + logfile
      found = True
      for logline in open(logfile):
        print logline
  if not found:
    print "no log file was found in " + configDir

def deployArtifacts(fromd, to, tmp, svnid):
  '''
  deploy the artifacts (zipped RCP applications) to Google Storage
  
  This function copies the artifacts to two places 
  gs://dart-editor-archive-continuous/svnid and 
  gs://dart-editor-archive-continuous/latest.  
  Google Storage Does not have sym links so we have to make two
  copies of the deployed artifacts so there will always be a 
  constant continuous URL.
  args:
  fromd - directory the zipped RCP applications are located
  to - the base location in Google Storage
  tmp - the temporary working directory
  svnid - the svn revision number for this build
  '''
  print 'deploying zips in {0} to {1} (tmp: {2} svnID: {3})'.format(str(fromd), str(to), 
                                                                    str(tmp), str(svnid))
  cwd = os.getcwd()
  deployDir = None
  status = None
  userName = os.environ.get('USER')
  forceDeploy = os.environ.get('DART_FORCE_DEPLOY')
  if (not(userName.startswith('chrome') or forceDeploy != None)):
    return 0
  
  try:
    os.chdir(tmp)
    args = []
    botGsUtil = '/b/build/scripts/slave/gsutil'
    homeGsUtil = os.path.join(os.path.expanduser('~'), 'gsutil', 'gsutil')
    gsutil = None
    if (os.path.exists(botGsUtil)):
      gsutil = botGsUtil
    elif os.path.exists(homeGsUtil):
      gsutil = homeGsUtil
    if (len(gsutil) > 0):
      deployDir = os.path.join(tmp, str(svnid))
      print 'creating directory ' + deployDir
      os.makedirs(deployDir)
      artifacts = []
      for zipFile in glob.glob(os.path.join(fromd, '*.zip')):
        artifacts.append(zipFile)
        shutil.copy2(zipFile, deployDir)
         
      args.append(gsutil)
      args.append('cp')
      args.append('-r')
      args.append('-a')
      args.append('public-read')
      args.append(svnid)
      args.append(to)
      
      print ' '.join(args)
      
      status = subprocess.call(args)
      if status:
        print "*****************************"
        print "*****************************"
        print " the push to Google Storage of {0} failed".format(svnid)
        print "*****************************"
        print "*****************************"
      else:
        deployDir = os.path.join(tmp, 'latest')
        shutil.move(svnid, 'latest')
        args = []
        args.append(gsutil)
        args.append('cp')
        args.append('-r')
        args.append('-a')
        args.append('public-read')
        args.append('latest')
        args.append(to)
        
        print ' '.join(args)
        
        status = subprocess.call(args)
        if status:
          print "*****************************"
          print "*****************************"
          print " the push to Google Storage of latest failed"
          print "*****************************"
          print "*****************************"
    else:
      print "*****************************"
      print "*****************************"
      print "could not find gsutil.  tried {0} and {1}".format(botGsUtil, homeGsUtil)
      print "*****************************"
      print "*****************************"
      
    print "code Successfully deployed to:{2}\t{0}/{1}{2}\t{0}/latest".format(to, svnid, os.linesep)
    print "The URL's for the artifacts:"
    for artifact in artifacts:
      print "  {1} -> {0}/latest/{1}".format(to, os.path.basename(artifact))
    print ''
    print "the console for Google storage for this project can be found at"
    print "https://sandbox.google.com/storage/?project=375406243259&pli=1#dart-editor-archive-continuous"
    sys.stdout.flush()

  finally:
    os.chdir(cwd)
    if deployDir:
      shutil.rmtree(deployDir)
                    
  return status

def printSeparator(text):
  '''
  print a separator for the build steps
  '''
  #used to print separators during the build process
  tagLineSeperator = "================================"
  tagLinetext = "= {0}"

  print tagLineSeperator
  print tagLineSeperator
  print tagLinetext.format(text)
  print tagLineSeperator
  print tagLineSeperator

if __name__ == '__main__':
  sys.exit(main())
