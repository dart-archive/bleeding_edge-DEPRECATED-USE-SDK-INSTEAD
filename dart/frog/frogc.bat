@echo off
REM Run frogc.dart on the Dart VM with its libdir set correctly.

set SCRIPTPATH=%~dp0

REM Does string have a trailing slash? If so, remove it.
IF %SCRIPTPATH:~-1%==\ set SCRIPTPATH=%SCRIPTPATH:~0,-1%
set LIBPATH=%SCRIPTPATH%\..\lib

%SCRIPTPATH%\dart.exe --new_gen_heap_size=128 %SCRIPTPATH%\frogc.dart --no_colors --libdir=%LIBPATH% %*
