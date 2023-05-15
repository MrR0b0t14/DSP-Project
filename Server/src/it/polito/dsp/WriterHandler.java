package it.polito.dsp;

import java.io.*;																																																										
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDateTime;

import it.polito.dsp.ServerExceptions.InvalidFileIdException;
import it.polito.dsp.ServerExceptions.InvalidFileNameException;
import it.polito.dsp.ServerExceptions.WrongSizeReceivedException;

public class WriterHandler implements Runnable{

	private static final int CHUNK_LENGTH = 1024;
	private static final int TIMEOUT = 30*1000;
	private static final int TIMESTAMP_LEN = 20;
	private static final int MAX_ID_LEN = 10;
	private static final int MAX_FILENAME_LEN = MAX_ID_LEN + TIMESTAMP_LEN + 1;
	
	Socket socket = null;
	DataInputStream inputSocketStream = null;
    DataOutputStream outputSocketStream = null;
    boolean success = true;
    String fileName = null;
    int fileLength;
    
	public WriterHandler(Socket socket) {
		this.socket = socket;
		try {
			this.socket.setSoTimeout(TIMEOUT);
			this.inputSocketStream = new DataInputStream(socket.getInputStream());
			this.outputSocketStream = new DataOutputStream(socket.getOutputStream());
		} catch (SocketException e) {
			System.err.println("Error in setting the timeout for the socket.");
			success = false;
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error in accessing the socket I/O streams.");
			success = false;
			e.printStackTrace();
		}
	}

	private void closeConnection() throws IOException {
		this.socket.close();
	}
	
	@Override
	public void run() {
		if(success) {
			// read the metadata from the client
			try {
				//read the original type of the file
				byte[] fileIdArray = new byte[MAX_FILENAME_LEN];
				int bytesRead = 0;
        	    int readByte;
        	    
				do {
					readByte = this.inputSocketStream.read();
					if (readByte == -1) {
        	            throw new IOException("Socket closed before complete message could be read.");
        	        }
					fileIdArray[bytesRead++] = (byte) readByte;
				} while(bytesRead < MAX_FILENAME_LEN && readByte != 0);
				
				if(bytesRead < 2 || (fileIdArray[bytesRead-1] != 0)) {
					throw new InvalidFileIdException(); 
				}
				
				fileName = new String(fileIdArray, StandardCharsets.US_ASCII);
				fileName = fileName.trim(); //Remove the '\0'
				
				byte[] fileNameByteArray = new byte[MAX_FILENAME_LEN];
				File fp = null;
				
				/*
				 * This checks if the fileName exceeds 10 chars (excluding the '\0')
				 * In that case there are 2 scenarios: the ID is wrong, 
				 * or the received filename is made up by ID+Timestamp, in this second case
				 * we should check that it is a valid combination (only a specific timestamp format)
				*/
				if(fileName.length() > 10) {
					//Check fileName format
					boolean validFileName = fileName.matches("[a-zA-Z0-9]{1,10}2[0-9]{3}-[0-9]{2}-[0-9]{2}T[0-9]{9}");

					if(validFileName) {
						//Check if the file exists
						fp = FileHelper.fileOpener(fileName);
						this.outputSocketStream.writeByte(2);
						this.outputSocketStream.writeInt((int) fp.length());
					}else {
						throw new InvalidFileNameException();
					}
				}else {
					fileName = fileName.substring(0, Math.min(bytesRead-1, 10));
					fileName = fileName.concat(LocalDateTime.now().toString().replaceAll("[:.]", "").substring(0, 20));
					fp = FileHelper.fileCreate(fileName);
					fileNameByteArray = fileName.concat("\0").getBytes(StandardCharsets.US_ASCII);
					this.outputSocketStream.writeByte(0);
					this.outputSocketStream.write(fileNameByteArray);
				}
								
				//Receiving file from client
				int bytesToRead = this.inputSocketStream.readInt();
				
				if(bytesToRead > 0) {
					byte[] fileArray = new byte[CHUNK_LENGTH];
					FileOutputStream fileToWrite = new FileOutputStream(fp, true);
					System.out.println("Client transferring data.");
					
					while(bytesToRead > 0) {
						int readBytes = this.inputSocketStream.read(fileArray, 0, Math.min(CHUNK_LENGTH, bytesToRead));
						fileToWrite.write(fileArray, 0, readBytes);
						bytesToRead -= readBytes;
					}
					
					fileToWrite.close();
					
					System.out.println("Client Finished!");
				}else if(bytesToRead == 0) {
					System.out.println("File already transferred entirely.");
				}else {
					this.closeConnection();
					throw new WrongSizeReceivedException();
				}
			} catch (SocketException e) {
				success = false;
				System.err.println("Timeout expired for reading the medatadata.");
				e.printStackTrace();
			}  catch (IOException e) {
				success = false;
				System.err.println("Error in receiving the metadata. Client probably dropped connection!");
			} catch (InvalidFileIdException e) {
				success = false;
				System.err.println("Client is allowed to send an ID up to 10 characters. Connection Closed!");
			} catch (InvalidFileNameException e) {
				success = false;
				System.err.println("Invalid File Name, client must provide a name in the format \"idTimestamp\". Connection closed!");
			}catch(WrongSizeReceivedException e) {
				success = false;
				System.err.println("Received a wrong size for the file to write. Connection Closed!");
			}catch(Exception e) {
				success = false;
				System.err.println("Generic error!");
				e.printStackTrace();
			}
		}
		
		if(!success) {
			try {
				this.outputSocketStream.writeByte(1);
				this.closeConnection();
			} catch(SocketException e) {
				System.err.println("Unable to send back response. Connection has been dropped!");
			}catch (IOException e) {
				System.err.println("Unable to close connection. Aborting!");
				e.printStackTrace();
			}
		}else {
			/*
			 * Server sends to the client the confirmation that 
			 * the file has been totally transferred.
			 * Then it closes the connection Gracefully.
			*/
			try {
				this.outputSocketStream.writeByte(0);
				this.closeConnection();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
