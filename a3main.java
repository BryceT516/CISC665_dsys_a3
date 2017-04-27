//package Control;
import java.io.*;
import java.lang.*;
import java.util.concurrent.*;
import java.net.*;
import java.util.Date;

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
			
			//System.out.println("Document Server Process Started...");

			
			ProcessBuilder client1 = new ProcessBuilder("java.exe", "Client", "Client 1", logServerPort);
			//ProcessBuilder client2 = new ProcessBuilder("java.exe", "Client", "Client 2", logServerPort);
			client1.inheritIO();
			//client2.inheritIO();
			Process runClient1 = client1.start();
			//Process runClient2 = client2.start();
			
			runClient1.waitFor();
			//runClient2.waitFor();


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
		try{
			//int serverPort = 7896;
			ServerSocket listenSocket = new ServerSocket(0);
			this.portNumber = listenSocket.getLocalPort();
			//System.out.println("Server listening on: " + this.portNumber);
			listenSocket.setSoTimeout(100);
			while(this.runFlag) {				
				try{
					Socket clientSocket = listenSocket.accept();
					Connection c = new Connection(clientSocket);
					System.out.println(".");
				} catch(IOException e) {}
				
			}
			
			
		} catch(IOException e) {System.out.println("Listen : " + e.getMessage());} 

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

	public Connection (Socket aClientSocket){
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
		Date datetime = new Date();
		try {	//An echo server
			while(getMsgFlag){
				String data = in.readUTF();
				datetime = new Date();
				if(data == "exit"){
					getMsgFlag=false;
				} else {
					System.out.println(datetime.toString() + ": " + data);
					out.writeUTF(data + ": " + portNumber);
				}
				
			}
			
		} catch(EOFException e) {System.out.println("a-EOF: " + e.getMessage());
		} catch(IOException e) {System.out.println("a-IO-2: " + e.getMessage());
		} finally {try{clientSocket.close();}catch(IOException e) {/*close failed*/}}
	}
}


