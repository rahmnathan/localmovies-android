package networking;

import android.os.Handler;
import android.os.Looper;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import activity.MainActivity;

class ServerInput {

    private static final Handler UIHandler = new Handler(Looper.getMainLooper());

    public void receive() {
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