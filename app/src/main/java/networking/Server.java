package networking;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import activity.MainActivity;

public class Server {

    private static final Handler UIHandler = new Handler(Looper.getMainLooper());

    private void receive() {
        try {
            // Checking for title list from server

            ServerSocket serverSocket = new ServerSocket(3998);
            Socket socket = serverSocket.accept();
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            updateListView((ArrayList<String>) objectInputStream.readObject());

            socket.close();
            serverSocket.close();

        }  catch(Exception e){
            e.printStackTrace();
        }
    }

    public void send(Phone myPhone){

        // Sending phone info to server

        int portNum;
        String currentPath = myPhone.getPath();

        if(!currentPath.startsWith("initial")){
            portNum = 3998;
        } else{
            portNum = 3999;
            myPhone.setPath(currentPath.substring(7));
        }

        try {
            Socket socket = new Socket(myPhone.getComputerIP(), portNum);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(myPhone);

            socket.close();
            if(!myPhone.isCasting()){

                // If we're not playing a movie we wait to receive the new list

                receive();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void updateListView(final List<String> titleList){
        runOnUI(new Runnable() {
            @Override
            public void run() {
                MainActivity.ad.clear();
                MainActivity.ad.addAll(titleList);
                MainActivity.ad.notifyDataSetChanged();
            }
        });
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
}