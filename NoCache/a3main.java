//package Control;
import java.io.*;
import java.lang.*;
import java.util.concurrent.*;
import java.net.*;
import java.util.Date;
import java.util.Scanner;

public class a3main {
	
	public static void main(String args[]) {

		//Start up a logging server thread to collect information from the 
		// components of the experiment.
		String logServerPort;
		StartServer logServer = new StartServer();

		while(logServer.getPortNumber() == 0){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e){
				Thread.currentThread().interrupt();
			}
			
		}

		logServerPort = new String ().valueOf(logServer.getPortNumber());

		// Now launch the processes for the experiment, providing the logServerPort number
		//Launch the document server:

		try {
			ProcessBuilder docServer = new ProcessBuilder("java.exe", "DocServer", "Doc Server", logServerPort);
			docServer.inheritIO();
			Process runDocServer = docServer.start();
			
			//Read in the test scenario file, each line is a new client, launch clients per line.
			ClientInfoNode head = null;
			ClientInfoNode currentPtr = null;
			int clientCount = 0;

			try {
				String scenarioFile = "scenario1.txt";
				File file = new File(scenarioFile);
				Scanner scanner = new Scanner(file);
				String clientInfoIn = "";
				String[] clientInfoArray = new String[2];
				try {
					while(scanner.hasNextLine()){
						clientInfoIn = scanner.nextLine();
						//Parse the line for client info
						clientInfoArray=clientInfoIn.split(","); //Filename and decision time
						if (head == null){
							currentPtr = new ClientInfoNode();
							currentPtr.filenameWanted = clientInfoArray[0];
							currentPtr.decisionWaitTime = clientInfoArray[1];
							head = currentPtr;
							clientCount++;
						} else {
							currentPtr.next = new ClientInfoNode();
							currentPtr = currentPtr.next;
							currentPtr.filenameWanted = clientInfoArray[0];
							currentPtr.decisionWaitTime = clientInfoArray[1];
							clientCount++;
						}
					}
				} finally {
					scanner.close();
				}

			} catch (IOException e){};

			Process[] runClients = new Process[clientCount];
			currentPtr = head;

			for(int z = 0; z < runClients.length; z++){
				ProcessBuilder client = new ProcessBuilder("java.exe", "Client", "Client " + z, logServerPort, currentPtr.filenameWanted, currentPtr.decisionWaitTime);
				client.inheritIO();
				runClients[z] = client.start();
				currentPtr = currentPtr.next;
				if (currentPtr == null){
					break;
				}
			}		
			
			//Wait for all clients to complete
			for (int y = 0; y < runClients.length; y++){
				runClients[y].waitFor();
			}



			//Testing is complete, shut down the document server
			Socket docServerSocket=null;
			try {
				int serverPort = 61607;			
				docServerSocket = new Socket("localhost", serverPort);
				DataInputStream inFromDocServer = new DataInputStream(docServerSocket.getInputStream());
				DataOutputStream outToDocServer = new DataOutputStream(docServerSocket.getOutputStream());
				outToDocServer.writeUTF("quit");
				String data = inFromDocServer.readUTF();

			} catch (UnknownHostException e){System.out.println("a-Sock: " + e.getMessage());
			} catch (IOException e){System.out.println("a-IO: " + e.getMessage());
			} finally{if(docServerSocket!=null) try{docServerSocket.close();}catch(IOException e){/*Close Failed*/}}


			runDocServer.waitFor();

			//System.out.println("Clients completed");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	

		logServer.terminate();

		//System.out.println("Sent Terminate Signal");

		try{
			logServer.join();
		} catch (InterruptedException e){
			System.out.println(e);
		}
		
		//System.out.println("Program Completed");

	}
}

class StartServer extends Thread{
	int portNumber = 0;
	Boolean runFlag = true;

	public StartServer (){
		//System.out.println("Logging Server starting...");
		this.start();
	}

	public void run(){
		//Start the file connection
		BlockingQueue<String> logFileOutput = new ArrayBlockingQueue<String>(256);
		LogFileWriter logWriterThread = new LogFileWriter(logFileOutput);

		try{
			//int serverPort = 7896;
			ServerSocket listenSocket = new ServerSocket(0);
			this.portNumber = listenSocket.getLocalPort();
			//System.out.println("Server listening on: " + this.portNumber);
			listenSocket.setSoTimeout(100);
			while(this.runFlag) {				
				try{
					Socket clientSocket = listenSocket.accept();
					Connection c = new Connection(clientSocket, logFileOutput);
					System.out.println(".");
				} catch(IOException e) {}
				
			}
			
			
		} catch(IOException e) {System.out.println("Listen : " + e.getMessage());} 

		logWriterThread.terminateLogFileWriter();

		//System.out.println("Logging server shutting down...");
	}

	public int getPortNumber() {
		//Return the port number obtained by the server
		return this.portNumber;
	}

	public void terminate() {
		//System.out.println("Logging Server: Terminate signal received");
		this.runFlag = false;
	}

}

class Connection extends Thread{
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	String portNumber = "";
	BlockingQueue<String> logFileOutput= null;


	public Connection (Socket aClientSocket, BlockingQueue<String> logFileOutputIn){
		logFileOutput=logFileOutputIn;
		try{
			clientSocket = aClientSocket;
			portNumber = Integer.toString(clientSocket.getLocalPort());
			in = new DataInputStream( clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			this.start();
			//System.out.println("Log Server Connection");
		} catch (IOException e) {System.out.println("Connection: "+e.getMessage());}
	}
	public void run(){
		Boolean getMsgFlag = true;
		try {	//An echo server
			while(getMsgFlag){
				String data = in.readUTF();
				
				if(data == "exit"){
					getMsgFlag=false;
				} else {
					System.out.println(data);
					try {
						logFileOutput.put(data); //Add the text to the queue to be written to the log file.
					} catch (InterruptedException e) {System.out.println("Adding log entry to queue fail: " + e);}
					
					out.writeUTF(data + ": " + portNumber);
				}
				
			}
			
		} catch(EOFException e) {/*System.out.println("a-EOF: " + e.getMessage());*/
		} catch(IOException e) {System.out.println("a-IO-2: " + e.getMessage());
		} finally {try{clientSocket.close();}catch(IOException e) {/*close failed*/}}
	}
}

class LogFileWriter extends Thread{
	BlockingQueue<String> logEntryQueue;
	FileWriter outToLogFile = null;
	Boolean writeToFileFlag = true;

	public LogFileWriter (BlockingQueue<String> logEntryQueueIn){
		this.logEntryQueue = logEntryQueueIn;
		//Open the file for writing
		try {
			outToLogFile = new FileWriter("logFile.txt", true);
		} catch (IOException e) {System.out.println("log file failed: " + e);}
		this.start();	//launch the thread

	}

	public void run(){
		String outputText = "";
		try { //Writing to file
			while(writeToFileFlag){
				//Get data from the queue
				try {
					outputText = logEntryQueue.poll(100,TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {System.out.println("Reading from logging queue fail: " + e);}
				
				//Append it to the file
				if(outputText != null){
					outToLogFile.write(outputText + "\n");
				}
			}
			outToLogFile.close();
		} catch(IOException e){System.out.println("File logger fail: " + e);}
	}

	public void terminateLogFileWriter(){
		writeToFileFlag = false;
	}
}


class ClientInfoNode{
	public ClientInfoNode next = null;
	public String filenameWanted = "";
	public String decisionWaitTime = "";

	public void ClientInfoNode(){

	}

}

