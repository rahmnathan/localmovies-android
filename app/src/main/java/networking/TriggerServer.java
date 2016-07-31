package networking;

import android.content.Context;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Socket;

import activity.MainActivity;

public class TriggerServer extends Thread {

    private final Context context;
    private final Phone myPhone;

    public TriggerServer(Phone myPhone, Context context){
        this.myPhone = myPhone;
        this.context = context;
    }

    public void run(){

        if(MainActivity.myPhone.getComputerIP().equals(""))
            MainActivity.myPhone.setComputerIP(getServerIP());

        new ClientOutput().send(myPhone);
    }

    private String getServerIP() {

        ServerInput.runOnUI(new Runnable() {
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
                    ServerInput.runOnUI(new Runnable() {
                        @Override
                        public void run() {Toast.makeText(context, "Unable to find server", Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(IPRange + i, 3998), 110);
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
