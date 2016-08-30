package activity;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.io.File;

import networking.ServerRequest;

public class ThreadManager extends Thread {

    private final String request;
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());
    private static final ServerRequest serverRequest = new ServerRequest();

    public ThreadManager(String request){
        this.request = request;
    }

    public void run(){
        switch(request){
            case "GetTitles":
                updateListView();
                break;
            case "PlayMovie":
                serverRequest.playMovie(MainActivity.myPhone);
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
