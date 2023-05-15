package it.polito.dsp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.security.*;
import java.time.LocalDateTime;

import it.polito.dsp.ClientExceptions.FileHasChangedException;
import it.polito.dsp.ClientExceptions.InvalidFileNameException;
import it.polito.dsp.ClientExceptions.RecoverConnectionException;
import it.polito.dsp.ClientExceptions.ServerFailureException;
import it.polito.dsp.ClientExceptions.StartNewInteractionException;
import it.polito.dsp.ClientExceptions.TCPConnectionDroppedException;
import it.polito.dsp.ClientExceptions.TransferFileException;
import it.polito.dsp.ClientExceptions.WrongBytesWrittenException;
import it.polito.dsp.ClientExceptions.WrongPassingArgumentException;

public class FileTransfer {

	private static final int CHUNK_LENGTH = 1024;
	private static final int TIMEOUT = 30*1000;
	private Socket socket;
	private DataOutputStream outputSocketStream = null; 
	private DataInputStream inputSocketStream = null;
	
	public FileTransfer(InetAddress serverAddress, int serverPort) throws IOException  {
		this.socket = new Socket(serverAddress, serverPort);
		inputSocketStream = new DataInputStream(socket.getInputStream());
	    outputSocketStream = new DataOutputStream(socket.getOutputStream()); 
		socket.setSoTimeout(TIMEOUT);
	}
	
	public static void printConnectionInterrupted() {
		System.out.println("TCP connection interrupted");
	}
	
	private void closeConnection() throws IOException {
			this.socket.close();
	}
	
	/*
	 * This function is used for the case in which the client gets only 1 argument,
	 * to start the interaction with the server. 
	 * 
	 * Throws:
	 * 		ServerFailureException -- If the server answers '1' for any reason.
	 * 		TCPConnectionDroppedException -- If the read over the socket stream returns '-1' or a TimeOut is reached.
	 * 		StartNewInteractionException -- If an exception is thrown internally to inform the caller.
	*/		
	private void startNewInteraction(String filePath) throws ServerFailureException, TCPConnectionDroppedException, StartNewInteractionException {
		try {
			//1. from path gets an ID 	
			String fileId = FileOperationsManager.pathToId(filePath, false);
			System.out.println(fileId);
			
			//2. sends ID to the server
			byte[] fileIdByteArray = fileId.getBytes(StandardCharsets.US_ASCII);
			this.outputSocketStream.write(fileIdByteArray);
			
			//3. receive server answer (1 or 0)
	         int isSuccess  = (int)this.inputSocketStream.read();
	         
	         switch(isSuccess) {
	         case 0:
	        	    byte[] serverFileName = new byte[255]; //xxxxxxxxxxtimestamp
	        	    int bytesRead = 1;
	        	    int readByte = this.inputSocketStream.read();
	        	    
	        	    if(readByte == 0)
	        	    	throw new InvalidFileNameException();
	        	    else if (readByte == -1)
	        	    	throw new TCPConnectionDroppedException();
	
	        	    while (bytesRead < 255 && readByte != 0) {
	        	    	serverFileName[-1 + bytesRead++] = (byte) readByte;
	        	        readByte = this.inputSocketStream.read();
	        	        if (readByte == -1) {
	        	            throw new TCPConnectionDroppedException();
	        	        }
	        	    }
	        	    
	        	    String receivedFileName = new String(serverFileName, StandardCharsets.US_ASCII);
	        	    System.out.println("If connection fails, please use this filename to recover it as second argument: " + receivedFileName);
	        	    break;
	         	case 1:
	         		this.closeConnection();
	         		throw new ServerFailureException(ServerFailureException.KindOfFailure.DURING_FIRST_INTERACTION);
	         	case -1: //In this case the connection has been dropped
	         		this.closeConnection();
	         		throw new TCPConnectionDroppedException();
	         }
		}catch(SocketTimeoutException e) {
			System.err.println("It seems that the TIMEOUT has been reached.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);				
			}
			throw new TCPConnectionDroppedException();
		}catch (InvalidFileNameException e) {
			System.err.println("It seems that you received a 0 bytes FileName, an error occured.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new StartNewInteractionException();
		}catch(NoSuchAlgorithmException e) {
			System.err.println("No algorithm found to generate file ID. Please change it and try again!");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new StartNewInteractionException();
		}catch(IOException e) {
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new StartNewInteractionException();
		}
	}
	
	
	/*
	 * This function is used to recover a previously interrupted connection
	 * at this purpose, 2 arguments must be provided to the client
	 * 
	 * Throws:
	 * 		ServerFailureException -- If the server answers '1' for any reason
	 * 		TCPConnectionDroppedException -- If the read over the socket stream returns '-1' or a TimeOut is reached.
	 * 		RecoverConnectionException -- If an exception is thrown internally to inform the caller.
	*/
	public int recoverConnection(String serverFileName) throws ServerFailureException, TCPConnectionDroppedException, RecoverConnectionException{
		int byteAlreadyWritten = 0;
		try {
			byte[] serverFileNameByteArray = (serverFileName.concat("\0")).getBytes(StandardCharsets.US_ASCII);
			System.out.println("If connection fails, please use this filename to recover it as second argument: " + serverFileName);
			
			//It sends the filename with which the server has saved the file
			this.outputSocketStream.write(serverFileNameByteArray);
			
			//receive server answer (2 or 1) and in case of success (2) it receives the #bytes already written
	         int isSuccess  = (int)this.inputSocketStream.read();
	         
	         switch(isSuccess) {
	         case 2:
	        	 	byteAlreadyWritten = this.inputSocketStream.readInt();
	        	 	return byteAlreadyWritten;
	         	case 1:
	         		this.closeConnection();
	         		throw new ServerFailureException(ServerFailureException.KindOfFailure.DURING_RECOVERY);
	         	case -1: //Connection Dropped
	         		this.closeConnection();
	         		throw new TCPConnectionDroppedException();
	         	case 0: //This is a strange case in which the client sends a valid file ID in recovery mode and server unexpectedly answers 0.
	         		this.closeConnection();
	         		throw new WrongPassingArgumentException();
	         }
		}catch(SocketTimeoutException e) {
			System.err.println("It seems that the TIMEOUT has been reached.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);				
			}
			throw new TCPConnectionDroppedException();
		}catch(WrongPassingArgumentException e) {
			System.err.println("Server responded '0', it seems that you were trying recovery mode but the server is creating a new file. Try to start a new Interaction, passing 1 argument only, if you want that.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new RecoverConnectionException();
		}catch (IOException e) {
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new RecoverConnectionException();
		}
		return byteAlreadyWritten;
	}
	
	
	/*
	 * This function is used to send the file itself to the server,	
	 * independently from the kind of operation performed before (1st transfer or conn. recover)
	 * 
	 * Throws:
	 * 		ServerFailureException -- If the server answers '1' for any reason
	 * 		TCPConnectionDroppedException -- If the read over the socket stream returns '-1' or a TimeOut is reached. 
	 * 		TransferFileException -- If an exception is thrown internally to inform the caller.
	*/
	public void transferFile(int bytesAlreadyWritten, String filePath) throws TCPConnectionDroppedException, ServerFailureException, TransferFileException {
		try {
			File fileToWrite = FileOperationsManager.ClientFileOpener(filePath);
			
			int totalFileLength = (int)fileToWrite.length();
			int missingPartSize = totalFileLength - bytesAlreadyWritten;
			//System.out.println("Total is: " + totalFileLength + ", bytesAlreadyWritten are: " + bytesAlreadyWritten); //Used for debugging
			
			if(missingPartSize < 0) {
				throw new FileHasChangedException();
			}
			
			FileInputStream fin = new FileInputStream(fileToWrite);
			byte [] fileByteArray  = new byte [CHUNK_LENGTH];
			int count = 0;
			
			/*
			 * Sends to the server the size of the bytes to be written
			 * (IF 1st interaction, missingPartSize is equal to totalFileLength, since bytesAlreadyWritten = 0)
			*/
			this.outputSocketStream.writeInt(missingPartSize);
			
			//Used to skip the first N bytes in case the server already wrote something
			if(bytesAlreadyWritten >= 0)
			{
				fin.skip(bytesAlreadyWritten);
			}else {
				fin.close();
				throw new WrongBytesWrittenException();
			}
			System.out.println("The client is starting the file transfer.");
			
			// sending the File itself if missingPartSize > 0
			while ((count = fin.read(fileByteArray, 0, missingPartSize > CHUNK_LENGTH ? CHUNK_LENGTH : missingPartSize)) > 0) {
	        	  this.outputSocketStream.write(fileByteArray, 0, count);
	        	  missingPartSize -= count;
	        	  fileByteArray  = new byte [CHUNK_LENGTH];
	         }
	         this.outputSocketStream.flush();
	         fin.close();
	         
	         int transferConfirmation = (int) this.inputSocketStream.read();
	         
	         switch(transferConfirmation) {
	         	case 0:
	         		System.out.println("The client has finished transferring the file.");
	         		this.closeConnection();
	         		break;
	         	case 1:
	         		this.closeConnection();
	         		throw new ServerFailureException(ServerFailureException.KindOfFailure.DURING_TRANSFER);
	         	case -1:
	         		this.closeConnection();
	         		throw new TCPConnectionDroppedException();
	         }
	         
		}catch(SocketTimeoutException e) {
			System.err.println("It seems that the TIMEOUT has been reached.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);				
			}
			throw new TCPConnectionDroppedException();
		}catch (WrongBytesWrittenException e) {
			System.err.println("Server said that has written negative bytes, this impossible. Try to create a new file.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}			
			throw new TransferFileException();
		}catch (FileHasChangedException e) {
			System.err.println("You can only append data to previous file, you cannot overwrite or delete them.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}			
			throw new TransferFileException();
		}catch(FileNotFoundException e) {
			System.err.println("The file has not been found. Check the path correctness and try again.");
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new TransferFileException();
		}catch(IOException e) {
			try {
				this.closeConnection();
			} catch (IOException e1) {
				System.err.println("Unable to close connection. Aborting!");
				e1.printStackTrace();
				System.exit(1);
			}
			throw new TCPConnectionDroppedException(); //We can do like this since we are sure that other kinds of IOExceptions have been managed in other catches
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

		if (args.length > 2 || args.length == 0) {
			System.err.println("Check command line arguments (1 or 2): you must specify at least the file path. Optionally, you can specify a timestamp.");
			System.exit(1);
		}
		
		File file = new File(args[0]);
		if (!file.isAbsolute() || !file.exists()) {
		    System.err.println("Invalid directory path: " + args[0]);
		    System.exit(1);
		}

		//Check if the file ID is longer than 10 chars
		String path = args[0];
		String serverFileName = null;
		
		//Check if the time-stamp has been passed or not
		if(args.length == 2) {
			serverFileName = args[1];
		}
		
		try {
			FileTransfer client = new FileTransfer(
			        InetAddress.getByName("0.0.0.0"), 
			        2001);
			int startReadingFrom = 0;
			
			if(args.length == 1) {
				//Create NEW File on Server
				client.startNewInteraction(path);
			}else {
				//Recover connection with server and get size of byte sent
				startReadingFrom = client.recoverConnection(serverFileName);
			}
			
			/*
			 * Send file content to the server 
			 * (We are sure that is called only in case the previous
			 *  functions haven't got any problems) 
			*/
			client.transferFile(startReadingFrom, path);	      
		}catch(TCPConnectionDroppedException e) {
     		FileTransfer.printConnectionInterrupted();
     		System.exit(1);
		}catch (ServerFailureException e) { 
			switch(e.getKindOfFailure()) {
				case DURING_FIRST_INTERACTION:
					System.err.println("Server Responded '1', some error occured on the server, check the filename."); 
					break;
				case DURING_RECOVERY:
					System.err.println("Server Response '1', something went wrong in recovering connection, please check all parameters and retry.");
					break;
				case DURING_TRANSFER:
					System.err.println("Server Response '1', something went wrong during the transfer of the file, please check all parameters and retry with recovery mode.");
			}
			System.exit(1);
		}catch(StartNewInteractionException e) {
			System.err.println("Error in starting a new interaction with server.");
			System.exit(1);
		}catch(RecoverConnectionException e) {
			System.err.println("Error in recovering connection. Connection closed!");
			System.exit(1);
		}catch(TransferFileException e) {
			System.err.println("Error in transferring the file.");
			System.exit(1);
		}catch (ConnectException e) {
			System.err.println("Error when connecting to the server. Check that the address and the port number are valid. If so, then the Server is not running!");
			System.exit(1);
		}catch (Exception e) {
			System.err.println("Error when connecting to server");
			System.exit(1);
		}
		
	}

}
