package it.polito.dsp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

public class FileHelper {

	static File fileCreate(String fileName) throws IOException{
		// Create a File object for the file you want to create/open
		// Create the directory if it doesn't exist
		File directory = new File("../fileTransferDSP");
		directory.mkdirs();
        File fp = new File(directory.getPath() + File.separator + fileName); 

        // Check if the file already exists, and create it if it doesn't
        if (!fp.exists()) {
            fp.createNewFile();
        }else {
        	throw new FileAlreadyExistsException(fileName);
        }
        
        return fp;
	}
	
	static File fileOpener(String fileName) throws IOException{
		// Create a File object for the file you want to create/open
		// Create the directory if it doesn't exist
		File directory = new File("../fileTransferDSP");
		
        File fp = new File(directory.getPath() + File.separator + fileName);

        // Check if the file already exists, and throws an exception if it doesn't
        if (!fp.exists()) {
            throw new FileNotFoundException();
        }
        
        return fp;
	}
}
