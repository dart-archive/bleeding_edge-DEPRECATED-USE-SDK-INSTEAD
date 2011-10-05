#!/bin/bash

# Note: this is very fragile. If the projects that contain linked resources
# change then this script will need to be update. Pay particular attention
# to the here-docs, which redefine the projects to eliminate linkage.

export TRUNK=~/src/dart-all/dart
export GDT_PROF=~/src/prof-git5/google3/third_party/java/google_plugin_eclipse/opensource/trunk
export PLUGINS=$TRUNK/eclipse/tools/plugins
export FEATURES=$TRUNK/eclipse/tools/features
rm -rf workDart
unzip $TRUNK/../emptyDartWorkspace.zip > unzip.log
rm -r __MACOSX
cd workDart
cp -r $PLUGINS/* .
cp -r $FEATURES/*.feature .
cp -r $GDT_PROF/plugins/* .
cp -r $GDT_PROF/features/* .
cd com.google.dart.compiler.js
mkdir src-compiler
mkdir third_party
cp -r $TRUNK/compiler/java/* src-compiler
cp -r $TRUNK/third_party/* third_party
cd ..
cd com.google.dart.library.core
mkdir src-corelib
mkdir src-corelib-dartc
cp -r $TRUNK/corelib/* src-corelib
cp -r $TRUNK/compiler/lib/* src-corelib-dartc
cd ..
cd com.google.dart.library.dom
mkdir src-dom
cp -r $TRUNK/client/dom/* src-dom
cd ..
cat <<UpToEndOfFirstProject > com.google.dart.compiler.js/.project
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>com.google.dart.compiler.js</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.ManifestBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.SchemaBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.pde.PluginNature</nature>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
</projectDescription>
UpToEndOfFirstProject
cat <<UpToEndOfSecondProject > com.google.dart.library.core/.project
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>com.google.dart.library.core</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.ManifestBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.SchemaBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.pde.PluginNature</nature>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
</projectDescription>
UpToEndOfSecondProject
cat <<UpToEndOfThirdProject > com.google.dart.library.dom/.project
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>com.google.dart.library.dom</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.ManifestBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.pde.SchemaBuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.pde.PluginNature</nature>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
	<filteredResources>
		<filter>
			<id>1304830150715</id>
			<name>src-dom</name>
			<type>5</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-*.lib</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150720</id>
			<name>src-dom</name>
			<type>5</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-*.dart</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150725</id>
			<name>src-dom</name>
			<type>5</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-*.js</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150730</id>
			<name>src-dom</name>
			<type>26</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-test</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150735</id>
			<name>src-dom</name>
			<type>26</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-webkit</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150740</id>
			<name>src-dom</name>
			<type>6</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-dart_dom_webkit.lib</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150746</id>
			<name>src-dom</name>
			<type>26</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-snippets</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150751</id>
			<name>src-dom</name>
			<type>10</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-scripts</arguments>
			</matcher>
		</filter>
		<filter>
			<id>1304830150756</id>
			<name>src-dom</name>
			<type>26</type>
			<matcher>
				<id>org.eclipse.ui.ide.multiFilter</id>
				<arguments>1.0-name-matches-false-false-idl</arguments>
			</matcher>
		</filter>
	</filteredResources>
</projectDescription>
UpToEndOfThirdProject
