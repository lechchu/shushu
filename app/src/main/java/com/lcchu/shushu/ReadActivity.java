package com.lcchu.shushu;

import java.io.IOException;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
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
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.navigation.NavigationView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
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

    int currentIndex = 0;
    int scrolled_histroy = 0;
    int text_size = 24;

    boolean isExit=false;
    boolean switch_clock, switch_darkmode;

    BookData book;


    ChapterListAdapter CAdapter;

    ArrayList<ArrayList<String>> chapterList = new ArrayList<>();
    ArrayList<String> chapterData = new ArrayList<>();

    Thread chapterLoad, stroyRead;

    AlertDialog settingDialog;

    private Timer saveTimer;

    boolean adOn = true;

//UI


    View setting_layout,story_layout;

    SmartRefreshLayout switchChapter;

    TextView tv1, chapterName, txtsizeView;
    ImageView bookCover;
    ScrollView storyScrollView;
    NavigationView chapterListView, settingView;
    RecyclerView chapterListViewR;
    DrawerLayout chapterListDrawer;
    SeekBar editfontsize;
    Switch darkmodeSwitch, clockSwitch;
    ProgressDialog loadingDialog;
    TextClock clock;

    Elements title = new Elements();



/*
    public static void trustEveryone() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            } }, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

 */

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
        tv1 = findViewById(R.id.textView);
        editfontsize.setProgress(text_size);

        txtsizeView.setText(String.valueOf(text_size));

        switchChapter = findViewById(R.id.loadLayout);
        switchChapter.setEnableLoadMore(true);
        switchChapter.setEnableRefresh(true);
        switchChapter.setEnableAutoLoadMore(false);
        switchChapter.setFooterTriggerRate((float)0.5);
        switchChapter.setHeaderTriggerRate((float)0.5);


        storyScrollView = findViewById(R.id.storyscroll);
        story_layout = findViewById(R.id.story_layout);

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

        switchChapter.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                if(currentIndex-1<0)
                    Toast.makeText(ReadActivity.this,"已是第一章",Toast.LENGTH_LONG).show();
                else {
                    scrolled_histroy = 0;
                    book.updateChapter(chapterList.get(--currentIndex).get(1));
                    CAdapter.updateIndex(currentIndex);

                    stroyRead = new Thread(getStory);
                    stroyRead.start();
                    try {
                        stroyRead.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                switchChapter.finishRefresh();
            }
        });

        switchChapter.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                if(currentIndex+1>=chapterList.size())
                    Toast.makeText(ReadActivity.this,"已是最後一章",Toast.LENGTH_LONG).show();
                else {
                    scrolled_histroy = 0;
                    book.updateChapter(chapterList.get(++currentIndex).get(1));
                    CAdapter.updateIndex(currentIndex);

                    stroyRead = new Thread(getStory);
                    stroyRead.start();
                    try {
                        stroyRead.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                switchChapter.finishLoadMore();

            }
        });

        chapterListDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                chapterListViewR.smoothScrollToPosition(currentIndex);

            }
        });

        settingView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
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
                if(isChecked) {
                    //storyScrollView.setBackgroundColor(Color.parseColor("#2C3E50"));

                    story_layout.setBackground(new ColorDrawable(Color.parseColor("#2C3E50")));
                    tv1.setTextColor(Color.parseColor("#EAECEE"));
                }else {
                    //storyScrollView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    story_layout.setBackground(new ColorDrawable(Color.parseColor("#FFFFFF")));

                    tv1.setTextColor(Color.parseColor("#666666"));
                }
                saveSetting();
                }


        });

        clockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switch_clock=isChecked;
                if(isChecked) {
                    clock.setVisibility(View.VISIBLE);
                }else {
                    clock.setVisibility(View.GONE);
                }
                saveSetting();
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
            saveHistory();
            saveSetting();
        }
    }

    private void setSaveTimer(){

        saveTimer = new Timer();

        saveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                saveHistory();
            }
        }, 3000, 1000);
    }

    private void adLoad(){
        final InterstitialAd mInterstitialAd;

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3049736794394736/2698302682");


        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                mInterstitialAd.show();
            }
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                //mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }
            @Override
            public void onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("努力加載中");
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
/*
        chapterLoad = new Thread(JsonReader);

        try{
            chapterLoad.start();
            chapterLoad.join();
        }catch (Exception e){e.printStackTrace();}
*/

        new Thread(loadChapterList).start();



        //new Thread(getCover).start();
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


            handler.sendEmptyMessage(3);
            try {
                System.out.println("start load story-thread");
                //trustEveryone();
                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");
                /*
                okHttp = new OkHttpClient();
                Request request = new Request.Builder().url(book.getBookURL()).get().build();
                Document doc = Jsoup.parse(okHttp.newCall(request).execute().body().string());

                 */
                Document doc = Jsoup.connect(book.getBookURL()).ignoreContentType(true).get();

                title = doc.select("p");
                saveHistory();

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("start send story message");
            handler.sendEmptyMessage(0);
        }
    };


    Runnable loadChapterList = new Runnable(){
        @Override
        public void run() {


                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");

                try {

                    //TODO 加速讀章節
                    long t1,t2;


                    t1=System.currentTimeMillis();
                    Connection conn = Jsoup.connect(book.getChapterListURL()).ignoreContentType(true);
                    chapterList = new ArrayList<>();
                    JSONArray chapterJson = new JSONObject(conn.get().text()).getJSONArray("items");

                    book.updateChapter(chapterJson.getJSONObject(currentIndex).getString("chapter_id"));
                    System.out.println("make getStory start"); // 讀取章節過程中先讀取小說內容
                    new Thread(getStory).start();

                    for(int i=0;i<chapterJson.length();i++){

                        chapterData = new ArrayList<>();
                        chapterData.add(chapterJson.getJSONObject(i).getString("chapter_name"));
                        chapterData.add(chapterJson.getJSONObject(i).getString("chapter_id"));
                        chapterList.add(chapterData);

                    }
                    t2=System.currentTimeMillis();
                    System.out.println("load chapter time:"+(t2-t1));


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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {// 當keyCode等於退出事件值時
            ToQuitTheApp();
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_HOME||keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            saveHistory();
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
                        long t1,t2;
                        t1 = System.currentTimeMillis();
                        System.out.println("Start load story");
                        tv1.setText("\n\n\n");
                        for (Element text : title)
                            tv1.append(text.text()+"\n\n");
                        t2= System.currentTimeMillis();
                        System.out.println("load story time: "+(t2-t1));

                        chapterName.setText(chapterList.get(currentIndex).get(0));
                        handler.sendEmptyMessageDelayed(5, 300);

                        break;

                    case 1:
                        chapterListViewR.setAdapter(CAdapter);
                        chapterListViewR.scrollToPosition(currentIndex);
                        break;
                    case 2:
                        //bookCover.setImageBitmap(cover);
                        break;
                    case 3:
                        tv1.setVisibility(View.INVISIBLE);
                        break;
                    case 4:
                        darkmodeSwitch.setChecked(switch_darkmode);
                        clockSwitch.setChecked(switch_clock);
                        tv1.setTextSize(text_size);
                        break;
                    case 5:
                        storyScrollView.scrollTo(0, scrolled_histroy);
                        tv1.setVisibility(View.VISIBLE);
                        loadingDialog.dismiss();
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