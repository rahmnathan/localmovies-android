package networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ServerRequest {

    public List<String> requestTitles(Phone myPhone) {

        String restRequest = "http://" + myPhone.getComputerIP() + ":3999/titlerequest?path=" +
                myPhone.getPath().replace(" ", "%20");

        try {
            URL url = new URL(restRequest);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            List<String> list = Arrays.asList(br.readLine()
                    .replace("\"", "")
                    .replace("[", "")
                    .replace("]", "")
                    .split(","));

            connection.disconnect();

            return list;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void playMovie(Phone myPhone){

        String restRequest = "http://" + myPhone.getComputerIP() + ":3999/playmovie?path=" +
                myPhone.getPath().replace(" ", "%20") + "&phoneName=" + myPhone.getPhoneName() +
                "&computerIP=" + myPhone.getComputerIP() + "&chromeIP=" + myPhone.getCastIP();

        try {
            URL url = new URL(restRequest);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.getResponseCode();

            connection.disconnect();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void refresh(Phone myPhone){
        String restRequest = "http://" + myPhone.getComputerIP() + ":3999/refresh";

        try{
            URL url = new URL(restRequest);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.getResponseCode();
            connection.disconnect();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
