In order to optimize the jars (re-order their entries into load-order):

- Build the application, and run it with '-vmargs -verbose:class'. Save the resulting text into a file
(class_load_order.txt).

- Run the jar processor tool over the target plugins, to re-order their entries based in the load
order in foo.txt

java -jar ../../../util/dart.jar.processor/dist/JarProcessor.jar -processDir ../../../../third_party/eclipse/3.7.0/plugins class_load_order.txt 
