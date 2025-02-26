package com.lcchu.shushu;

import java.io.IOException;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextClock;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Protocol;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;
/*
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
*/


public class ReadActivity extends AppCompatActivity {

    String bookName;

    private int currentIndex = 0;
    int scrolled_histroy = 0;
    int text_size = 24;

    boolean isExit=false;
    boolean switch_clock, switch_darkmode;

    String novel_content;

    BookData book;


    private ChapterListAdapter CAdapter;
    private ContentAdapter contentAdapter;

    ArrayList<ArrayList<String>> chapterList = new ArrayList<>();
    ArrayList<String> chapterData = new ArrayList<>();

    Thread chapterLoad, stroyRead;

    AlertDialog settingDialog;

    private Timer saveTimer;

    boolean adOn = false;
    private static final OkHttpClient client = OkHttpSingleton.getInstance();

//UI


    View setting_layout,story_layout;

    SmartRefreshLayout switchChapter;

    private TextView tv1, chapterName, txtsizeView;
    ImageView bookCover;
    ScrollView storyScrollView;
    NavigationView chapterListView, settingView;
    private RecyclerView chapterListViewR;
    private RecyclerView novelcontentView;
    DrawerLayout chapterListDrawer;
    SeekBar editfontsize;
    Switch darkmodeSwitch, clockSwitch;
    ProgressDialog loadingDialog;
    TextClock clock;
    private ArrayList<Pair<Integer, String>> novelcontentList = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isLoadingPreviousChapter = false;
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


    @SuppressLint("InflateParams")
    public void setupView(){
        bookCover = findViewById(R.id.cover_imageView);

        setting_layout = LayoutInflater.from(ReadActivity.this).inflate(R.layout.setting, null);
        editfontsize = setting_layout.findViewById(R.id.fontsize_seekbar);
        darkmodeSwitch = setting_layout.findViewById(R.id.switch_darkmode);
        clockSwitch = setting_layout.findViewById(R.id.switch_clock);
        clock = findViewById(R.id.textclock);

        txtsizeView = setting_layout.findViewById(R.id.fontsize_textview);
        chapterName = findViewById(R.id.chapternameView);
        editfontsize.setProgress(text_size);

        txtsizeView.setText(String.valueOf(text_size));

//        switchChapter = findViewById(R.id.loadLayout);
//        switchChapter.setEnableLoadMore(true);
//        switchChapter.setEnableRefresh(true);
//        switchChapter.setEnableAutoLoadMore(false);
//        switchChapter.setFooterTriggerRate((float)0.5);
//        switchChapter.setHeaderTriggerRate((float)0.5);


        storyScrollView = findViewById(R.id.storyscroll);
        story_layout = findViewById(R.id.story_layout);
//        tv1 = findViewById(R.id.textView);

//      RecyclerView Novel Content
        novelcontentView = findViewById(R.id.content_recyclerView);
        LinearLayoutManager novelcontent_layoutManager = new LinearLayoutManager(this);
        novelcontentView.setLayoutManager(novelcontent_layoutManager);



        chapterListView = findViewById(R.id.chapterlist_navigation_view);
        settingView = findViewById(R.id.setting_navigation_view);

        chapterListDrawer = findViewById(R.id.drawerLayout);
        chapterListViewR = findViewById(R.id.chapterlist_RecyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        chapterListViewR.setLayoutManager(layoutManager);
        chapterListViewR.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupListener(){

//        switchChapter.setOnRefreshListener(new OnRefreshListener() {
//            @Override
//            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
//                if(currentIndex-1<0)
//                    Toast.makeText(ReadActivity.this,"已是第一章",Toast.LENGTH_LONG).show();
//                else {
//                    scrolled_histroy = 0;
//                    book.updateChapter(chapterList.get(--currentIndex).get(1));
//                    CAdapter.updateIndex(currentIndex);
//
//                    stroyRead = new Thread(getStory);
//                    stroyRead.start();
//                    try {
//                        stroyRead.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                switchChapter.finishRefresh();
//            }
//        });
//
//        switchChapter.setOnLoadMoreListener(new OnLoadMoreListener() {
//            @Override
//            public void onLoadMore(RefreshLayout refreshLayout) {
//                if(currentIndex+1>=chapterList.size())
//                    Toast.makeText(ReadActivity.this,"已是最後一章",Toast.LENGTH_LONG).show();
//                else {
//                    scrolled_histroy = 0;
//                    book.updateChapter(chapterList.get(++currentIndex).get(1));
//                    CAdapter.updateIndex(currentIndex);
//
//                    stroyRead = new Thread(getStory);
//                    stroyRead.start();
//                    try {
//                        stroyRead.join();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                switchChapter.finishLoadMore();
//
//            }
//        });

        chapterListDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                chapterListViewR.smoothScrollToPosition(currentIndex);

            }
        });

        settingView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                chapterListDrawer.closeDrawer(GravityCompat.END);
                settingDialog.show();
                return false;
            }
        });

        //語法一：new AlertDialog.Builder(主程式類別).XXX.XXX.XXX;
        final AlertDialog.Builder builder = new AlertDialog.Builder(ReadActivity.this);
        // Set icon value.
        // Set title value.
        builder.setTitle("設定");
        builder.setView(setting_layout);
        builder.setCancelable(true);
        settingDialog = builder.create();


        settingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                saveSetting();
            }
        });

        editfontsize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtsizeView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                text_size = seekBar.getProgress();
                handler.sendEmptyMessage(4);
            }
        });

        darkmodeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch_darkmode=isChecked;
//                if(isChecked) {
//                    story_layout.setBackground(new ColorDrawable(Color.parseColor("#2C3E50")));
//                    tv1.setTextColor(Color.parseColor("#EAECEE"));
//                }else {
//                    story_layout.setBackground(new ColorDrawable(Color.parseColor("#FFFFFF")));
//                    tv1.setTextColor(Color.parseColor("#666666"));
//                }
                saveSetting();
                }


        });

        clockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                switch_clock=isChecked;
//                if(isChecked) {
//                    clock.setVisibility(View.VISIBLE);
//                }else {
//                    clock.setVisibility(View.GONE);
//                }
//                saveSetting();
            }

        });


        novelcontentView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    int currentPosition = contentAdapter.getChapterIndex(firstVisibleItem);
                    if(currentIndex!=currentPosition) {
                        currentIndex = currentPosition;
                        CAdapter.updateIndex(currentIndex);
                    }

                    // 預載入下一章
                    if ((lastVisibleItem == (totalItemCount - 1)) && !isLoading) {
                        if(currentIndex==chapterList.size()-1)
                            Toast.makeText(ReadActivity.this,"已是第一章",Toast.LENGTH_LONG).show();
                        else {
                            isLoadingPreviousChapter = false;
                            book.updateChapter(chapterList.get(currentIndex + 1).get(1));
                            stroyRead = new Thread(getStory);
                            stroyRead.start();
                            try {
                                stroyRead.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // 預載入上一章
                    if (firstVisibleItem <= 1 && !isLoading) {
                        if(currentIndex-1<0)
                            Toast.makeText(ReadActivity.this,"已是第一章",Toast.LENGTH_LONG).show();
                        else {
                            isLoadingPreviousChapter = true;
                            book.updateChapter(chapterList.get(currentIndex-1).get(1));
                            stroyRead = new Thread(getStory);
                            stroyRead.start();
                            try {
                                stroyRead.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

//                    Log.d("RecyclerView", "當前第一個可見的 Item 是：" + firstVisibleItem);
//                    Log.d("RecyclerView", "當前最後可見的 Item 是：" + lastVisibleItem);
//                    Log.d("RecyclerView", "當前總共的 Item 是：" + totalItemCount);
//                    Log.d("RecyclerView", "當前Chapter ID 是：" + currentIndex);
//                    Log.d("RecyclerView", "當前書名 是：" + currentPosition);

                    try{
                        getSharedPreferences(bookName, MODE_PRIVATE).edit()
                                .putInt("Index", currentIndex)
                                .putInt("Scrolled", recyclerView.computeVerticalScrollOffset())
                                .apply();
                    }catch (Exception e){e.printStackTrace();}
                }

            }
        });

    }

    public void loadUserPref(){

        try {
            // 閱讀紀錄
            currentIndex = getSharedPreferences(bookName, MODE_PRIVATE).getInt("Index",0);
            scrolled_histroy = getSharedPreferences(bookName, MODE_PRIVATE).getInt("Scrolled",0);

            // 偏好設定
            text_size = getSharedPreferences("user_setting", MODE_PRIVATE).getInt("FontSize", 24);
            switch_clock = getSharedPreferences("user_setting", MODE_PRIVATE).getBoolean("Clock", true);
            switch_darkmode = getSharedPreferences("user_setting", MODE_PRIVATE).getBoolean("DarkMode", false);

            handler.sendEmptyMessage(4); //discharge textsize, darkmode, clock

        } catch (Exception e) {
            e.printStackTrace();
//            saveHistory();
            saveSetting();
        }
    }

    private void setSaveTimer(){

        saveTimer = new Timer();

//        saveTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                saveHistory();
//            }
//        }, 3000, 1000);
    }

    private void adLoad(){
//        final InterstitialAd mInterstitialAd;
//        AdRequest adRequest = new AdRequest.Builder().build();
//        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest,
//                new InterstitialAdLoadCallback() {
//                    @Override
//                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
//                        // The mInterstitialAd reference will be null until
//                        // an ad is loaded.
//                        mInterstitialAd = interstitialAd;
//                        Log.i(TAG, "onAdLoaded");
//                    }
//
//                    @Override
//                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                        // Handle the error
//                        Log.d(TAG, loadAdError.toString());
//                        mInterstitialAd = null;
//                    }
//                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("加載中");
        loadingDialog.show();

        System.out.println("started readactivty");
        bookName = Objects.requireNonNull(getIntent().getExtras()).getString("bookName");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_main);

        if(adOn)
            adLoad();


        long t1,t2;
        t1 = System.currentTimeMillis();
        setupView();
        t2 = System.currentTimeMillis();
        System.out.println("view connet time:"+(t2-t1));

        t1 = System.currentTimeMillis();
        setupListener();
        t2 = System.currentTimeMillis();
        System.out.println("listener setup time:"+(t2-t1));

        t1 = System.currentTimeMillis();
        loadUserPref();
        t2 = System.currentTimeMillis();
        System.out.println("load pref time:"+(t2-t1));

        book = new BookData(bookName,"0");
        contentAdapter = new ContentAdapter(novelcontentList);
        novelcontentView.setAdapter(contentAdapter);
        new Thread(loadChapterList).start();
        setSaveTimer();
    }

    public void saveHistory(){
        try{

            getSharedPreferences(bookName, MODE_PRIVATE).edit()
                    .putInt("Index", currentIndex)
                    .putInt("Scrolled", storyScrollView.getScrollY())
                    .apply();

        }catch (Exception e){e.printStackTrace();}
    }

    public void saveSetting(){
        try{

            getSharedPreferences("user_setting", MODE_PRIVATE).edit()
                    .putInt("FontSize", text_size)
                    .putBoolean("DarkMode",switch_darkmode)
                    .putBoolean("Clock",switch_clock)
                    .apply();
        }catch (Exception e){e.printStackTrace();}
    }

    Runnable getStory = new Runnable(){
        @Override
        public void run() {

            isLoading = true;
            try {
                System.out.println("start load story-thread");


                long startOkHttp = System.currentTimeMillis();
                String html = fetchHtml(book.getBookURL());
                Document doc = Jsoup.parse(html);
                long endOkHttp = System.currentTimeMillis();
                Log.d("ReadActivity","OkHttp + Jsoup 文章解析花費時間：" + (endOkHttp - startOkHttp) + "ms");

                Element novel_doc = doc.select("div.contents").first();
                novel_doc.select("div").remove();
                novel_content = novel_doc.html();
                novel_content = novel_content.replaceAll("<p>", "\n");
                novel_content = novel_content.replaceAll("</p>", "");
                novel_content = novel_content.replaceAll("明智屋中文 wWw.MinGzw.Net 沒有彈窗,更新及時", "");
                novel_content = novel_content.replaceAll("mayiwsk", "");
                novel_content = novel_content.replaceAll("←→", "");


//                saveHistory();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ReadActivity.this, "加載失敗，請確認網路環境", Toast.LENGTH_LONG).show();
            }
            System.out.println("start send story message");
            Handler handler = new Handler(Looper.getMainLooper());
            if(!isLoadingPreviousChapter){
                handler.postDelayed(() -> contentAdapter.insertNextChapter(currentIndex + 1, novel_content), 100);
            }else
                handler.postDelayed(() -> contentAdapter.insertPreviousChapter(currentIndex - 1, novel_content), 100);
//                runOnUiThread(() -> contentAdapter.insertNextChapter(currentIndex+1,novel_content));
//            else
//                runOnUiThread(() -> contentAdapter.insertPreviousChapter(currentIndex-1,novel_content));
            handler.postDelayed(() -> isLoading = false, 100);
//            isLoading = false;

            Log.d("ReadActivity", "getStory: Data added to adapter, novelcontentList size: " + novelcontentList.size());
//            handler.sendEmptyMessage(0);
        }
    };


    Runnable loadChapterList = new Runnable(){
        @Override
        public void run() {
                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");

                try {

                    long startOkHttp = System.currentTimeMillis();
                    String html = fetchHtml(book.getChapterListURL());
                    long temp = System.currentTimeMillis();
                    Log.d("ReadActivity","OkHttp 章節解析花費時間：" + (temp - startOkHttp) + "ms");

                    long startsc = System.currentTimeMillis();
                    String html2 = fetchHtml("https://www.mingzw.net/mzwbook/41935.html");
                    long endsc = System.currentTimeMillis();
                    Log.d("ReadActivity","OkHttp SC章節解析花費時間：" + (endsc - startsc) + "ms");

                    Document doc = Jsoup.parse(html);
                    long endOkHttp = System.currentTimeMillis();
                    Log.d("ReadActivity","Jsoup 章節解析花費時間：" + (endOkHttp - temp) + "ms");

                    Elements chapterList_temp = doc.select("div.content.gclearfix > ul >li");
                    String chapter_id = chapterList_temp.get(currentIndex).select("a").attr("href").split("_")[1];
                    book.updateChapter(chapter_id);
                    new Thread(getStory).start();
                    chapterList = new ArrayList<>();
                    for(int i=0; i< chapterList_temp.size()-2;i++)
                    {
                        chapterData = new ArrayList<>();
                        try
                        {
                            Element chapterlist_element = chapterList_temp.get(i);
                            chapter_id = chapterlist_element.select("a").attr("href").split("_")[1];
                            chapterData.add(chapterlist_element.text());
                            chapterData.add(chapter_id);
                            chapterList.add(chapterData);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    }


                    CAdapter = new ChapterListAdapter(chapterList,currentIndex);
                    CAdapter.setItemClickListener(new ChapterListAdapter.OnRecyclerViewClickListener() {
                        @Override
                        public void onItemClickListener(View view) {
                            // 重置滑動紀錄
                            scrolled_histroy = 0;
                            chapterListDrawer.closeDrawer(GravityCompat.START);
                            currentIndex = chapterListViewR.getChildAdapterPosition(view);
                            book.updateChapter(chapterList.get(currentIndex).get(1));
                            new Thread(getStory).start();
                            CAdapter.updateIndex(currentIndex);
                        }

                        @Override
                        public void onItemLongClickListener(View view) {

                        }
                    });


                }catch (Exception e){e.printStackTrace();}

            handler.sendEmptyMessage(1);
        }
    };

    Runnable getCover = new Runnable(){
        @Override
        public void run() {
            try {

                Glide.with(ReadActivity.this).load(book.getCoverURL()).into(bookCover);
                //handler.sendEmptyMessage(2);
            }catch (Exception e){e.printStackTrace();}
        }
    };


    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
//        saveHistory();
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {// 當keyCode等於退出事件值時
            ToQuitTheApp();
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_HOME||keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
//            saveHistory();
            return false;
        } else
            return super.onKeyDown(keyCode, event);
    }


    private void ToQuitTheApp() {
        if (isExit) {
            // ACTION_MAIN with category CATEGORY_HOME 啟動主屏幕
            saveTimer.cancel();
            this.finish();
        } else {
            isExit = true;
            //saveHistory();
            Toast.makeText(ReadActivity.this, "按下返回退出閱讀", Toast.LENGTH_SHORT).show();
            handler.sendEmptyMessageDelayed(9, 3000);// 3秒後發送消息
        }
    }



    @Override
    public void finish() {

        super.finish();
        this.overridePendingTransition(R.anim.stay, R.anim.out);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        if (isFinishing()){
//            overridePendingTransition(R.anim.stay, R.anim.out);
//        }
//
//    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            try {
                switch (msg.what) {

                    case 0:
//                        long t1,t2;
//                        t1 = System.currentTimeMillis();
//                        System.out.println("Start load story");
//                        tv1.setText("\n\n\n");
//                        tv1.append(novel_content);
//                        t2= System.currentTimeMillis();
//                        System.out.println("load story time: "+(t2-t1));
//                        contentAdapter.addChapter(novel_content);
//                        chapterName.setText(chapterList.get(currentIndex).get(0));
//                        handler.sendEmptyMessageDelayed(5, 300);


                        break;

                    case 1:
                        chapterListViewR.setAdapter(CAdapter);
                        chapterListViewR.scrollToPosition(currentIndex);
                        loadingDialog.dismiss();
                        break;
                    case 2:
                        //bookCover.setImageBitmap(cover);
                        break;
                    case 3:
//                        tv1.setVisibility(View.INVISIBLE);
                        break;
                    case 4:
                        darkmodeSwitch.setChecked(switch_darkmode);
                        clockSwitch.setChecked(switch_clock);
//                        tv1.setTextSize(text_size);
                        break;
                    case 5:
//                        storyScrollView.scrollTo(0, scrolled_histroy);
//                        tv1.setVisibility(View.VISIBLE);

                        break;

                    case 9:
                        isExit=false;
                        break;
                    default:
                        break;

                }


            }catch (Exception e){e.printStackTrace();}

        }
    };


}