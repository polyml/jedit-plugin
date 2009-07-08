package polyml;

import java.io.File;
import java.io.FileFilter;

import org.gjt.sp.jedit.Buffer;

public class ProjectTools {

	static public class PolyMLSaveDir implements FileFilter {
		public boolean accept(File f) {
			File savedir = new File(f.getParent() + File.separator
					+ PolyMLProcess.POLY_SAVE_DIR);
			//System.err.println("PolyMLSaveDir: \n  " + f + "\n  " + savedir);
			return (f.compareTo(savedir) == 0);
		}
	}

	static public File searchForProjectSaveDir(Buffer b) {
		File p = new File(b.getDirectory()).getAbsoluteFile();
		File projectDir = null;

		while (projectDir == null && p != null) {
			//System.err.println("looking for .polysave in:  " + p);
			File[] polysavedir = p.listFiles(new PolyMLSaveDir());
			if (polysavedir.length != 0) {
				//System.err.println("Found .polysave in:  " + p);
				projectDir = new File(p.getAbsolutePath() + File.separator
						+ PolyMLProcess.POLY_SAVE_DIR);
				//System.err.println("Looking for:  " + heapFile);
				if (!projectDir.exists()) {
					projectDir = null;
				}
			}
			p = p.getParentFile();
		}

		return projectDir;
	}


	static public String searchForProjectDir(Buffer b) {
		File projectSaveDir = searchForProjectSaveDir(b);
		if (projectSaveDir != null) {
			return projectSaveDir.getParent();
		} else {
			File bFile = new File(b.getPath());
			return bFile.getParent();
		}
	}
	
	static public String searchForBufferHeapFile(Buffer b) {
		String s = b.getPath();
		File p = new File(b.getDirectory()).getAbsoluteFile();
		String heap = null;
		boolean noProject = true;

		while (noProject && p != null) {
			//System.err.println("looking for .polysave in:  " + p);
			File[] polysavedir = p.listFiles(new PolyMLSaveDir());
			if (polysavedir.length != 0) {
				//System.err.println("Found .polysave in:  " + p);
				File heapFile = new File(p.getAbsolutePath() + File.separator
						+ PolyMLProcess.POLY_SAVE_DIR + File.separator
						+ s.substring(p.getPath().length()) + ".save");
				//System.err.println("Looking for:  " + heapFile);
				if (heapFile.exists()) {
					heap = heapFile.getAbsolutePath();
					noProject = false;
				}
			}
			p = p.getParentFile();
		}

		return heap;
	}


	public static String MLStringForLoadHeap(String heapPath) {
		return "PolyML.SaveState.loadState \"" + heapPath + "\";\n";
	}
	
}
