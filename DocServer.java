//package assignment 3;

import java.net.*;
import java.io.*;
import java.util.Random;

public class DocServer{
	static Socket logServerSocket = null;
	static DataInputStream inFromLogServer = null;
	static DataOutputStream outToLogServer = null;

	Boolean shutDown = false;


	public static void main (String args[]){
		//arguments supply message and hostname of destination
		Random rand = new Random();
		int logServerPort = Integer.parseInt(args[1]);
		//Connect to the log server
		connectLogServer(logServerPort);
		//Log starting up
		msgLogServer("DocServer starting...");
		//Start the data layer process
		try {
			ProcessBuilder dataLayerServer = new ProcessBuilder("java.exe", "dataLayer"); //Not sending any arguments
			dataLayerServer.inheritIO();
			Process runDataLayerServer = dataLayerServer.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Wait to give the process time to start up
		try {
			Thread.sleep(500);
		} catch (InterruptedException e){
			Thread.currentThread().interrupt();
		}

		//Connect to the data layer
		Socket dataServerSocket = null;
		try{
			dataServerSocket = new Socket("localhost", 61627);
			DataInputStream inFromDataServer = new DataInputStream(dataServerSocket.getInputStream());
			DataOutputStream outToDataServer = new DataOutputStream(dataServerSocket.getOutputStream());
			//Request the list of files (with usage data)
			outToDataServer.writeUTF("filelist");
			String dataLayerResp = inFromDataServer.readUTF();
			//System.out.println("DocServer: Data layer response: " + dataLayerResp);

		} catch (UnknownHostException e){System.out.println("DS-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("DS-IO: " + e.getMessage());
		} finally{
			if(dataServerSocket!=null) try{dataServerSocket.close();} catch(IOException e){/*Close Failed*/}
		}

		
		//Prepare the list of files
			//TODO - linked list of files with status info?
				// filename, usage info, current cache IP, file info
		//Prepare the cache information: cache 1 count, cache 2 size

		//Start cache process(es)

		//Start listening for client requests
		ThruChannel channel = new ThruChannel();
		try{
			int docServerPort = 61607;
			ServerSocket docServerSocket = new ServerSocket(docServerPort);
			docServerSocket.setSoTimeout(1000);
			while(channel.readFlag()){
				try {
					Socket clientSocket = docServerSocket.accept();
					DocServerConnection c = new DocServerConnection(clientSocket, channel);
					//System.out.println("*");
				} catch(IOException e){}
			}
		} catch(IOException e) {System.out.println("Doc Server Listen: " + e.getMessage());}
		//System.out.println("Doc server shutting down..."); //Quit command received
				//Tell cache processes to quit
				//Tell data layer process to quit
				try{
					dataServerSocket = new Socket("localhost", 61627);
					DataInputStream inFromDataServer = new DataInputStream(dataServerSocket.getInputStream());
					DataOutputStream outToDataServer = new DataOutputStream(dataServerSocket.getOutputStream());
					//Request the list of files (with usage data)
					outToDataServer.writeUTF("quit");
					String dataLayerResp = inFromDataServer.readUTF();
					//System.out.println("DocServer: Data layer response: " + dataLayerResp);

				} catch (UnknownHostException e){System.out.println("DS-Sock2: " + e.getMessage());
				} catch (IOException e){System.out.println("DS-IO2: " + e.getMessage());
				} finally{
					if(dataServerSocket!=null) try{dataServerSocket.close();} catch(IOException e){/*Close Failed*/}
				}

		//Disconnect from the logging server.
		//msgLogServer("exit");
		disconnectLogServer();

	}

	public static void connectLogServer (int logServerPort){
		try{
			logServerSocket = new Socket("localhost", logServerPort);
			inFromLogServer = new DataInputStream(logServerSocket.getInputStream());
			outToLogServer = new DataOutputStream(logServerSocket.getOutputStream());

		} catch (UnknownHostException e){System.out.println("DS-Sock3: " + e.getMessage());
		} catch (IOException e){System.out.println("DS-IO3: " + e.getMessage());
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
		} catch (IOException e){System.out.println("DS-IO: " + e.getMessage());}
		
		try {
			data = inFromLogServer.readUTF();
			//System.out.println("Doc Server: Response Received: " + data);
		} catch (IOException e){System.out.println("DS-IO: " + e.getMessage());}
	}

}


class DocServerConnection extends Thread{
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	String portNumber = "";
	ThruChannel channel;

	public DocServerConnection (Socket aClientSocket, ThruChannel channelIn){
		this.channel = channelIn;
		try{
			clientSocket = aClientSocket;
			in = new DataInputStream( clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			this.start();
			//System.out.println("Doc Server Connection");
		} catch (IOException e) {System.out.println("Doc Server Connection: " + e.getMessage());}
	}
	public void run(){
		Boolean getMsgFlag = true;
		String data = "";
		Boolean conversing = true;
		try {
			while (conversing){
				//Read in the type of request
				data = in.readUTF();
				//Validate the request
				if(data == null){
					//TODO - handle this
				} else {
					switch (data.toLowerCase()){
						case "filerequest": //Document request:
							//Query for the file name
							//Receive the file name
							//Check if the file name is in the cache
								//In cache - send the IP address and port number
								//cache miss - 
									//Kill a cache
									//Spawn a cache with the new file request
									//Once the IP address and port number return
									//Send the IP address and port number to client
									out.writeUTF("localhost:61627");
									//Update the cache list
							conversing = false;//End connection
							break;
						case "filelist": //Request for list of files
							// Respond with the list of files available and costs.
							out.writeUTF("Doc Server file list goes here");

							conversing = false;
							break;
						case "quit": //Request is a command to quit
							this.channel.setFlag(false);
							out.writeUTF("quitting.");
							conversing = false;
							break;
						default:
							out.writeUTF("Doc Server unknown request");	//UTF is a string encoding.
							break;
					}				
				}
			
			}
			
		} catch(EOFException e) {System.out.println("EOF: " +e.getMessage());
		} catch(IOException e) {System.out.println("IO: "+e.getMessage());
		} finally {try{clientSocket.close();}catch(IOException e) {/*close failed*/}}
	}
}


