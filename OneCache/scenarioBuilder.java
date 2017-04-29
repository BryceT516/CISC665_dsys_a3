//Scenario builder

//This program creates a file with a list of client info
// The data is used for testing the document vending application.
import java.io.*;
import java.util.Random;


public class scenarioBuilder {

	public static void main(String[] args) throws Exception {
		long start = System.currentTimeMillis();
		File file = new File("scenario1.txt");
		file.createNewFile();
		FileWriter writer = new FileWriter(file);

		final int numClients = 10;

		Random rand = new Random();

		String filename = "";
		int decisionTime = 0;		

		for (int u = 0; u < numClients; u++){
			//Randomly select a filename

			switch(rand.nextInt(6)) {
				case 0:
					filename = "file75k1.txt";
					break;
				case 1:
					filename = "file110k1.txt";
					break;
				case 2:
					filename = "file130k1.txt";
					break;
				case 3:
					filename = "file200k1.txt";
					break;
				case 4:
					filename = "file350k1.txt";
					break;
				case 5:
					filename = "file500k1.txt";
					break;
				default:
					filename = "file500k1.txt";
					break;
			}


			//Randomly create a decision wait time
			decisionTime = (rand.nextInt(500) + 1) * 10; //Random milliseconds for how long the client waits before requesting a document.

			//Write the information to the file: comma separated
			writer.write(filename + "," + Integer.toString(decisionTime));
			writer.write("\n");

		}
		writer.flush();
		writer.close();
		long end = System.currentTimeMillis();
		System.out.println((end-start)/1000f + " seconds");



	}

}

				