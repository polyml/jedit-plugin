---------------------
 PolyML Plugin 0.1.0
---------------------

This is a plugin for working with PolyML (http://www.polyml.org). It requires PolyML 5.3 or later. 
The source code is under the GNU GPL license: http://www.gnu.org/copyleft/gpl.html

---------
 Install 
---------

The file "PolyML.jar" contains the compiled plugin, put this in the "jars" directory of jedit, or in your user plugins directory. On unix, the user jedit plugins directory is "$HOME/.jedit/jars/". Hint: making a symbolic link here will let you use "svn update" to update to the latest version of the plugin.  

--------------
 (re) Compile
--------------

You can re-compile the plugin using ant. This requires that you specify the location of your installed jEdit in a "build.properties" file. This is so that ant can find the jEdit.Jar file and the needed plugins. You can copy and edit the "build.properties.sample" file for this.

Note that the PolyML plugin requires the ErrorList plugin to be installed. 

Then you can (re)compile the PolyML jEdit Plugin with: 

  ant clean; ant

Then follow the instructions above (Install from compiled jar) for installing the newly created "PolyML.jar" file.

-------------------
 Details
-------------------

build.xml is the ant makefile.

There are currently two parts to this code: 

1. ShellBuffer: you can create a shell-buffer for an emacs-style interaction with an ML shell.  

2. PolyMLProcess which interacts with PolyML 5.3/development version using this --ideprotocol setting. see: http://www.polyml.org/docs/IDEProtocol.html

-------------------
 More Information
-------------------

The PolyML website is at: 
http://www.polyml.org.

Its sourceforce development page is:       
http://sourceforge.net/projects/polyml/ 

The PolyML mailing list is at: 
http://lists.inf.ed.ac.uk/mailman/listinfo/polyml
