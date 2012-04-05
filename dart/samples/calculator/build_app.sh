#!/bin/bash

# Example:
#
#   ./build_app.sh ~/www
#
# get the number of command-line arguments given
ARGC=$#

if [[ $ARGC -ne 1 ]];
then
  echo "Output directory required"
else
  echo "Generating template"
  ../../utils/template/template calcui.tmpl calcui.dart
  cd ../../frog

  echo "Compiling Dart code"
  ./frog.py --html -- --out=../samples/calculator/calculator.js ../samples/calculator/calculator.dart

  echo "Creating dart directory in ~/www"
  mkdir $1/dart
  chmod 777 $1/dart

  echo "Creating calculator directory in ~/www/dart"
  mkdir $1/dart/calculator
  chmod 777 $1/dart/calculator

  echo "Copying all Calculator files to ~/www/dart/calculator"
  cp ../samples/calculator/*.* $1/dart/calculator/.
  chmod 777 $1/dart/calculator/*.*

  echo "ChromeOS application ready"
fi
