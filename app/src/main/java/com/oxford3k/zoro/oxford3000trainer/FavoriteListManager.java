package com.oxford3k.zoro.oxford3000trainer;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by Zoro on 04/11/2015.
 */
public class FavoriteListManager {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FAVORITE_KEY = "MyFavorite";

    private static FavoriteListManager instance = null;

    protected FavoriteListManager(){

    }

    public static FavoriteListManager GetInstance() {
        if (instance == null)
            instance = new FavoriteListManager();
        return instance;
    }

    public void Save(Context contex, ArrayList<String> listword) {

        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < listword.size(); i++) {
            strBuilder.append(listword.get(i));
            strBuilder.append(";");
        }

        SharedPreferences settings = contex.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(FAVORITE_KEY, strBuilder.toString());
        editor.commit();
    }

    public ArrayList<String> Load(Context context) {
        ArrayList<String> result = new ArrayList<String>();

        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);

        String strFavorite = settings.getString(FAVORITE_KEY, "");
        String[] items = strFavorite.split(";");
        for (int i = 0; i < items.length; i++) {
            result.add(items[i]);
        }

        return result;
    }

}
