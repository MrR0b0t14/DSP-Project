package it.polito.dsp;

import java.io.IOException;

public class WritingServer {

	public static void main(String[] args) {
		
		int port = 2001;
		MyWriter writer = null;
		
		//Creation of the server socket
		try {
			writer = new MyWriter(port);
		} catch (IOException e) {
			System.out.println("Error in the creation of the socket server.");
			e.printStackTrace();
			System.exit(0);
		}
		
		//Management of the communications with clients
		System.out.println("Server running on port " + port + "."); //TODO: Check println
		try {
			writer.execute();
		} catch (IOException e) {
			System.out.println("Error in the management of the server socket.");
			e.printStackTrace();
		}
		
		//Closure of the server socket (in case of error)
		try {
			writer.stop();
		} catch (IOException e) {
			System.out.println("Error in the closure of the server socket.");
			e.printStackTrace();
		}

	}

}
