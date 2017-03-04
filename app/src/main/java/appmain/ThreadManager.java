package appmain;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;

import com.google.common.cache.LoadingCache;
import com.phoneinfo.Phone;
import com.rahmnathan.MovieInfo;
import com.restclient.RestClient;

import java.util.Collections;
import java.util.List;

class ThreadManager extends Thread {

    enum Task {
        TITLE_REQUEST, TOKEN_REFRESH
    }

    private final String title;
    private final Phone phone;
    private List<MovieInfo> movieInfoList;
    private ProgressBar progressBar;
    private MovieListAdapter movieListAdapter;
    private LoadingCache<String, List<MovieInfo>> movieInfo;
    private final Task task;
    private final RestClient restClient = new RestClient();

    private final Handler UIHandler = new Handler(Looper.getMainLooper());

    ThreadManager(String title, ProgressBar progressBar, MovieListAdapter movieListAdapter,
                  Phone myPhone, List<MovieInfo> movieInfoList, Task task,
                  LoadingCache<String, List<MovieInfo>> movieInfo){
        this.task = task;
        this.movieInfo = movieInfo;
        this.title = title;
        this.phone = myPhone;
        this.movieListAdapter = movieListAdapter;
        this.progressBar = progressBar;
        this.movieInfoList = movieInfoList;
    }

    public void run() {
        switch (task){
            case TITLE_REQUEST:
                phone.setCurrentPath(phone.getCurrentPath() + title + "/");
                updateListView();
                break;
            case TOKEN_REFRESH:
                restClient.refreshKey(phone);
                break;
        }
    }

    private void updateListView() {
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });

        try {
            List<MovieInfo> infoList = movieInfo.get(phone.getCurrentPath());
            Collections.sort(infoList, MovieInfo.Builder.newInstance().build());
            movieInfoList.clear();
            movieInfoList.addAll(infoList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                movieListAdapter.notifyDataSetChanged();
            }
        });
    }
}
