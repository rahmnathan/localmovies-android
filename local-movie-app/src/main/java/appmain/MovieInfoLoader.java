package appmain;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.localmovies.client.Client;
import com.localmovies.provider.boundary.MovieInfoFacade;
import com.localmovies.provider.data.MovieInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class MovieInfoLoader implements Runnable {

    private final Client client;
    private final List<MovieInfo> movieInfoList;
    private final ProgressBar progressBar;
    private final MovieListAdapter movieListAdapter;
    private final ConcurrentMap<String, List<MovieInfo>> movieInfoCache;
    private final MovieInfoFacade movieInfoFacade = new MovieInfoFacade();
    private final Logger logger = Logger.getLogger("MovieInfoLoader");
    private final Handler UIHandler = new Handler(Looper.getMainLooper());
    private final Context context;

    MovieInfoLoader(ProgressBar progressBar, MovieListAdapter movieListAdapter, Client myClient,
                    List<MovieInfo> movieInfoList, ConcurrentMap<String, List<MovieInfo>> movieInfoCache,
                    Context context){
        this.movieInfoCache = movieInfoCache;
        this.client = myClient;
        this.movieListAdapter = movieListAdapter;
        this.progressBar = progressBar;
        this.movieInfoList = movieInfoList;
        this.context = context;
    }

    public void run() {
        logger.log(Level.INFO, "Dynamically loading titles");
        if(client.getAccessToken() == null){
            UIHandler.post(() -> Toast.makeText(context, "Login failed - Check credentials", Toast.LENGTH_LONG).show());
            return;
        }
        int itemsPerPage = 30;
        UIHandler.post(()-> progressBar.setVisibility(View.VISIBLE));
        movieInfoList.clear();
        try {
            List<MovieInfo> movieInfos = new ArrayList<>();
            int i = 0;
            do{
                List<MovieInfo> infoList = movieInfoFacade.getMovieInfo(client, i, itemsPerPage);
                movieInfoList.addAll(infoList);
                movieInfos.addAll(infoList);
                UIHandler.post(movieListAdapter::notifyDataSetChanged);
                i++;
            } while (i <= (client.getMovieCount() / itemsPerPage));

            UIHandler.post(()-> progressBar.setVisibility(View.GONE));
            movieInfoCache.putIfAbsent(client.getCurrentPath().toString(), movieInfos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}