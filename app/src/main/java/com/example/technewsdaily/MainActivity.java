package com.example.technewsdaily;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    SQLiteDatabase articlesDB;

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content=new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        articlesDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER,articleTitle VARCHAR,articleContent VARCHAR)");



        DownloadTask task = new DownloadTask();
        String html;

        try
        {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        }
        catch (Exception e)
        {
            e.printStackTrace();

        }

        ListView listView = findViewById(R.id.listView);
        arrayAdapter= new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);
        updateListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent= new Intent(getApplicationContext(),News.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });

    }
   public void updateListView()
    {
        Cursor c= articlesDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex=c.getColumnIndex("articleContent");
        int titleIndex=c.getColumnIndex("articleTitle");
        if(c.moveToFirst())
        {
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            }while(c.moveToNext());
        }
        arrayAdapter.notifyDataSetChanged();


    }

    public class DownloadTask extends AsyncTask<String,Void,String>
    {

        @Override
        protected String doInBackground(String... urls) {
            String result="";
            URL url;
            HttpURLConnection urlConnection=null;
            try
            {
                url = new URL(urls[0]);
                urlConnection =(HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int data=inputStreamReader.read();
                while(data!=-1)
                {
                    char current = (char) data;
                    result+=current;
                    data=inputStreamReader.read();
                }
                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems=10;
                if (jsonArray.length()<10)
                {
                    numberOfItems=jsonArray.length();
                }
                articlesDB.execSQL("DELETE FROM articles");

                for(int i=0;i<numberOfItems;i++)
                {
                    String articleInfo="";
                    String itemId=jsonArray.getString(i);
                    url= new URL("https://hacker-news.firebaseio.com/v0/item/"+itemId+".json?print=pretty");
                    urlConnection =(HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                   inputStreamReader = new InputStreamReader(inputStream);
                    data=inputStreamReader.read();
                    while(data!=-1)
                    {
                        char current = (char) data;
                        articleInfo+=current;
                        data=inputStreamReader.read();
                    }
                    Log.i("Article",articleInfo);
                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title")&&!jsonObject.isNull("title"))
                    {
                        String title=jsonObject.getString("title");
                        String website = jsonObject.getString("url");
                        Log.i("Title and Url",title+" "+website);
                        url= new URL(website);
                        String articleContent="";
                        urlConnection =(HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        data=inputStreamReader.read();
                        while(data!=-1)
                        {
                            char current = (char) data;
                            articleContent+=current;
                            data=inputStreamReader.read();
                        }
                        Log.i("Article Content",articleContent);

                        String sql="INSERT INTO articles (articleId,articleTitle,articleContent) VALUES (?,?,?)";
                        SQLiteStatement statement=articlesDB.compileStatement(sql);
                        statement.bindString(1,itemId);
                        statement.bindString(2,title);
                        statement.bindString(3,articleContent);
                        statement.execute();
                    }

                }


             //   TextView textView = findViewById(R.id.textView);
               // textView.setText(result);
               // Log.i("URL Content",result);
                return result;


            }
            catch (Exception e)
            {
                e.printStackTrace();
            }



            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
         //   updateListView();
        }
    }
}
