package rahmnathan.localmovies;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import Phone.Phone;

/**
 * Created by Nathan on 7/10/2016.
 */
public class TriggerServer extends Thread {

    private Context context;
    private String path;

    public TriggerServer(String path, Context context){
        this.context = context;
        this.path = path;
    }

    public void run(){
        new Server().send(getPhoneInfo(path));
    }

    private Phone getPhoneInfo(String currentPath) {

        File setupFile = new File(Environment.getExternalStorageDirectory(), "setup.txt");
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(setupFile));
            MainActivity.myPhone = (Phone) objectInputStream.readObject();
            objectInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        MainActivity.myPhone.setPath("initial" + currentPath + "Movies/");

        if(MainActivity.myPhone.getComputerIP().equals(""))
            MainActivity.myPhone.setComputerIP(getServerIP());

        return MainActivity.myPhone;
    }

    private String getServerIP() {

        MainActivity.runOnUI(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Scanning for server", Toast.LENGTH_SHORT).show();
            }
        });

        String[] IPRangeArray = MainActivity.myPhone.getPhoneIP().split("\\.");
        String IPRange = IPRangeArray[0] + "." + IPRangeArray[1] + "." + IPRangeArray[2] + ".";
        String address = null;

        int i = 0;
        while (i < 257) {
            try {
                if(i == 256){
                    MainActivity.runOnUI(new Runnable() {
                            @Override
                            public void run() {Toast.makeText(context, "Unable to find server", Toast.LENGTH_LONG).show();
                            }
                    });
                    break;
                }
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(IPRange + i, 3999), 110);
                socket.close();
                address = IPRange + i;
                break;
            } catch (Exception e) {
                i++;
            }
        }
        return address;
    }
}
