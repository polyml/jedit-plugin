package polyml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class PolyMLProcess {
	static char ESC = 0x1b;
	static char EOT = 0x04;
	
	Process process;
	BufferedWriter writer;
	BufferedReader reader;
	
	public PolyMLProcess() throws IOException {
		String cmd = "poly";
		ProcessBuilder pb = new ProcessBuilder(cmd, "--ideprotocol");
		
		pb.redirectErrorStream(true);
		try {
			process = pb.start();
		} catch (IOException e) {
			System.err.println("PolyMLProcess:" + "Failed to start process: " + cmd);
			process = null;
			throw e;
		}
		reader = new BufferedReader(new InputStreamReader(process
				.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(process
				.getOutputStream()));
	}
	
	public synchronized void closeProcess() {
		if(process != null) {
			send("" + EOT);
			process = null;
		}
	}
	
	/**
	 * 
	 * @param heap
	 * @param srcFileName
	 * @param startPos
	 * @param src
	 * @return
	 * @throws MarkupException
	 * @throws IOException
	 */
	public synchronized CompileResult compile(String heap, String srcFileName, int startPos, String src) {
		String loadHeap;
		if(heap != null) { loadHeap = heap; } else { loadHeap = ""; }
		
		String compile_cmd = ESC + "R" + loadHeap + ESC + "," + srcFileName + ESC + 
			"," + startPos + ESC + "," + src + ESC + 'r';
		send(compile_cmd);
		PolyMarkup m = null;
		try {
			m = PolyMarkup.readPolyMarkup(reader);
		} catch (MarkupException e) {
			e.printMarkupException();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new CompileResult(loadHeap, m);
	}
	
	/**
	 * Compile the string
	 * @param srcFileName
	 * @param src
	 * @return
	 * @throws MarkupException
	 * @throws IOException
	 */
	public CompileResult compile(String srcFileName, String src) {
		return compile(null, srcFileName, 0, src);
	}
	
	public synchronized void send(String command) {
		if(writer != null){
			try {
				writer.write(command);
				writer.newLine(); // CHECK: is this ok? 
				writer.flush();
			} catch (IOException e) {
				System.err.println("Exit value from polyml: "
						+ process.exitValue());
				e.printStackTrace();
			}
		}
	}

	
	//0x1b
	
	//ESC 'R' loadfile ESC ',' sourcename ESC ',' startposition ESC ',' source ESC 'r'
}
