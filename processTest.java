//package Control;
import java.io.*;
import java.lang.*;

public class processTest {
	
	public static void main(String args[]) {
		try {
			ProcessBuilder broker = new ProcessBuilder("java.exe", "test");
			Process runBroker = broker.start();
			
			Reader reader = new InputStreamReader(runBroker.getInputStream());
			int ch;
			while ((ch = reader.read())!= -1)
				System .out.println((char)ch);
			reader.close();

			runBroker.waitFor();

			System.out.println("Program complete");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}