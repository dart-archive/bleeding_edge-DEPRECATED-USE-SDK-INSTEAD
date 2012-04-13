@echo off
REM Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
REM for details. All rights reserved. Use of this source code is governed by a
REM BSD-style license that can be found in the LICENSE file.

REM Run frogc.dart on the Dart VM with its libdir set correctly.

set SCRIPTPATH=%~dp0

REM Does the string have a trailing slash? If so, remove it.
if %SCRIPTPATH:~-1%==\ set SCRIPTPATH=%SCRIPTPATH:~0,-1%
set LIBPATH=%SCRIPTPATH%\..\lib

REM Canonicalize the direction of the slashes.
set script=%*
set script=%script:\=/%

"%SCRIPTPATH%\dart.exe" "%SCRIPTPATH%\frogc.dart" --no_colors --libdir="%LIBPATH%" %script%
