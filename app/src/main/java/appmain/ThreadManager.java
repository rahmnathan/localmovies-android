package appmain;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.phoneinfo.Phone;
import com.restclient.RestClient;

public class ThreadManager extends Thread {

    private final String request;
    private final String title;
    private final Phone phone;
    public static final Handler UIHandler = new Handler(Looper.getMainLooper());
    private static final RestClient REST_CLIENT = new RestClient();

    public ThreadManager(String request, String title){
        this.request = request;
        this.title = title;
        this.phone = MainActivity.myPhone;
    }

    public void run(){
        switch(request){
            case "GetTitles":
                MainActivity.myPhone.setCurrentPath(phone.getCurrentPath() + title + "/");
                updateListView();
                break;
            case "PlayMovie":
                phone.setVideoPath(phone.getCurrentPath() + title);
                REST_CLIENT.playMovie(phone);
                break;
            case "Refresh":
                MainActivity.movieInfo.invalidateAll();
                REST_CLIENT.refresh(phone);
                MainActivity.myPhone.setCurrentPath(MainActivity.myPhone.getMainPath() + "Movies/");
                break;
        }
    }

    private void updateListView() {

        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.progressBar.setVisibility(View.VISIBLE);
            }
        });

        MainActivity.MOVIE_INFO_LIST.clear();
        try {
            MainActivity.MOVIE_INFO_LIST.addAll(MainActivity.movieInfo.get(MainActivity.myPhone.getCurrentPath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                MainActivity.progressBar.setVisibility(View.GONE);
                MainActivity.myAdapter.notifyDataSetChanged();
            }
        });
    }
}
