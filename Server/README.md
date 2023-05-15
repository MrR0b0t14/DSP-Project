# Server

This folder contains the code of the server, all the files at it associated and also the folder for the exceptions.

## Implementation Choices
In this section are reported the implementation choices for a better understanding of the output. To run only the server, skip to the next section.
Some messages on the server side have been provided on the standard output, to improve the understandability of what the server is doing during the transfer operation (even though they were not required by the project track).

### Added Timestamp
The **Timestamp** precision has been set to milliseconds so that the server can create up to 1K file per second with the same name (concatenation fileIDTimestamp may have collisions).

### Edge Cases
Many problems have been considered but since the messages exchanged in the protocol are fixed, some assumptions must be done:
  - If the client will send a valid **ID** (instead of idTimestamp), while trying recovery mode, the server will not know that and will create a new file sending back **_'0'_** to the client, while it is expecting _**'2'**_ (this has been managed on the client, but a malicious one could still break this).
  - If the client will add data to the previous file, even if the file was previously completely stored, by contacting the server in recovery mode it is possible to append these new data. At the same time, overwriting of previous bytes on the client, will be discarded by the server, it is able only to append data. Moreover, if the file on the client is smaller than before (so than the one stored on the server), the server will send back _**'1'**_.
  - If a client is able to retrieve someonelse's _idTimestamp_ and the length of the file stored on the server, it is able to add data to that file by using recovery mode, this could be avoided by adding (not required and not provided by me) authentication mechanism.

Many others assumptions have been done, but they may have not been reported in this README, so that can be discussed at the oral.

## How To Run
This must be started first than the client.
You have to put yourself in the "_src_" folder inside the "_Server_" one. So from the Server folder run:

    cd src

Then, you must compile server files so run:

    javac it/polito/dsp/*.java

And to start it run:

    java it.polito.dsp.WritingServer

**Note:** Once that you start also the client and transfer the file, a folder will be created one step up to the current directory, since you are running in _src_ folder, it should be directly in the Server folder. The folder containing the files is named "_**fileTransferDSP**_". To check that the files have been correctly transferred, please manually add the extension to the file to be opened (consider that if by any chance you'll modify the fileName and then for some reasons you want to try recovery mode on the client, the two names must be compliant).
