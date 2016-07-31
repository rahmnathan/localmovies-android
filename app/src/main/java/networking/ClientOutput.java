package networking;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientOutput {

    private final ServerInput serverInput = new ServerInput();

    public void send(Phone myPhone){

        // Sending phone info to server

        int portNum = 3998;

        try {
            Socket socket = new Socket(myPhone.getComputerIP(), portNum);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(myPhone);

            socket.close();
            if(!myPhone.isCasting()){

                // If we're not playing a movie we wait to receive the new list

                serverInput.receive();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
