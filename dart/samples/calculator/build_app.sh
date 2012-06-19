#!/bin/bash

# Example:
#
#   Ensure that dart2js is in your PATH .
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

  echo "Compiling Dart code"
  dart2js --out=calculator.js calculator.dart

  echo "Creating dart directory in $1"
  mkdir $1/dart
  chmod 777 $1/dart

  echo "Creating calculator directory in $1/dart"
  mkdir $1/dart/calculator
  chmod 777 $1/dart/calculator

  echo "Copying all Calculator files to $1/dart/calculator"
  cp * $1/dart/calculator/.
  chmod 777 $1/dart/calculator/*

  echo "ChromeOS application ready"
fi
