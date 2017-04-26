//package assignment 3;

import java.net.*;
import java.io.*;
import java.lang.*;

public class DocServer{

	public static void main (String args[]){
		int serverPortNumber = 0;
		Boolean runFlag = true;
		//Get connected to the log Server
		int logServerPort = Integer.parseInt(args[1]);
		Socket logSocket = null;
		try{
			logSocket = new Socket("localhost", logServerPort);
			DataInputStream logIn = new DataInputStream(logSocket.getInputStream());
			DataOutputStream logOut = new DataOutputStream(logSocket.getOutputStream());	
			logOut.writeUTF("DocServer: Starting...");	//UTF is a string encoding.	
			

			try{
				ServerSocket listenSocket = new ServerSocket(0);
				serverPortNumber = listenSocket.getLocalPort();
				logOut.writeUTF("DocServer: Port:" + serverPortNumber);
				System.out.println("DocServer listening on: " + serverPortNumber);
				listenSocket.setSoTimeout(10000);
				while(runFlag) {				
					try{
						Socket clientSocket = listenSocket.accept();
						Connection c = new Connection(clientSocket);
						System.out.println(".");
						runFlag = false;						
					} catch(IOException e) {}
				}
			} catch(IOException e) {System.out.println("DS-Listen : " + e.getMessage());}


		} catch (UnknownHostException e){System.out.println("DS-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("DS-IO: " + e.getMessage());
		} finally{if(logSocket!=null) try{logSocket.close();} catch(IOException e){/*Close Failed*/}}
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