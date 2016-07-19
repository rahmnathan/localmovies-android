package rahmnathan.localmovies;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import Phone.Phone;

/**
 * Created by nathan on 3/7/16.
 */

public class Server {

    public void receive() {
        try {
            // Checking for title list from server

            ServerSocket serverSocket = new ServerSocket(3998);
            Socket socket = serverSocket.accept();
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            MainActivity.titles = (ArrayList<String>) objectInputStream.readObject();
            objectInputStream.close();
            socket.close();
            serverSocket.close();

            MainActivity.runOnUI(new Runnable() {
                @Override
                public void run() {
                    MainActivity.ad.clear();
                    MainActivity.ad.addAll(MainActivity.titles);
                    MainActivity.ad.notifyDataSetChanged();
                }
            });
        }  catch(Exception e){
            e.printStackTrace();
        }
    }

    public void send(Phone myPhone){

        // Sending command int to server

        int portNum = 3998;
        String path = myPhone.getPath();

        if(path.startsWith("initial")){
            portNum = 3999;
            myPhone.setPath(path.substring(7));
        }

        try {
            Socket socket = new Socket(MainActivity.myPhone.getComputerIP(), portNum);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(myPhone);

            objectOutputStream.close();
            socket.close();
            if(!myPhone.isCasting()){

                // If we're viewing Series and not episodes we receive new list and update listview

                receive();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}