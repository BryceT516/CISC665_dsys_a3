//package assignment 3;

import java.net.*;
import java.io.*;

import java.util.Date;
import java.text.*;

public class Client{

	static DataOutputStream outToLogServer;
	static DataInputStream inFromLogServer;
	static Socket logServerSocket;

	static Socket docServerSocket;
	static DataOutputStream outToDocServer;
	static DataInputStream inFromDocServer;


	public static void main (String args[]){
		//arguments supply message and hostname of destination
		
		String clientName = args[0];

		//Connect to the logging server
		int logServerPort = Integer.parseInt(args[1]);
		connectLogServer(logServerPort);
		msgLogServer(clientName + "-started-" + args[2]);

		//Connect to the document server

		String data = "";

		
		try {
			//msgLogServer(clientName + " - Connecting to doc server...");
			connectDocServer();
			//msgLogServer(clientName + " - Connected.");
			//msgLogServer(clientName + " - Requesting file list");
			outToDocServer.writeUTF("filelist");	//UTF is a string encoding.
			data = inFromDocServer.readUTF();
			//msgLogServer(clientName + " - received: " + data);

		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}
		
		//msgLogServer(clientName + " - going to sleep...");
		int n = Integer.parseInt(args[3]);
		try {
			Thread.sleep(n);
		} catch (InterruptedException e){
			System.out.println(e);
		}
		//msgLogServer(clientName + " - awake.");

		try {
			msgLogServer(clientName + "-connecting to doc server");
			connectDocServer();
			msgLogServer(clientName + "-connected to doc server");
			msgLogServer(clientName + "-requesting a file");
			outToDocServer.writeUTF("filerequest");	//UTF is a string encoding.
			data = inFromDocServer.readUTF();
			msgLogServer(clientName + "-received " + data); //Should be "filename"
			outToDocServer.writeUTF(args[2]); //Send the filename
			msgLogServer(clientName + "-sent filename");
			data = inFromDocServer.readUTF();
			msgLogServer(clientName + "-received cache address");
			//Use received IP address and Port number to request the file.
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}

		disconnectDocServer();

		//Unpack the data.
		String[] address = data.split(":");
		int fileServerPort = Integer.parseInt(address[1]);
		if(!address[0].equals("localhost")){
			//TODO handle the case where the IP address is actually different.
		}
		
		Socket fileServerSocket = null;
		
		try {
			msgLogServer(clientName + "-connecting to cache server-" + address[0] + ":" + address[1]);
			//Connect to the given address (this will serve the desired file)
			fileServerSocket = new Socket(address[0], fileServerPort);
			msgLogServer(clientName + "-connected to cache server");
			DataInputStream inFromFileServer = new DataInputStream(fileServerSocket.getInputStream());
			DataOutputStream outToFileServer = new DataOutputStream(fileServerSocket.getOutputStream());

			//Request the file
			msgLogServer(clientName + "-requesting file from cache");
			outToFileServer.writeUTF("filerequest");

			String dataIn = inFromFileServer.readUTF(); //Read in the total size and number of chunks
			msgLogServer(clientName + "-received file info-" + dataIn);
			String[] fileInfo = new String[2];
			int breakerChar = dataIn.indexOf(',');
			fileInfo[0] = dataIn.substring(0, breakerChar); // The total file length
			fileInfo[1] = dataIn.substring(breakerChar + 1); // The number of chunks to be sent
			long fileLength = Long.parseLong(fileInfo[0]);
			int chunkCount = Integer.parseInt(fileInfo[1]);

			//Prepare to receive the chunks
			StringBuilder fileReceived = new StringBuilder((int)fileLength);
			//Receive the chunks.
			for (int v = 0; v < chunkCount; v++){
				fileReceived.append(inFromFileServer.readUTF());
			}
			//Finalize the received text of the file.
			String finalFileReceived = fileReceived.toString();
			msgLogServer(clientName + "-received file");

		} catch (UnknownHostException e){System.out.println(clientName +"C-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println(clientName +"C-IO: " + e.getMessage());
		} finally{
			if(fileServerSocket!=null) try{fileServerSocket.close();} catch(IOException e){/*Close Failed*/}
		}

		
		msgLogServer(clientName + "-is complete");
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
		SimpleDateFormat ft = new SimpleDateFormat ("hh:mm:ss:S");
		Date datetime = new Date();
		try {
			outToLogServer.writeUTF(  msgToSend+ "-" +ft.format(datetime));	//UTF is a string encoding.
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}

	
		try {
			data = inFromLogServer.readUTF();
			//System.out.println("Doc Server: Response Received: " + data);
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}


	}

}