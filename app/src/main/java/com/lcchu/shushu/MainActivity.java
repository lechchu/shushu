package com.lcchu.shushu;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

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

    String[] tab_item = {"xuanhuan", "lianzai", "suixuan", "xuanhuan", "gudaiyanqing", "chuanyuechongsheng", "dushi", "kehuan", "xianxia", "yanqing", "lishi", "lingyi", "xuanyi", "xuanhuan", "youxi", "qita"};

    RecyclerView searchResultList;
    RecyclerView favoriteBooksList;

    private AdView ad;
    boolean adOn = true;

    SearchAdapter SAdapter;
    FavoriteAdapter FAdapter;

    public void setupListener() {

        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadingDialog = new ProgressDialog(MainActivity.this);
                loadingDialog.setMessage("努力加載中");
                loadingDialog.show();

                if(tab.getPosition()==1){
                    new Thread(getRank).start();
                    if(searchResultList.getVisibility()!=View.VISIBLE)
                        searchResultList.setVisibility(View.VISIBLE);
                    if(favoriteBooksList.getVisibility()==View.VISIBLE)
                        favoriteBooksList.setVisibility(View.GONE);
                }
                else if (tab.getPosition()>0){

                    new Thread (new getNovelList(tab_item[tab.getPosition()])).start();
                    if(searchResultList.getVisibility()!=View.VISIBLE)
                        searchResultList.setVisibility(View.VISIBLE);
                    if(favoriteBooksList.getVisibility()==View.VISIBLE)
                        favoriteBooksList.setVisibility(View.GONE);
                }else{

                    loadingDialog.dismiss();
                    if (searchResultList.getVisibility() == View.VISIBLE){
                        searchResultList.setVisibility(View.GONE);
                        favoriteBooksList.setVisibility(View.VISIBLE);

                        uiUpdateHandler.sendEmptyMessage(1);
                        loadFavBooksJson();
                    }
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
                    //new Thread(getRank).start();
                    //new Thread(getAll).start();
                    if(searchResultList.getVisibility()!=View.VISIBLE)
                        searchResultList.setVisibility(View.VISIBLE);
                    if(favoriteBooksList.getVisibility()==View.VISIBLE)
                        favoriteBooksList.setVisibility(View.GONE);
                }
                return false;
            }
        });



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

        AdRequest adRequest = new AdRequest.Builder().build();
        if(adOn)
            ad.loadAd(adRequest);


        //new Thread(getSearchResult).start();
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

                long t1,t2;
                uiUpdateHandler.sendEmptyMessage(1);
                //https://tw.ttkan.co/novel/search?q=%E9%87%91%E5%BA%B8
                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");
                bookKeyWord = java.net.URLEncoder.encode(bookKeyWord,"UTF-8");


                t1 = System.currentTimeMillis();
                Document doc = Jsoup.connect("https://tw.mingzw.net/mzwlist/"+bookKeyWord+".html").ignoreContentType(true).get();
                t2 = System.currentTimeMillis();
                System.out.println("get load page: "+(t2-t1));

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
                        ni.setDesc(result.select("div.cont > p").text());
                        searchResult.add(ni);
                    }

            } catch (IOException e) {
                e.printStackTrace();
            }
            SAdapter = new SearchAdapter(searchResult);
            uiUpdateHandler.sendEmptyMessage(0);
        }
    };

    Runnable getRank = new Runnable() {
        @Override
        public void run() {
            try{
                searchResult = new ArrayList<>();
                //TODO 抓排行榜
                long t1,t2;
                uiUpdateHandler.sendEmptyMessage(1);
                //https://tw.ttkan.co/novel/search?q=%E9%87%91%E5%BA%B8
                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");
               // bookKeyWord = java.net.URLEncoder.encode(bookKeyWord,"UTF-8");


                t1 = System.currentTimeMillis();

                String rankURL = "https://tw.ttkan.co/novel/rank/";
//                String rankType = "xuanhuan";

                Document doc = Jsoup.connect(rankURL).ignoreContentType(true).get();
                t2 = System.currentTimeMillis();
                System.out.println(t2-t1);

                int infoIndex = 0;
                NovelInfo ni = null;
                Elements temp = doc.select("li");

                for (Element result : temp) {
                    switch (infoIndex++) {
                        case 0:
                            ni = new NovelInfo();
                            String[] name = result.select("a").attr("href").split("/");
                            ni.setName(name[3]);
                            ni.setCoverURL("https://static.ttkan.co/cover/" + ni.getName().split("-")[0] + ".jpg");
                            ni.setTitle(result.text());
                            break;
                        case 1:
                            ni.setAuthor(result.text());
                            break;
                        case 2:
                            //類別
//                            Document desdoc = Jsoup.connect("https://tw.ttkan.co/novel/chapters/"+ni.getName()).ignoreContentType(true).get();
//                            ni.setDesc("簡介: "+desdoc.select(".description").select(".p").text());
                            ni.setDesc("簡介: ");
                            break;
                        case 3:
                            //狀態:連載;完結
                            infoIndex = 0;
                            searchResult.add(ni);
                            break;
                        default:
                            break;
                    }

                }
                SAdapter = new SearchAdapter(searchResult);
                uiUpdateHandler.sendEmptyMessage(0);
            }catch (Exception e){e.printStackTrace();}
        }
    };


    class getNovelList implements Runnable {
        String mCategory ;
        getNovelList ( String Category ) { mCategory = Category; }
        public void run ( ) {

//            https://tw.ttkan.co/api/nq/amp_novel_list?type=xuanhuan&limit=100
            try{
                //TODO 抓分類小說
                long t1,t2;
                uiUpdateHandler.sendEmptyMessage(1);

                t1 = System.currentTimeMillis();

                String allURL = "https://tw.ttkan.co/novel/class/";
                String allType = mCategory;

                Document doc = Jsoup.connect(allURL+allType+"&limit=100").ignoreContentType(true).get();
                t2 = System.currentTimeMillis();
                System.out.println(t2-t1);

                int info_index = 0;
                NovelInfo ni = null;
                Elements temp = doc.select("li");

                for (Element result : temp) {
                    switch (info_index++) {
                        case 0:
                            ni = new NovelInfo();
                            String[] name = result.select("a").attr("href").split("/");
                            ni.setName(name[3]);
                            ni.setCoverURL("https://static.ttkan.co/cover/" + ni.getName().split("-")[0] + ".jpg");
                            ni.setTitle(result.text());
                            break;
                        case 1:
                            ni.setAuthor(result.text());
                            break;
                        case 2:
                            ni.setDesc(result.text());
                            info_index = 0;
                            if(!ni.getName().equals("{{novel_id}}"))
                                searchResult.add(ni);
                            break;
                        default:
                            break;
                    }

                }
                SAdapter = new SearchAdapter(searchResult);
                uiUpdateHandler.sendEmptyMessage(0);
            }catch (Exception e){e.printStackTrace();}

        }
    }

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