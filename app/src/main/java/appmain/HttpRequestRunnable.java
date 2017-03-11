package appmain;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;

import com.phoneinfo.Phone;
import com.rahmnathan.MovieInfo;
import com.restclient.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

class HttpRequestRunnable implements Runnable {

    enum Task {
        TITLE_REQUEST, TOKEN_REFRESH
    }

    private final Phone phone;
    private List<MovieInfo> movieInfoList;
    private ProgressBar progressBar;
    private MovieListAdapter movieListAdapter;
    private ConcurrentMap<String, List<MovieInfo>> movieInfoCache;
    private final Task task;
    private final RestClient restClient = new RestClient();

    private final Handler UIHandler = new Handler(Looper.getMainLooper());

    HttpRequestRunnable(ProgressBar progressBar, MovieListAdapter movieListAdapter, Phone myPhone,
                        List<MovieInfo> movieInfoList, Task task, ConcurrentMap<String, List<MovieInfo>> movieInfoCache){
        this.task = task;
        this.movieInfoCache = movieInfoCache;
        this.phone = myPhone;
        this.movieListAdapter = movieListAdapter;
        this.progressBar = progressBar;
        this.movieInfoList = movieInfoList;
    }

    public void run() {
        switch (task){
            case TITLE_REQUEST:
                dynamicallyLoadTitles();
                break;
            case TOKEN_REFRESH:
                restClient.refreshKey(phone);
                break;
        }
    }

    private void dynamicallyLoadTitles() {
        int itemsPerPage = 30;
        UIHandler.post(() -> progressBar.setVisibility(View.VISIBLE));
        try {
            if (movieInfoList.size() != 0)
                movieInfoList.clear();

            List<MovieInfo> movieInfos = new ArrayList<>();
            int i = 0;
            do{
                List<MovieInfo> infoList = restClient.getMovieInfo(phone, i, itemsPerPage);
                movieInfoList.addAll(infoList);
                movieInfos.addAll(infoList);
                UIHandler.post(() -> movieListAdapter.notifyDataSetChanged());
                i++;
            } while (i <= (phone.getMovieCount() / itemsPerPage));

            UIHandler.post(() -> progressBar.setVisibility(View.GONE));
            movieInfoCache.putIfAbsent(phone.getCurrentPath(), movieInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
