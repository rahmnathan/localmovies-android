package activity;

import android.os.Handler;
import android.os.Looper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.List;
import java.util.concurrent.ExecutionException;

import networking.ServerRequest;

public class ThreadManager extends Thread {

    private final String request;
    private static final Handler UIHandler = new Handler(Looper.getMainLooper());
    public static final ServerRequest serverRequest = new ServerRequest();

    public ThreadManager(String request){
        this.request = request;
    }

    public static final LoadingCache<String, List<String>> titles =
            CacheBuilder.newBuilder()
                    .maximumSize(100)
                    .build(
                            new CacheLoader<String, List<String>>() {

                                @Override
                                public List<String> load(String currentPath) {
                                    return serverRequest.requestTitles(MainActivity.myPhone);
                                }
                            });


    public void run(){
        switch(request){
            case "GetTitles":
                try {
                    updateListView(titles.get(MainActivity.myPhone.getPath()));
                } catch (ExecutionException e){
                    e.printStackTrace();
                }
                break;
            case "PlayMovie":
                serverRequest.playMovie(MainActivity.myPhone);
                break;
            case "Refresh":
                titles.invalidateAll();
                serverRequest.refresh(MainActivity.myPhone);
                MainActivity.myPhone.setPath(MainActivity.myPhone.getMainPath() + "Movies/");
                updateListView(serverRequest.requestTitles(MainActivity.myPhone));
                break;
        }
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
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
}
