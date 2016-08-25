package remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import activity.MainActivity;

class ViewPressRepeater extends Thread {

    private final String command;

    public ViewPressRepeater(String command){
        this.command = command;
    }

    public void run(){
        do{
            sendControl(command);
        }
        while (Remote.repeat);
    }

    private void sendControl(String command) {

        String uri = "http://" + MainActivity.myPhone.getComputerIP() + ":3999/control?control=" +
                command + "&name=" + MainActivity.myPhone.getPhoneName();

        try {
            URL url = new URL(uri);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.getResponseCode();
            connection.disconnect();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
