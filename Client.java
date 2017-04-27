//package assignment 3;

import java.net.*;
import java.io.*;
import java.util.Random;

public class Client{

	static DataOutputStream outToLogServer;
	static DataInputStream inFromLogServer;
	static Socket logServerSocket;

	static Socket docServerSocket;
	static DataOutputStream outToDocServer;
	static DataInputStream inFromDocServer;


	public static void main (String args[]){
		//arguments supply message and hostname of destination
		Random rand = new Random();
		String clientName = args[0];

		//Connect to the logging server
		int logServerPort = Integer.parseInt(args[1]);
		connectLogServer(logServerPort);
		msgLogServer(clientName + ": started");

		//Connect to the document server

		String data = "";

		
		try {
			msgLogServer(clientName + " - Connecting to doc server...");
			connectDocServer();
			msgLogServer(clientName + " - Connected.");
			msgLogServer(clientName + " - Requesting file list");
			outToDocServer.writeUTF("filelist");	//UTF is a string encoding.
			data = inFromDocServer.readUTF();
			msgLogServer(clientName + " - received: " + data);

		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}
		
		msgLogServer(clientName + " - going to sleep...");
		int n = (rand.nextInt(100) + 1) * 100;
		try {
			Thread.sleep(n);
		} catch (InterruptedException e){
			System.out.println(e);
		}
		msgLogServer(clientName + " - awake.");

		try {
			msgLogServer(clientName + " - connecting to doc server...");
			connectDocServer();
			msgLogServer(clientName + " - connected.");
			msgLogServer(clientName + " - requesting a file.");
			outToDocServer.writeUTF("filerequest");	//UTF is a string encoding.
			data = inFromDocServer.readUTF();
			msgLogServer(clientName + " - received: " + data);
			//Use received IP address and Port number to request the file.
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}

		//Unpack the data.
		String[] address = data.split(":");
		int fileServerPort = Integer.parseInt(address[1]);
		if(address[0] != "localhost"){
			//TODO handle the case where the IP address is actually different.
		}
		
		Socket fileServerSocket = null;
		
		try {
			msgLogServer(clientName + " - connecting to the file server.");
			//Connect to the given address
			fileServerSocket = new Socket(address[0], fileServerPort);
			msgLogServer(clientName + " - connected.");
			DataInputStream inFromFileServer = new DataInputStream(fileServerSocket.getInputStream());
			DataOutputStream outToFileServer = new DataOutputStream(fileServerSocket.getOutputStream());

			//Request the file
			msgLogServer(clientName + " - requesting a file.");
			outToFileServer.writeUTF("filerequest");

			//Receive response
			String dataIn = inFromFileServer.readUTF();
			msgLogServer(clientName + " - received response: " + dataIn);

		} catch (UnknownHostException e){System.out.println("C-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());
		} finally{
			if(fileServerSocket!=null) try{fileServerSocket.close();} catch(IOException e){/*Close Failed*/}
		}

		disconnectDocServer();
		msgLogServer(clientName + " - is all done.");
		disconnectLogServer();
	}

	public static void connectDocServer(){
		try {
			if(docServerSocket!=null){
				docServerSocket.close();
			}
			int serverPort = 61607;			
			docServerSocket = new Socket("localhost", serverPort);
			inFromDocServer = new DataInputStream(docServerSocket.getInputStream());
			outToDocServer = new DataOutputStream(docServerSocket.getOutputStream());
		} catch (UnknownHostException e){System.out.println("C-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());
		} finally{}

	}

	public static void disconnectDocServer(){

		if(docServerSocket!=null){
			try{
				docServerSocket.close();
			} catch(IOException e){
				/*Close Failed*/}
		}
	}



	public static void connectLogServer (int logServerPort){
		try{
			logServerSocket = new Socket("localhost", logServerPort);
			inFromLogServer = new DataInputStream(logServerSocket.getInputStream());
			outToLogServer = new DataOutputStream(logServerSocket.getOutputStream());

		} catch (UnknownHostException e){System.out.println("C-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());
		} finally{}


	}

	public static void disconnectLogServer(){
		//msgLogServer("exit");
		if(logServerSocket!=null){
			try{
				logServerSocket.close();
			} catch(IOException e){
				/*Close Failed*/}
		}
	}

	public static void msgLogServer(String msgToSend){
		String data;

		try {
			outToLogServer.writeUTF(msgToSend);	//UTF is a string encoding.
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}
		
		try {
			data = inFromLogServer.readUTF();
			//System.out.println("Doc Server: Response Received: " + data);
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}
	}

}