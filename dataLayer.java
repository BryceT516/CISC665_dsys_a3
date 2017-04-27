//package Control;
import java.io.*;
import java.lang.*;
import java.util.concurrent.*;
import java.net.*;

public class dataLayer {
	//Class to implement the data layer, serves up files

	public static void main(String args[]) {
		ThruChannel channel = new ThruChannel();
		StartFileServer fileServer = new StartFileServer(channel);

		try {
			fileServer.join();
		} catch (InterruptedException e){
			System.out.println(e);
		}

	}

}

class StartFileServer extends Thread{
	Boolean runFlag = true;
	ThruChannel channel;

	public StartFileServer(ThruChannel channelIn){
		//System.out.println("File server starting...");
		this.channel = channelIn;
		this.start();
	}

	public void run(){
		try{
			int serverPort = 61627;
			ServerSocket listenSocket = new ServerSocket(serverPort);
			listenSocket.setSoTimeout(1000);
			while(this.channel.readFlag()){
				try {
					Socket clientSocket = listenSocket.accept();
					FileServerConnection c = new FileServerConnection(clientSocket, this.channel);
					//System.out.println("+");
				} catch(IOException e){}
			}
		} catch(IOException e) {System.out.println("File Server Listen: " + e.getMessage());}
		//System.out.println("File server shutting down...");

	}

}

class FileServerConnection extends Thread{
	DataInputStream in;
	DataOutputStream out;
	Socket clientSocket;
	String portNumber = "";
	ThruChannel channel;

	public FileServerConnection (Socket aClientSocket, ThruChannel channelIn){
		this.channel = channelIn;
		try{
			clientSocket = aClientSocket;
			in = new DataInputStream( clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			this.start();
			//System.out.println("Data Layer Connection");
		} catch (IOException e) {System.out.println("Connection: " + e.getMessage());}
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
							//Get the document from file
							//Transfer the file
							out.writeUTF("File requested.");
							conversing = false;//End connection
							break;
						case "filelist": //Request for list of files
							//This only comes from the doc server.
							out.writeUTF("file list goes here");

							conversing = false;
							break;
						case "quit": //Request is a command to quit
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
			
		} catch(EOFException e) {System.out.println("EOF: " +e.getMessage());
		} catch(IOException e) {System.out.println("IO: "+e.getMessage());
		} finally {try{clientSocket.close();}catch(IOException e) {/*close failed*/}}
	}
}


class ThruChannel {
	Boolean flag = true;
	public ThruChannel(){
		this.flag = true;
	}
	public Boolean readFlag(){
		return this.flag;
	}
	public void setFlag(Boolean flagSetVal){
		this.flag = flagSetVal;
	}
}





