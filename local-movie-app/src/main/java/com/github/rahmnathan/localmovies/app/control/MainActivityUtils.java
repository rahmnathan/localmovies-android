package com.github.rahmnathan.localmovies.app.control;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;
import android.widget.GridView;

import com.github.rahmnathan.localmovies.app.activity.SetupActivity;
import com.github.rahmnathan.localmovies.app.adapter.list.MovieListAdapter;
import com.github.rahmnathan.localmovies.app.data.MovieGenre;
import com.github.rahmnathan.localmovies.app.data.MovieOrder;
import com.github.rahmnathan.localmovies.app.persistence.MovieHistory;
import com.github.rahmnathan.localmovies.app.data.Client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import rahmnathan.localmovies.R;

public class MainActivityUtils {

    public static void sortVideoList(MenuItem item, MovieListAdapter listAdapter, GridView gridView, Context context, Client client, MovieHistory history){
        switch (item.getItemId()) {
            case R.id.action_settings:
                context.startActivity(new Intent(context, SetupActivity.class));
                break;
            case R.id.action_history:
                client.resetCurrentPath();
                client.appendToCurrentPath("Movies");
                listAdapter.display(history.getHistoryList());
                break;
            case R.id.order_date_added:
                sort(MovieOrder.DATE_ADDED, listAdapter, gridView);
                break;
            case R.id.order_views:
                sort(MovieOrder.MOST_VIEWS, listAdapter, gridView);
                break;
            case R.id.order_year:
                sort(MovieOrder.RELEASE_YEAR, listAdapter, gridView);
                break;
            case R.id.order_rating:
                sort(MovieOrder.RATING, listAdapter, gridView);
                break;
            case R.id.order_title:
                sort(MovieOrder.TITLE, listAdapter, gridView);
                break;
            case R.id.genre_comedy:
                filterGenre(MovieGenre.COMEDY, listAdapter, gridView);
                break;
            case R.id.action_action:
                filterGenre(MovieGenre.ACTION, listAdapter, gridView);
                break;
            case R.id.genre_sciFi:
                filterGenre(MovieGenre.SCIFI, listAdapter, gridView);
                break;
            case R.id.genre_horror:
                filterGenre(MovieGenre.HORROR, listAdapter, gridView);
                break;
            case R.id.genre_thriller:
                filterGenre(MovieGenre.THRILLER, listAdapter, gridView);
                break;
            case R.id.genre_fantasy:
                filterGenre(MovieGenre.FANTASY, listAdapter, gridView);
                break;
        }
    }

    public static Client getPhoneInfo(InputStream inputStream) {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            Client client = (Client) objectInputStream.readObject();
            client.resetCurrentPath();
            return client;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }

    private static void sort(MovieOrder order, MovieListAdapter listAdapter, GridView gridView){
        listAdapter.sort(order);
        gridView.smoothScrollToPosition(0);
    }

    private static void filterGenre(MovieGenre genre, MovieListAdapter listAdapter, GridView gridView){
        listAdapter.filterGenre(genre);
        gridView.smoothScrollToPosition(0);
    }
}
