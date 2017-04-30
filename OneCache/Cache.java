//package Control;
import java.io.*;
import java.lang.*;
import java.util.concurrent.*;
import java.net.*;
import java.util.Scanner;

public class Cache {
	//Class to implement the cache process, serves up a given file.
	static Socket logServerSocket = null;
	static DataInputStream inFromLogServer = null;
	static DataOutputStream outToLogServer = null;

	public static void main(String args[]) {
		//Args 0 = filename to cache.
		//Args 1 = log server port number

		String filenameVal = args[0];
		int logServerPort = Integer.parseInt(args[1]);
		//connect to the log server
		connectLogServer(logServerPort);

		//msgLogServer("Cache for " + filenameVal + " starting...");

		ThruChannel channel = null;
		channel = new ThruChannel();

		String[] dataArray = null;
		String fileTxInfo = "";

		//Get the file from the data layer
		Socket dataLayerSocket = null;
		try{
			dataLayerSocket = new Socket("localhost", 61627);
			DataInputStream inFromDataLayer = new DataInputStream(dataLayerSocket.getInputStream());
			DataOutputStream outToDataLayer = new DataOutputStream(dataLayerSocket.getOutputStream());
			outToDataLayer.writeUTF("filerequest");
			String dataRcv = inFromDataLayer.readUTF(); // Should be "filename"
			outToDataLayer.writeUTF(filenameVal); //Send the filename
			dataRcv = inFromDataLayer.readUTF(); //Total size and number of chunks
			fileTxInfo = dataRcv;
			String[] fileInfo = new String[2];
			fileInfo = dataRcv.split(",");
			long fileLength = Long.parseLong(fileInfo[0]);
			int chunkCount = Integer.parseInt(fileInfo[1]);
			dataArray = new String[chunkCount];

			//Receive the chunks.
			for (int v = 0; v < chunkCount; v++){
				dataArray[v] = inFromDataLayer.readUTF();
			}

			//msgLogServer("Cache: "+filenameVal + " : " + fileTxInfo + " : " + String.valueOf(dataArray.length));

		} catch (UnknownHostException e){System.out.println("Cache-Sock1: " + e.getMessage());
		} catch (IOException e){System.out.println("Cache-IO1: " + e.getMessage());
		} finally{
			if(dataLayerSocket!=null) try{dataLayerSocket.close();} catch(IOException e){/*Close Failed*/}
		} 

		//Now have the needed file data to pass on to clients...

		try{
			
			ServerSocket listenSocket = new ServerSocket(0);
			//Get server port
			String portNumber = Integer.toString(listenSocket.getLocalPort());
			//Send the port number to the cache manager. port 61617
			Socket cacheMgrSocket=null;
			try{
				cacheMgrSocket = new Socket("localhost", 61617);
				DataInputStream inFromCacheMgr = new DataInputStream(cacheMgrSocket.getInputStream());
				DataOutputStream outToCacheMgr = new DataOutputStream(cacheMgrSocket.getOutputStream());
				outToCacheMgr.writeUTF(portNumber); // Send the port number for this cache.

			} catch (UnknownHostException e){System.out.println("Cache-Sock2: " + e.getMessage());
			} catch (IOException e){System.out.println("Cache-IO2: " + e.getMessage());
			} finally{
				if(cacheMgrSocket!=null) try{cacheMgrSocket.close();} catch(IOException e){/*Close Failed*/}
			} 

			//msgLogServer("Cache "+ filenameVal +": Listening for clients..." + portNumber);
			//Start listening for client requests.
			listenSocket.setSoTimeout(500);
			while(channel.readFlag()){
				try {
					Socket clientSocket = listenSocket.accept();
					//msgLogServer("Cache "+ filenameVal +": connection accepted : " + String.valueOf(dataArray.length));
					CacheFileServerConnection c = new CacheFileServerConnection(clientSocket, channel, fileTxInfo, dataArray);

				} catch(IOException e){}
			}
		} catch(IOException e) {System.out.println("File Server Listen: " + e.getMessage());}
		
		//disconnect from the logging server
		disconnectLogServer();

	}


	public static void connectLogServer (int logServerPort){
		try{
			logServerSocket = new Socket("localhost", logServerPort);
			inFromLogServer = new DataInputStream(logServerSocket.getInputStream());
			outToLogServer = new DataOutputStream(logServerSocket.getOutputStream());

		} catch (UnknownHostException e){System.out.println("Cache-Sock3: " + e.getMessage());
		} catch (IOException e){System.out.println("Cache-IO3: " + e.getMessage());
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
		} catch (IOException e){System.out.println("Cache-IO4: " + e.getMessage());}
		
		try {
			data = inFromLogServer.readUTF();
		} catch (IOException e){System.out.println("Cache-IO5: " + e.getMessage());}
	}


}

class CacheFileServerConnection extends Thread{
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;

	ThruChannel channel;
	String[] dataArray = null;
	String fileTxInfo = "";



	public CacheFileServerConnection (Socket aClientSocket, ThruChannel channelIn, String fileTxInfoIn, String[] dataArrayIn){
		this.channel = channelIn;
		this.dataArray = dataArrayIn;
		this.fileTxInfo = fileTxInfoIn;

		try{
			clientSocket = aClientSocket;
			in = new DataInputStream( clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());

			this.start();
			
		} catch (IOException e) {System.out.println("Connection: " + e.getMessage());}
	}
	public void run(){

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

							//Send the total file size and the number of chunks to be sent.
							out.writeUTF(fileTxInfo);

							//Send each chunk.					
							for (int g=0; g < dataArray.length; g++){
								try {
									out.writeUTF(dataArray[g]);
								} catch(IOException e) {
									System.out.println("File Send: "+e.getMessage());
								}
							}
							conversing = false;//End connection
							break;
						case "quit": //Request is a command to have this cache process quit
							this.channel.setFlag(false);
							out.writeUTF("quitting");
							conversing = false;
							break;
						default:
							out.writeUTF("cmdnotknown");	//UTF is a string encoding.
							break;
					}				
				}
			
			}
			
		} catch(EOFException e) {System.out.println("Cache - EOF: " +e.getMessage());
		} catch(IOException e) {System.out.println("Cache - IO6: "+e.getMessage());
		} finally {try{clientSocket.close();}catch(IOException e) {/*close failed*/}}
	}
}




