//package Control;
import java.io.*;
import java.lang.*;
import java.util.concurrent.*;
import java.net.*;

public class a3main {
	
	public static void main(String args[]) {

		String ServerPort;
		
		StartServer docServer = new StartServer();

		while(docServer.getPortNumber() == 0){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e){
				Thread.currentThread().interrupt();
			}
			
		}

		ServerPort = new String ().valueOf(docServer.getPortNumber());

		try {
			ProcessBuilder client1 = new ProcessBuilder("java.exe", "Client", "Client 1", ServerPort);
			ProcessBuilder client2 = new ProcessBuilder("java.exe", "Client", "Client 2", ServerPort);
			Process runClient1 = client1.start();
			Process runClient2 = client2.start();
			
			runClient1.waitFor();
			runClient2.waitFor();

			System.out.println("Clients completed");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		docServer.terminate();

		System.out.println("Sent Terminate Signal");

		try{
			docServer.join();
		} catch (InterruptedException e){
			System.out.println(e);
		}
		
		System.out.println("Program Completed");

	}
}

class StartServer extends Thread{
	int portNumber = 0;
	Boolean runFlag = true;

	public StartServer (){
		System.out.println("Logging Server starting...");
		this.start();
	}

	public void run(){
		try{
			//int serverPort = 7896;
			ServerSocket listenSocket = new ServerSocket(0);
			this.portNumber = listenSocket.getLocalPort();
			System.out.println("Server listening on: " + this.portNumber);
			listenSocket.setSoTimeout(100);
			while(this.runFlag) {				
				try{
					Socket clientSocket = listenSocket.accept();
					Connection c = new Connection(clientSocket);
					System.out.println(".");
				} catch(IOException e) {}
				
			}
			
			
		} catch(IOException e) {System.out.println("Listen : " + e.getMessage());} 

		System.out.println("Logging server shutting down...");
	}

	public int getPortNumber() {
		//Return the port number obtained by the server
		return this.portNumber;
	}

	public void terminate() {
		System.out.println("Logging Server: Terminate signal received");
		this.runFlag = false;
	}

}

class Connection extends Thread{
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	public Connection (Socket aClientSocket){
		try{
			clientSocket = aClientSocket;
			in = new DataInputStream( clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			this.start();
		} catch (IOException e) {System.out.println("Connection: "+e.getMessage());}
	}
	public void run(){
		try {	//An echo server
			String data = in.readUTF();
			System.out.println("Received: " + data);
			out.writeUTF(data);
		} catch(EOFException e) {System.out.println("EOF: " +e.getMessage());
		} catch(IOException e) {System.out.println("IO: "+e.getMessage());
		} finally {try{clientSocket.close();}catch(IOException e) {/*close failed*/}}
	}
}


