package com.lcchu.shushu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.tabs.TabLayout;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;



public class MainActivity extends AppCompatActivity {

    String bookKeyWord = "";

    ArrayList<NovelInfo> searchResult = new ArrayList<>();

    ProgressDialog loadingDialog;

    EditText bookkeyEdit;

    TabLayout tab;

    String[] tab_item = {"all", "全本", "玄幻", "奇幻", "武俠", "仙俠", "都市", "言情", "軍事", "游戲", "競技", "科幻", "靈異"};

    private RecyclerView searchResultList;
    private RecyclerView favoriteBooksList;

    private AdView ad;
    boolean adOn = true;

    SearchAdapter SAdapter;
    FavoriteAdapter FAdapter;
    private static final OkHttpClient client = OkHttpSingleton.getInstance();

    private static String fetchHtml(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public void setupListener() {

        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadingDialog = new ProgressDialog(MainActivity.this);
                loadingDialog.setMessage("加載中");
                loadingDialog.show();

                if(tab.getPosition()==0){
                    loadingDialog.dismiss();
                    if (searchResultList.getVisibility() == View.VISIBLE){
                        searchResultList.setVisibility(View.GONE);
                        favoriteBooksList.setVisibility(View.VISIBLE);
                        uiUpdateHandler.sendEmptyMessage(1);
                        loadFavBooksJson();
                    }
                }
                else{

                    new Thread (new getNovelList(tab_item[tab.getPosition()-1])).start();
                    if(searchResultList.getVisibility()!=View.VISIBLE)
                        searchResultList.setVisibility(View.VISIBLE);
                    if(favoriteBooksList.getVisibility()==View.VISIBLE)
                        favoriteBooksList.setVisibility(View.GONE);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });


        //搜索鍵
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            bookkeyEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if(actionId == EditorInfo.IME_ACTION_SEARCH&& !bookkeyEdit.getText().toString().equals("")){
                        bookKeyWord = bookkeyEdit.getText().toString();
                        tab.setScrollPosition(0,0,true);

                        //hide ime after enter
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
                        bookkeyEdit.clearFocus();

                        loadingDialog = new ProgressDialog(MainActivity.this);
                        loadingDialog.setMessage("努力加載中");
                        loadingDialog.show();

                        new Thread(getSearchResult).start();
                        if(searchResultList.getVisibility()!=View.VISIBLE)
                            searchResultList.setVisibility(View.VISIBLE);
                        if(favoriteBooksList.getVisibility()==View.VISIBLE)
                            favoriteBooksList.setVisibility(View.GONE);
                    }
                    return false;
                }
            });
        }


    }

    public void initView() {
        bookkeyEdit = findViewById(R.id.BookKeyeditText);

        ad = findViewById(R.id.adView);

        tab = findViewById(R.id.tab);

        searchResultList = findViewById(R.id.searchResultView);
        searchResultList.addItemDecoration(new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL));
        favoriteBooksList = findViewById(R.id.favoriteBooksView);
        favoriteBooksList.addItemDecoration(new GridSpacingItemDecoration(2, 50, false));

        favoriteBooksList.setLayoutManager(new GridLayoutManager(this, 2));


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        setupListener();
        loadFavBooksJson();


        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });


        if(adOn) {

            ad = new AdView(this);
            ad.setAdUnitId("ca-app-pub-3940256099942544/9214589741");
            AdRequest adRequest = new AdRequest.Builder().build();
            ad.loadAd(adRequest);
        }

        // pre connect for speed up load chapter and content
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://tw.mingzw.net/") // 用輕量級 API 測試
                        .build();
                client.newCall(request).execute().close();
            } catch (Exception ignored) {}
        }).start();
    }

    public void loadFavBooksJson(){
        //TODO improve

        //String json = "{\"books\":[{\"name\":\"xianwujinyong-chuqiao\",\"title\":\"仙武金庸\",\"cover_url\":\"https://static.ttkan.co/cover/xianwujinyong-chuqiao.jpg\"},{\"name\":\"xiaoaojianghu-jinyong\",\"title\":\"笑傲江湖\",\"cover_url\":\"https://static.ttkan.co/cover/xiaoaojianghu.jpg\"}]}";
        String json;

        ArrayList<NovelInfo> favBooks = new ArrayList<>();
        File favFile = getBaseContext().getFileStreamPath("favBooksInfo.json");
        if(favFile.exists()) {
            try {

                FileInputStream fis = new FileInputStream(favFile);
                byte[] readBytes = new byte[fis.available()];
                fis.read(readBytes);
                fis.close();
                json = new String(readBytes);

                NovelInfo favBookDatas;
                JSONArray favBooksJson = new JSONObject(json).getJSONArray("books");
                JSONObject tempJO;

                for (int i = 0; i < favBooksJson.length(); i++) {
                    tempJO = favBooksJson.getJSONObject(i);
                    favBookDatas = new NovelInfo();

                    favBookDatas.setName(tempJO.getString("name"));
                    favBookDatas.setTitle(tempJO.getString("title"));
                    favBookDatas.setCoverURL(tempJO.getString("cover_url"));

                    favBooks.add(favBookDatas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            FAdapter = new FavoriteAdapter(favBooks);
            uiUpdateHandler.sendEmptyMessage(2);
        }
    }

    Runnable getSearchResult = new Runnable() {
        @Override
        public void run() {

            try {

                uiUpdateHandler.sendEmptyMessage(1);

                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");
                bookKeyWord = java.net.URLEncoder.encode(bookKeyWord,"UTF-8");

                long startOkHttp = System.currentTimeMillis();
                String html = fetchHtml("https://tw.mingzw.net/mzwlist/"+bookKeyWord+".html");
                Document doc = Jsoup.parse(html);
                long endOkHttp = System.currentTimeMillis();
                Log.d("MainActivity","OkHttp + Jsoup 解析花費時間：" + (endOkHttp - startOkHttp) + "ms");

                int infoIndex = 0;
                NovelInfo ni = null;
                Elements temp = doc.select("div.figure-horizontal.figure-1");

                for (Element result : temp)
                {
                    ni = new NovelInfo();
                    String[] name = result.select("div.cont > h3 > a").attr("href").split("/");
                    ni.setName(name[2].split("\\.")[0]);
                    ni.setCoverURL("https://tw.mingzw.net/images/mzwid/" + ni.getName() + ".jpg");
                    ni.setTitle(result.select("div.cont > h3 > a").text());
                    ni.setAuthor(result.select("div.cont > dl:nth-child(2) > dd").text());
                    ni.setDesc(result.select("div.cont > p").text().replaceAll("查看詳細>>", ""));
                    searchResult.add(ni);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
//                throw new RuntimeException(e);
                e.printStackTrace();
            }
            SAdapter = new SearchAdapter(searchResult);
            uiUpdateHandler.sendEmptyMessage(0);
        }
    };

    class getNovelList implements Runnable {
        String mCategory ;
        getNovelList ( String Category ) { mCategory = Category; }
        public void run ( ) {

            try{
                //TODO 抓分類小說
                long t1,t2;
                uiUpdateHandler.sendEmptyMessage(1);

                t1 = System.currentTimeMillis();

                String allURL = "https://tw.mingzw.net/mzwlist/";
                String allType = java.net.URLEncoder.encode(mCategory,"UTF-8");

                Document doc = Jsoup.connect(allURL+allType+".html").ignoreContentType(true).get();
                t2 = System.currentTimeMillis();
                System.out.println(t2-t1);

                int infoIndex = 0;
                NovelInfo ni = null;
                Elements temp = doc.select("div.figure-horizontal.figure-1");

                for (Element result : temp)
                {
                    ni = new NovelInfo();
                    String[] name = result.select("div.cont > h3 > a").attr("href").split("/");
                    ni.setName(name[2].split("\\.")[0]);
                    ni.setCoverURL("https://tw.mingzw.net/images/mzwid/" + ni.getName() + ".jpg");
                    ni.setTitle(result.select("div.cont > h3 > a").text());
                    ni.setAuthor(result.select("div.cont > dl:nth-child(2) > dd").text());
                    ni.setDesc(result.select("div.cont > p").text().replaceAll("查看詳細>>", ""));
                    searchResult.add(ni);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
//                throw new RuntimeException(e);
                e.printStackTrace();
            }
            SAdapter = new SearchAdapter(searchResult);
            uiUpdateHandler.sendEmptyMessage(0);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) { // 攔截返回鍵
            tab.setScrollPosition(0,0,true);
            if (searchResultList.getVisibility() == View.VISIBLE){
                searchResultList.setVisibility(View.GONE);
                favoriteBooksList.setVisibility(View.VISIBLE);

                uiUpdateHandler.sendEmptyMessage(1);
                loadFavBooksJson();
            }else
                return super.onKeyDown(keyCode, event);

        }
        return true;
    }

    @SuppressLint("HandlerLeak")
    Handler uiUpdateHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try{
                switch (msg.what) {
                    case 0:
                        searchResultList.setAdapter(SAdapter);
                        loadingDialog.dismiss();
                        break;
                    case 1:
                        bookkeyEdit.setText("");
                        searchResult.clear();
                        Objects.requireNonNull(searchResultList.getAdapter()).notifyDataSetChanged();
                    case 2:
                        favoriteBooksList.setAdapter(FAdapter);
                        break;
                    default:
                        break;
                }
            }catch (Exception e){e.printStackTrace();}
        }


    };

    public void showToast(String msg){
        Toast.makeText(MainActivity.this,String.valueOf(msg),Toast.LENGTH_LONG).show();
    }
    public void showToast(int msg){
        Toast.makeText(MainActivity.this,String.valueOf(msg),Toast.LENGTH_LONG).show();
    }
}