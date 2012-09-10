Do NOT re-format the Eclipse source files in this project. The debugger displays source from the
original, un-patched file. If the source is relatively unchanged, it may be possible to make use
of the debugger.

Many errors and warnings have been disabled. There's no point reporting them for files in this
plug-in, since the files were copied from Eclipse sources.

Add new class file transforms to transform.csv. We need a better way to specify transform paths.
Currently, every class file produced by a single compilation unit (i.e. nested classes) needs to be
specified separately.

Testing a patch requires installing org.eclipse.osgi as a source plugin. For discussion on this
topic, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=143696.

Source to the original eclipse class that needs to be modified is also required, obviously.

The easy way to get source projects from eclipse plugins:
1) Show the class you want to edit in Eclipse (Ctrl-Shift-T, type the class name, enter)
2) Show the Plug-ins View in your workbench (Window, Show View, Other, PDE, select Plug-ins)
3) Find the corresponding package in the Plug-ins View
4) Right-mouse click the package name and select Import as Source Project

Additional patching mechanisms can be created by installing other patch bundles. Transforms based
on XSLT and sed are available from eclipse.org.

References:
http://www.eclipse.org/project-slides/Equinox%20Transforms%20Review.pdf
http://wiki.eclipse.org/Steps_to_use_Fragments_to_patch_a_plug-in
http://wiki.eclipse.org/Equinox_Transforms
http://wiki.eclipse.org/Adaptor_Hooks
http://wiki.eclipse.org/PDE/Incubator/ProductCustomization
