//package assignment 3;

import java.net.*;
import java.io.*;
import java.util.Random;

public class Client{
	public static void main (String args[]){
		//arguments supply message and hostname of destination
		Socket s = null;
		Random rand = new Random();

		try{
			int serverPort = Integer.parseInt(args[1]);
			System.out.println("Client port: " + serverPort);
			s = new Socket("localhost", serverPort);
			DataInputStream in = new DataInputStream(s.getInputStream());
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			String data;
			int n;

			for(int i = 0; i<10; i++){
				out.writeUTF(args[0] + " : " + i);	//UTF is a string encoding.
				try {
					data = in.readUTF();
					System.out.println("Client: Response Received: " + data);
				} catch (IOException e){System.out.println("C-IO: " + e.getMessage());}
				
				n = (rand.nextInt(50) + 1) * 100;
				try {
					Thread.sleep(n);
				} catch (InterruptedException e){
					System.out.println(e);
				}
			}
			


			out.writeUTF("exit");	//UTF is a string encoding.
			data = in.readUTF();
			System.out.println("Client: Response Received: " + data);
				
		} catch (UnknownHostException e){System.out.println("C-Sock: " + e.getMessage());
		} catch (IOException e){System.out.println("C-IO: " + e.getMessage());
		} finally{if(s!=null) try{s.close();} catch(IOException e){/*Close Failed*/}}
	}

}