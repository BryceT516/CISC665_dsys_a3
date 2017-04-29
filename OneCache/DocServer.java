//package assignment 3;

import java.net.*;
import java.io.*;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class DocServer{
	static Socket logServerSocket = null;
	static DataInputStream inFromLogServer = null;
	static DataOutputStream outToLogServer = null;
	static CacheManager cacheManager = null;

	final static int maxCacheProcs = 5; //Set the number of cache processes to have active concurrently

	Boolean shutDown = false;

	public static void main (String args[]){
		//arguments supply message and hostname of destination
		Random rand = new Random();
		int logServerPort = Integer.parseInt(args[1]);
		//Connect to the log server
		connectLogServer(logServerPort);
		//Log starting up
		//msgLogServer("DocServer starting...");
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

		} catch (UnknownHostException e){System.out.println("DS-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("DS-IO: " + e.getMessage());
		} finally{
			if(dataServerSocket!=null) try{dataServerSocket.close();} catch(IOException e){/*Close Failed*/}
		}

		
		//Start the CacheManager - TODO get a list of files and usage from data layer.
		cacheManager = new CacheManager(maxCacheProcs, outToLogServer, logServerPort);

		//Start listening for client requests
		ThruChannel channel = new ThruChannel();
		try{
			int docServerPort = 61607;
			ServerSocket docServerSocket = new ServerSocket(docServerPort);
			docServerSocket.setSoTimeout(1000);
			while(channel.readFlag()){
				try {
					Socket clientSocket = docServerSocket.accept();
					DocServerConnection c = new DocServerConnection(clientSocket, channel, cacheManager);
					
				} catch(IOException e){}
			}
		} catch(IOException e) {System.out.println("Doc Server Listen: " + e.getMessage());}
		//System.out.println("Doc server shutting down..."); //Quit command received
				//Tell cache processes to quit
				cacheManager.terminateCache();

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
	CacheManager cacheManager;


	public DocServerConnection (Socket aClientSocket, ThruChannel channelIn, CacheManager cacheManagerIn){
		this.channel = channelIn;
		this.cacheManager = cacheManagerIn;

		try{
			clientSocket = aClientSocket;
			in = new DataInputStream( clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			this.start();
			//System.out.println("Doc Server Connection");
		} catch (IOException e) {System.out.println("Doc Server Connection: " + e.getMessage());}
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
							//Query for the file name
							out.writeUTF("filename");
							//Receive the file name
							data = in.readUTF();
							//Check if the file name is in the cache
							String[] fileInfo = null;
							fileInfo = cacheManager.fileRequested(data); //Returns the address and port number

							if (fileInfo != null){
								out.writeUTF(fileInfo[0]+":"+fileInfo[1]); //Send on the address and port number
							} else {
								//Major error, the filename is not in the file list
								out.writeUTF("file not found");
							}
							
							conversing = false;//End connection
							break;
						case "filelist": //Request for list of files
							// Respond with the list of files available and costs.
							out.writeUTF("Doc Server file list goes here");

							conversing = false;
							break;
						case "quit": //Request is a command have the doc server quit
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

class CacheManager {
	// Cache manager maintains a list of files
	// and the cache processes that have those files
	// The manager will terminate and spawn cache processes
	private int maxCacheProcs = 0;
	private int cacheProcsCount = 0;

	private FileInfoNode head = null;
	
	private Process[] caches = null;
	private Semaphore mutex = new Semaphore(1);

	DataOutputStream outToLogServer = null;

	String logServerPort = "";

	public CacheManager(int maxCacheProcsIn, DataOutputStream outToLogServerIn, int logServerPortIn){
		if (maxCacheProcsIn > 6){
			maxCacheProcs = 6;
		} else {
			maxCacheProcs = maxCacheProcsIn;
		}
		outToLogServer = outToLogServerIn;
		logServerPort = String.valueOf(logServerPortIn);

		// New cache manager
		// Load up the list of files with given info.
			//Parse out the data from the string.
		// *Hard coding the file list for now.
		try{
			mutex.acquire();

			addFile("file75k1.txt", 0);
			addFile("file110k1.txt", 0);
			addFile("file130k1.txt", 0);
			addFile("file200k1.txt", 0);
			addFile("file350k1.txt", 0);
			addFile("file500k1.txt", 0);
		
		} catch(InterruptedException e){

		} finally {
			mutex.release();
		}
		
		// Prepare the array of cache process hooks
		caches = new Process[maxCacheProcs];

		//Future todo: add a way to decide which files to load to cache initially.

	}

	private void addFile(String filenameIn, int accessCountIn){
		//Create a new node in the list with the given values.
		if(head == null){
			head = new FileInfoNode(filenameIn, accessCountIn);
			head.filename = filenameIn;
			head.accessCounter = accessCountIn;
		} else {
			FileInfoNode tempNode = new FileInfoNode(filenameIn, accessCountIn);
			tempNode.filename = filenameIn;
			tempNode.accessCounter = accessCountIn;
			tempNode.next = head;
			head = tempNode;
			tempNode = null;
		}

	}

	public String[] fileRequested(String filenameIn){
		//Lock the list to prevent changes
		String[] tempRtn = new String[2];
		int newCacheProcHookIndex = 0;
		String newPortNumber = "";
		String newIpAddress = "";

		try {
			mutex.acquire();
			//Search for a file in the list.
			FileInfoNode currentFile = findFilename(filenameIn);

			// Check process index
			if (currentFile.cacheProcHookIndex == -1){

				//The file is not currently in a cache
				if(cacheProcsCount < maxCacheProcs){
					//Just start a new cache process for the given filename.
					//Find an open index in the caches array
					
					while(caches[newCacheProcHookIndex] != null){
						newCacheProcHookIndex++;
					}
					
						ProcessBuilder cacheProcess = new ProcessBuilder("java.exe", "Cache", currentFile.filename, logServerPort);
						try {
							caches[newCacheProcHookIndex] = cacheProcess.start();
							cacheProcsCount++;
						} catch (IOException e){}
					
				} else {
					// Kill an existing cache process
						//Look through the list of files
						// get a pointer to a cache that has the lowest number
						// of accesses.
					FileInfoNode currentPtr = head;
					FileInfoNode killCandidate = null;
					while(currentPtr != null){
						if(currentPtr.cacheProcHookIndex > -1){
							if(killCandidate == null){
								killCandidate = currentPtr;
								currentPtr=currentPtr.next;
							} else {
								if(killCandidate.accessCounter > currentPtr.accessCounter){
									//Move to the candidate with fewest accesses
									killCandidate = currentPtr;
									currentPtr = currentPtr.next;
								} else {
									currentPtr = currentPtr.next;
								}
							}
						}
					}
						//Send the cache process the "quit" command.
					Socket cacheProcessSocket = null;
					try{
						cacheProcessSocket = new Socket(killCandidate.ipAddress, Integer.parseInt(killCandidate.portNumber));
						DataOutputStream outToCacheProcess = new DataOutputStream(cacheProcessSocket.getOutputStream());
						outToCacheProcess.writeUTF("quit");
					} catch(IOException e){

					} finally {
						if(cacheProcessSocket!=null){
							try {
								cacheProcessSocket.close();
							} catch(IOException e){/*close failed*/}
						}
					}

						//Wait for the cache to exit.
					caches[killCandidate.cacheProcHookIndex].waitFor();
					cacheProcsCount--;
						//Update the file info that the file is no longer in a cache process
					newCacheProcHookIndex = killCandidate.cacheProcHookIndex;
					killCandidate.cacheProcHookIndex = -1;
					killCandidate.ipAddress = "";
					killCandidate.portNumber = "";
					killCandidate = null;

						// Start a new cache process for the file, recycle the cache hook index
					ProcessBuilder cacheProcess = new ProcessBuilder("java.exe", "Cache", currentFile.filename, logServerPort);
					try {
						caches[newCacheProcHookIndex] = cacheProcess.start();
						cacheProcsCount++;
					} catch (IOException e){}


				}
				//A new cache process has started, listen for it to report its port number
				int cacheManagerPort = 61617;
				ServerSocket cacheManagerSocket = null;
				Socket cacheSocket = null;
				try {
					cacheManagerSocket = new ServerSocket(cacheManagerPort);
					cacheSocket = cacheManagerSocket.accept();
					DataInputStream inFromCache = new DataInputStream(cacheSocket.getInputStream());
					newPortNumber = inFromCache.readUTF(); //The cache process will report its port number
					newIpAddress = "localhost"; //Hard coded for now, cache process could be on another machine.
				} catch(IOException e) {

				} finally {
					if(cacheSocket!=null){
							try {
								cacheSocket.close();
							} catch(IOException e){/*close failed*/}
						}
					if(cacheManagerSocket!=null){
							try {
								cacheManagerSocket.close();
							} catch(IOException e){/*close failed*/}
						}
				}


				currentFile.ipAddress = newIpAddress;
				currentFile.portNumber = newPortNumber;
				currentFile.cacheProcHookIndex = newCacheProcHookIndex;
				// Update the file info with the cache process info
				// return the ipAddress and port number for the cache process				

			} else {
				//The file is currently in a cache
				// Return the ipAddress and port number

			}
			tempRtn[0] = currentFile.ipAddress;
			tempRtn[1] = currentFile.portNumber;

			//Increment the file request count.
			currentFile.accessCounter ++;
			

		} catch (InterruptedException e){

		} finally {
			mutex.release();
		}

		return tempRtn;
		
	}


	private FileInfoNode findFilename (String filenameIn){
		//Iterate through the list to find the desired filename
		FileInfoNode pointer = this.head;
		FileInfoNode returnPointer = null;
		Boolean seeking = true;
		while (seeking){
			if(pointer.filename.equals(filenameIn)){
				returnPointer = pointer;
				seeking = false;
			} else {
				if(pointer.next != null){
					pointer=pointer.next;
				} else {
					seeking=false;
				}
			}
		}
		return returnPointer;
	}

	public void terminateCache (){
		//Make all the cache processes terminate
		FileInfoNode currentPtr = head;
		while (currentPtr != null){
			if(currentPtr.cacheProcHookIndex > -1){
				//Send the terminate command to the cache process for this file
				Socket cacheProcessSocket = null;
				try{
					cacheProcessSocket = new Socket(currentPtr.ipAddress, Integer.parseInt(currentPtr.portNumber));
					DataOutputStream outToCacheProcess = new DataOutputStream(cacheProcessSocket.getOutputStream());
					outToCacheProcess.writeUTF("quit");
				} catch(IOException e){

				} finally {
					if(cacheProcessSocket!=null){
						try {
							cacheProcessSocket.close();
						} catch(IOException e){/*close failed*/}
					}
				}

					//Wait for the cache to exit.
				try{
					caches[currentPtr.cacheProcHookIndex].waitFor();
					cacheProcsCount --;
				} catch(InterruptedException e) {}

				currentPtr = currentPtr.next;
			} else {
				currentPtr = currentPtr.next;
			}
		}
	}

}



class FileInfoNode {
	public FileInfoNode next = null;
	public String filename = "";
	public int accessCounter = 0;
	public String ipAddress = "";
	public String portNumber = "";
	public int cacheProcHookIndex = -1;

	public FileInfoNode(String filenameIn, int accessCountIn){
		//Creating a new file info node.
		filename = filenameIn;
		accessCounter = accessCountIn;
		this.next = null;
	}


}



