# This creates a "war" directory for dartboard.  Once you have a
# war directory, you can test dartboard in the appengin devserver,
# or you can upload it to appengine for public release.
#
# TODO - eventually make a real build file for this.

set -x
mkdir -p out/war
cp -r WEB-INF out/war/
cp -r codemirror out/war/
cp dartboard.html out/war/
cp dartboard.dart.js out/war/
cp dartlib.html out/war/

# to run appengine dev server
#   appengine-java-sdk-1.6.3.1/bin/dev_appserver.sh out/war

# to deploy to appengine
#   appengine-java-sdk-1.6.3.1/bin/appcfg.sh update out/war/