package activity;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.ExecutionException;

import movieinfo.ImageDownloader;
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
                try {
                    updateListView(MainActivity.titles.get(MainActivity.myPhone.getPath()));
                } catch (ExecutionException e){
                    e.printStackTrace();
                }
                break;
            case "PlayMovie":
                serverRequest.playMovie(MainActivity.myPhone);
                break;
            case "Refresh":
                MainActivity.titles.invalidateAll();
                serverRequest.refresh(MainActivity.myPhone);
                MainActivity.myPhone.setPath(MainActivity.myPhone.getMainPath() + "Movies/");
                updateListView(serverRequest.requestTitles(MainActivity.myPhone));
                break;
        }
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
    }

    private void updateListView(final List<String> titleList1){
        runOnUI(new Runnable() {
            @Override
            public void run() {
                MainActivity.movieList.clear();
                try {
                    MainActivity.movieList.addAll(MainActivity.movieInfo.get(MainActivity.myPhone.getPath()));
                } catch (Exception e){
                    e.printStackTrace();
                }
                MainActivity.myAdapter.notifyDataSetChanged();
            }
        });
    }
}
