package networking;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import activity.MainActivity;

public class ServerRequest {

    private static final Handler UIHandler = new Handler(Looper.getMainLooper());

    public void send(Phone myPhone) {

        // Sending phone info to server

        String restRequest = "http://" + myPhone.getComputerIP() + ":8080/titlerequest?path=" + myPhone.getPath() +
                "&phoneName=" + myPhone.getPhoneName() + "&phoneIP=" + myPhone.getPhoneIP() +
                "&computerIP=" + myPhone.getComputerIP() + "&chromeIP=" + myPhone.getCastIP() +
                "&casting=" + myPhone.isCasting();

        try {
            URL url = new URL(restRequest);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if (!myPhone.isCasting()) {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                List<String> list = Arrays.asList(br.readLine()
                        .replace("\"", "")
                        .replace("[", "")
                        .replace("]", "")
                        .split(","));

                updateListView(list);
            }
            connection.disconnect();

        } catch (Exception e) {
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

    private static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }
}
