#!/usr/bin/python

# Copyright (c) 2011 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""Eclipse Dart post process of the rcp files

"""
import glob
import os
import re
import shutil
import sys
import zipfile

configPattern = r'(.+)\[(.+)\]'
destPattern = r'(.)-(.+)'

def processZips(zipdir):
  '''
  Process the zips in the given directory.  Add the given files from the postporcess.config 
  file into the zips
  
  Args:
  zipdir - the directory the zip files are located in
  '''
  print 'processZips(%s)' % zipdir 
  
  #process the configuration file to get the files to add to the zip files
  zipFileData = __processConfig()
  #stop processing if there are no files to add or the configuration file could not be found
  if len(zipFileData) > 0:
    for zip in glob.glob(os.path.join(zipdir, '*.zip')):
      print 'proecssing zip file %s' %zip
      zf = zipfile.ZipFile(zip, mode='a')
      #read the first element from the zip file to get the prefix 
      for info in zf.infolist():
        prefix = info.filename
        break;
      
      try:
        #loop through the files to add to the zip files  
        for file in zipFileData.iterkeys():
          #a file may be added in more the one place in the zipfile so loop through all locations
          for dir in zipFileData[file]:
            dir = os.path.join(prefix, dir)
            #the directory structure has to be created on disk so the code can be added to the 
            # zip file
            os.makedirs(dir)
            shutil.copy(file, dir);
            fileToAdd = os.path.join(dir, file)
            print 'adding %s to file' % fileToAdd
            zf.write(fileToAdd)
            #remove the created directory and file to cleanup
            shutil.rmtree(dir)
      except:
        #handle exceptions
        print "Unexpected error:", sys.exc_info()[0]
        raise
      finally:
        zf.close()
  
def __processConfig():
  '''
  process the zip processor configuration file
  
  The File layout of the configuration file is:
   
  Lines starting with # are comments and are not processed
  Configuration lines start with the relative or absolute path to a file to add to a zip.  Next 
  comes a square bracket '['. Inside the square brackets is a list of one or more tag-directory listings
  and then a closing square bracket ']'
  
  tag-directory
  tag:
  R - relative to the root of the zip file
  directory:
  the path from the root of the zip file to add the given file
  
  Example:
  #this is an example configuration file
  debug.options[R-configuration]
  config.ini[R-/]
  README[R-/,R-info]
  
  line one says to add debug.options file in the /configuration directory in the zip file
  line two says to add config.ini file in the / directory of the zip file
  line three says to add README file in the / and /info directories in the zip file
  '''
  configFileName = "postporcess.config"
  zipFileData = {}

  #do nothing if it does not exist
  if os.path.exists(configFileName):
    for line in open(configFileName):
      print 'processing: %s' % line
      #skip comments
      if line.startswith("#"):
        continue
      else:
        pattern = re.match(configPattern, line)
        #if the pattern matches then get the two parts
        if pattern:
          file = pattern.group(1)
          dest = pattern.group(2)
          #split the directories
          destList = dest.split(',')
      print 'file %s [ %s ] \n' % (file, ', '.join(destList))
      #fail if the file specified can not be found
      if (not os.path.exists(file)):
        raise OSError("could not find file " + file)
      
      count = 0
      
      for element in destList:
        #search for the R-dir pattern
        pattern = re.match(destPattern, element)
        #if it is found
        if pattern:
          target = pattern.group(1)
          value = pattern.group(2)
          #if the target is R store it
          if target == 'R':
            destList[count] = value
            count += 1
          else:
            #print a message if a target is specified but not supported
            print str(target) + " is not supported yet"

      #store the list
      zipFileData[file] = destList
  else:
    print 'file %s does not exist, no post processing done' % configFileName

  print zipFileData
  return zipFileData


def main():
  con = open('postporcess.config', 'w+')
  con.write('#test config file\n')
  con.write('test.file[R-config,R-dropins]\n')
  con.close()
  processZips(os.path.join('/', 'var', 'tmp'))
  
if __name__ == '__main__':
  sys.exit(main())
