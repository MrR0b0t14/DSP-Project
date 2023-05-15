package it.polito.dsp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/*
 *	This class is used to help the client in the management of files
 *	and in all operations to them related.
 *	(Generation of ID, Opening of the file)
*/
public class FileOperationsManager {

	/* 
	 * Digest 10 ASCII char of the received file.
	 * This feature can be used to manage also the
	 * case in which the file changes before being
	 * totally transferred and the connection drops.
	 * 
	 * digestEntireFile true: stands for managing it safely (hash of the entire file)
	 * digestEntireFile false: hash only of the path, does not detect changes in the file.
	*/
	public static String pathToId(String filePath, boolean digestEntireFile) throws IOException, NoSuchAlgorithmException{		
	    byte[] hashBytes;
	    //First checks that the file exists to avoid creating a file on the server uselessly
	    if(FileOperationsManager.ClientFileOpener(filePath) != null) {
		    if(digestEntireFile) {
		        byte[] buffer = new byte[8192]; // buffer size for reading the file
		        MessageDigest sha256 = MessageDigest.getInstance("SHA-256"); // instantiate SHA-256 hashing algorithm
		        FileInputStream fis = new FileInputStream(filePath);
		        int bytesRead;
		        while ((bytesRead = fis.read(buffer)) != -1) {
		            sha256.update(buffer, 0, bytesRead); // update the hash with the next chunk of data
		        }
		        fis.close();
		        hashBytes = sha256.digest(); // obtain the final hash value
		    }else {
		        MessageDigest sha256 = MessageDigest.getInstance("SHA-256"); // instantiate SHA-256 hashing algorithm
		        hashBytes = sha256.digest(filePath.getBytes(StandardCharsets.US_ASCII)); // compute the hash of the filename as a byte array    
		    }
	
		    StringBuilder sb = new StringBuilder();
		    for (byte b : hashBytes) {
		        if (sb.length() >= 10) {
		            break; // stop appending characters if the string is already 10 characters long
		        }
	            int toAppend = (b + 255) % 62;
	            if(toAppend >= 0 && toAppend <= 9) {
	                sb.append((char)((int)'0' + toAppend));
	            }else if(toAppend >= 10 && toAppend <= 35){
	            	sb.append((char)((int)'A' + toAppend - 10));
	            } else {
	                sb.append((char)((int)'a' + toAppend - 36)); 
	            }
		    }
		    sb.append('\0');
		    return sb.toString();
	    }else {
	    	throw new FileNotFoundException();
	    }
	}

	static File ClientFileOpener(String path) throws FileNotFoundException{
		
        File fp = new File(path);

        // Check if the file already exists, and throws an exception if it doesn't
        if (!fp.exists()) {
            throw new FileNotFoundException();
        }
        
        return fp;
	}
}
