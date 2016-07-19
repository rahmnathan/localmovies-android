package rahmnathan.localmovies;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import Phone.Phone;

/**
 * Created by Nathan on 7/10/2016.
 */
public class TriggerServer extends Thread {

    private Context con;
    private String path;

    public TriggerServer(String path, Context con){
        this.con = con;
        this.path = path;
    }

    public void run(){
        new Server().send(getPhoneInfo(path));
    }

    private Phone getPhoneInfo(String currentPath) {

        // Reading information from setup file and assigning it to variables

        currentPath = "initial" + currentPath + "Movies\\";

        File setupFile = new File(Environment.getExternalStorageDirectory(), "setup.txt");
        String[] sort = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(setupFile));
            sort = reader.readLine().split("splithere159");
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MainActivity.myPhone = new Phone(sort[0], sort[1], sort[2], currentPath);

        try{
            MainActivity.myPhone.setComputerIP(sort[3]);
        } catch (ArrayIndexOutOfBoundsException e){
            MainActivity.myPhone.setComputerIP(getServerIP());
        }

        return MainActivity.myPhone;
    }

    private String getServerIP() {

        MainActivity.runOnUI(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(con, "Scanning for server", Toast.LENGTH_SHORT).show();
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
                            public void run() {Toast.makeText(con, "Unable to find server", Toast.LENGTH_LONG).show();
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
