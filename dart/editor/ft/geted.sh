#!/bin/bash
export DEV=http://gsdview.appspot.com/dart-archive/channels/dev
export ED_DIR=$DEV/raw/latest/editor
export ED_FILE=darteditor-macos-x64.zip
rm -f $ED_FILE
/usr/local/bin/wget -nv $ED_DIR/$ED_FILE
rm -rf ~/Desktop/dart
unzip -qq $ED_FILE -d ~/Desktop
rm -f $ED_FILE
