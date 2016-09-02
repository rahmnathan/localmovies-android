package appmain;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.phoneinfo.Phone;
import com.restclient.RestClient;

import java.io.File;

public class ThreadManager extends Thread {

    private final String request;
    private final String title;
    private final Phone phone;
    private final Context context;
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());
    private static final RestClient REST_CLIENT = new RestClient();

    public ThreadManager(String request, String title, Context context){
        this.request = request;
        this.title = title;
        this.phone = MainActivity.myPhone;
        this.context = context;
    }

    public void run(){
        switch(request){
            case "GetTitles":
                MainActivity.myPhone.setPath(phone.getPath() + title + "/");
                updateListView();
                break;
            case "PlayMovie":
                phone.setPath(phone.getPath() + title);
                REST_CLIENT.playMovie(phone);
                break;
            case "Refresh":
                MainActivity.movieInfo.invalidateAll();
                REST_CLIENT.refresh(phone);
                MainActivity.myPhone.setPath(MainActivity.myPhone.getMainPath() + "Movies/");

                File file = new File(context.getFilesDir() + "/LocalMovies/");
                for(File x : file.listFiles()){
                    x.delete();
                }

                updateListView();
                break;
        }
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    private void updateListView() {

        runOnUI(new Runnable() {
            @Override
            public void run() {
                MainActivity.progressBar.setVisibility(View.VISIBLE);
            }
        });

        MainActivity.movieList.clear();
        try {
            MainActivity.movieList.addAll(MainActivity.movieInfo.get(MainActivity.myPhone.getPath()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        runOnUI(new Runnable() {
            @Override
            public void run() {
                MainActivity.progressBar.setVisibility(View.GONE);
                MainActivity.myAdapter.notifyDataSetChanged();
            }
        });
    }
}
