package activity;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.example.Phone;
import com.example.RestClient;

import java.io.File;

public class ThreadManager extends Thread {

    private final String request;
    private final String title;
    private final Phone phone;
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());
    private static final RestClient serverRequest = new RestClient();

    public ThreadManager(String request, String title){
        this.request = request;
        this.title = title;
        this.phone = MainActivity.myPhone;
    }

    public void run(){
        switch(request){
            case "GetTitles":
                MainActivity.myPhone.setPath(phone.getPath() + title + "/");
                System.out.println(MainActivity.myPhone.getPath());
                updateListView();
                break;
            case "PlayMovie":
                phone.setPath(phone.getPath() + title);
                serverRequest.playMovie(phone);
                break;
            case "Refresh":
                MainActivity.titles.invalidateAll();
                MainActivity.movieInfo.invalidateAll();
                serverRequest.refresh(MainActivity.myPhone);
                MainActivity.myPhone.setPath(MainActivity.myPhone.getMainPath() + "Movies/");

                File file = new File(Environment.getExternalStorageDirectory().toString() + "/LocalMovies/");
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
