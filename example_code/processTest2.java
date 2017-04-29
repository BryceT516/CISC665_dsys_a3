//package Control;
import java.io.*;
import java.lang.*;

public class processTest2 {
	
	public static void main(String args[]) {
		try {
			ProcessBuilder client1 = new ProcessBuilder("java.exe", "TCPClient", "Hello world 1", "localhost");
			ProcessBuilder client2 = new ProcessBuilder("java.exe", "TCPClient", "Hello world 2", "localhost");
			Process runClient1 = client1.start();
			Process runClient2 = client2.start();
			
			runClient1.waitFor();
			runClient2.waitFor();

			System.out.println("Program complete");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}