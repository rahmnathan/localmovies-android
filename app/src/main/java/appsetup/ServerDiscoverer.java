package appsetup;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.phoneinfo.Phone;

import java.net.InetSocketAddress;
import java.net.Socket;

import appmain.MainActivity;
import appmain.ThreadManager;

public class ServerDiscoverer extends Thread {

    private final Context context;
    private final Phone myPhone;

    public ServerDiscoverer(Phone myPhone, Context context){
        this.myPhone = myPhone;
        this.context = context;
    }

    public void run(){

        if(myPhone.getComputerIP().equals("")) {
            MainActivity.myPhone.setComputerIP(getServerIP());
        }

        ThreadManager.runOnUI(new Runnable() {
            @Override
            public void run() {
                MainActivity.progressBar.setVisibility(View.GONE);
                Toast.makeText(context, "Downloading Movie Info", Toast.LENGTH_SHORT).show();
            }
        });

        new ThreadManager("GetTitles", "Movies", context).start();
    }

    private String getServerIP() {

        ThreadManager.runOnUI(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Scanning for server", Toast.LENGTH_SHORT).show();
                ThreadManager.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.progressBar.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        String[] IPRangeArray = myPhone.getCastIP().split("\\.");
        String IPRange = IPRangeArray[0] + "." + IPRangeArray[1] + "." + IPRangeArray[2] + ".";
        String address = null;

        int i = 0;
        while (i < 257) {
            try {
                if(i == 256){
                    ThreadManager.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            ThreadManager.runOnUI(new Runnable() {
                                @Override
                                public void run() {
                                    MainActivity.progressBar.setVisibility(View.GONE);
                                }
                            });
                            Toast.makeText(context, "Unable to find server", Toast.LENGTH_LONG).show();
                        }
                    });
                    break;
                }
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(IPRange + i, 3990), 250);
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