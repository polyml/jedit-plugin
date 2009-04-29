---------------------
 PolyML Plugin 0.1.0
---------------------

This is a plugin for working with PolyML (http://www.polyml.org). It requires PolyML 5.3 or later. 
The source code is under the GNU GPL license: http://www.gnu.org/copyleft/gpl.html

---------------------
 Build and Install
---------------------
To build the plugin use ant from the PolyML Jedit Plugin directory: 

  ant clean; ant

This will create the file "PolyML.jar", which you can then place in the "jars" directory of jedit, or in your user plugins directory. On unix, the user jedit plugins directory is "$HOME/.jedit/jars/".

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
