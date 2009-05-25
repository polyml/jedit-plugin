package polyml;

import java.util.Date;

public class TestThread extends Thread implements Runnable {
	Long time;
	String activateString;
	
	public TestThread(String s) {
		activateString = s;
	}
	
	public void run() {
		time = (new Date()).getTime() + 1000;
		
		while((new Date()).getTime() < time) {
			System.out.println("waiting..." + (new Date()).getTime());
			PolyMLPlugin.debugMessage("\nwaiting..." + (new Date()).getTime()); 
			try {
				sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		synchronized (activateString){
			activateString.notifyAll();
		}
	}
}



/* 
PolyMLPlugin.debugMessage("eeeeeek! \n\n"); 

String s = new String();
TestThread t = new TestThread(s);
synchronized (s) {
	t.start();
	try {
		s.wait(10000);
	} catch (InterruptedException e2) {
		e2.printStackTrace();
	}
}
 */