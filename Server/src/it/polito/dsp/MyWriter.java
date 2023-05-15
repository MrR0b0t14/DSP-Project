package it.polito.dsp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyWriter {
	
    private ServerSocket serverSocket = null;
    ExecutorService threadPool = null;

    public MyWriter(int portNumber) throws IOException {
        serverSocket = new ServerSocket(portNumber);
    }

    public void execute() throws IOException {
        threadPool = Executors.newCachedThreadPool();
        while (true) {
        	Socket socket = serverSocket.accept();
            threadPool.submit(new WriterHandler(socket));
        }
        
    }

    public void stop() throws IOException {
    	threadPool.shutdown();
        serverSocket.close();
    }


}