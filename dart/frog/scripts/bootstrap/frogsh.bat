@echo off
REM Call frogsh using node.

set SCRIPTPATH=%~dp0

REM Does the string have a trailing slash? If so, remove it.
if %SCRIPTPATH:~-1%==\ set SCRIPTPATH=%SCRIPTPATH:~0,-1%

REM Canonicalize the direction of the slashes.
set script=%*
set script=%script:\=/%

node "%SCRIPTPATH%\frogsh" --no_colors %script%
