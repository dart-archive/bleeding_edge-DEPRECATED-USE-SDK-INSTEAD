Building the Dart Editor RCP distro

Assuming Dart sources are checked out, there are four steps to building
Dart Editor. This process is only required because the RCP export wizard
cannot handle linked resources. The steps are:

1. Copy all the sources into a working directory.
2. Build an Eclipse workspace from the working directory.
3. Create the distro using Eclipse's Product export wizard.
4. Rename the export diretory and compress it into an archive file.

Step 1 is automated by the rcpinit.sh bash shell script, in the same
directory as this README. It also uses emptyDartWorkspace.zip (in the
same directory) to initialize  step 2.

Step 2 is completed manually, as is step 3. Step 4 is optional.

It is recommended that all files in this build directory be copied into the
parent directory of TRUNK (as defined in rcpinit.sh).

To begin, make sure the Dart plugin and feature sources are checked out from
SVN. Edit rcpinit.sh to define the location of the TRUNK directory that was
checked out. Also checkout the usage profile plugin and feature from perforce.
Define that directory in rcpinit.sh as GDT_PROF. You only the the usage
profiler, not all of GPE.

From a terminal emulator, cd to the directory containing the customized
rcpinit.sh and execute it. That completes step 1, and initializes step 2.
Note that a new directory called workDart is created. It contains a copy
of all the sources needed to build the distro.

Start up Eclipse. Switch workspace to the workDart directory created by
rcpinit.sh. Import the existing projects into Eclipse, but do not copy them.

Open the dart_feature.product definition (in com.google.dart.tools.deploy) in
the Product Editor (double-click it). Find the "Exporting" section in the lower
right, and click the "Eclipse Product export wizard" link to start the wizard.
Select an output directory. It should be empty to start; re-using a directory
does not clear out old stuff and can cause problems. You can uncheck the
"Generate metadata repository" option. Allow the wizard to finish.

In the selected output directory, rename the "eclipse" directory to "darttools"
then compress it for distribution. Finally, test the result. Reversing those
steps seems logical, but testing creates a lot of files and directories that
should not be distributed. To test the distro, import the Total app, build it,
and run it. Try a few editor functions like code completion and hyperlinking.

Note: To get the usage profiler without getting all of GPE you can use a two
stage checkout. Log into your Ubiquity instance, create a new directory, cd
into it and do:

git5 start trunk google3/third_party/java/google_plugin_eclipse/opensource/trunk/plugins/com.google.gdt.eclipse.usageprofiler
git5 track google3/third_party/java/google_plugin_eclipse/opensource/trunk/features/com.google.gdt.eclipse.usageprofiler.feature

Then use 'git clone' to make that directory available to your build machine.
In rcpinit.sh, change GDT_PROF to point to the cloned directory. Of course,
you only need to clone it if you are building on a Mac.

Note: These instructions only create an executable that runs on the same
platform as the build box. Cross-platform building requires some additional
work, including downloading the delta pack from eclipse.org.

There may be useful info in the RCP How-to:

http://wiki.eclipse.org/Eclipse_RCP_How-to

