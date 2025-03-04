package com.lcchu.shushu;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.Bundle;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadActivity extends AppCompatActivity {

    private String bookID;

    private int currentIndex = 0;
    private int scrolled_history = 0;
    int text_size = 24;

    boolean isExit=false;
    boolean switch_clock, switch_nightmode;

    private String novel_content;
    private BookData book;


    private ChapterListAdapter CAdapter;
    private ContentAdapter contentAdapter;

    private ArrayList<ArrayList<String>> chapterList = new ArrayList<>();
    private ArrayList<String> chapterData = new ArrayList<>();

    Thread chapterLoad, storyRead_thread;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable = null;  // 🔹 先初始化為 null

    private AlertDialog progressDialog;
    private AlertDialog.Builder alert_builder;

    private Timer saveTimer;

    boolean adOn = false;
    private static final OkHttpClient client = OkHttpSingleton.getInstance();

//UI
    private View story_layout;
    private TextView tv1, chapterName, clock;
    ImageView bookCover;
    ScrollView storyScrollView;
    NavigationView chapterListView, settingView;
    private RecyclerView chapterListViewR;
    private RecyclerView novelcontentView;
    DrawerLayout chapterListDrawer;
    private LinearLayoutManager chapterlist_layoutManager;
    SeekBar editfontsize;
    // Setting UI Define
    private View setting_layout;
    private SwitchMaterial switchNightMode, switchClock;
    private MaterialButton buttonIncreaseFont, buttonDecreaseFont;
    private SharedPreferences settings_preferences;
    private TextView textFontSizeValue;
    private BottomSheetDialog settings_bottomSheetDialog;
    // Novel Content
    private ArrayList<Pair<Integer, String>> novelcontentList = new ArrayList<>();
    private LinearLayoutManager novelcontent_layoutManager;
    private boolean isLoading = false;
    private boolean isPreLoading = false;
    private boolean isLoadingPreviousChapter = false;
    private boolean isUserScrolling = false;


    private static String fetchHtml(String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (SocketTimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @SuppressLint("InflateParams")
    public void setupView(){
        bookCover = findViewById(R.id.cover_imageView);

        clock = findViewById(R.id.textclock);

        // Settings
        setting_layout = getLayoutInflater().inflate(R.layout.settings_bottom_sheet, null);
        settings_bottomSheetDialog = new BottomSheetDialog(ReadActivity.this);
        settings_bottomSheetDialog.setContentView(setting_layout);
        switchNightMode = setting_layout.findViewById(R.id.switch_night_mode);
        switchClock = setting_layout.findViewById(R.id.switch_clock);
        buttonIncreaseFont = setting_layout.findViewById(R.id.button_increase_font);
        buttonDecreaseFont = setting_layout.findViewById(R.id.button_decrease_font);
        textFontSizeValue = setting_layout.findViewById(R.id.text_font_size_value);




        chapterName = findViewById(R.id.chapternameView);
        storyScrollView = findViewById(R.id.storyscroll);
        story_layout = findViewById(R.id.story_layout);

//      RecyclerView Novel Content
        novelcontentView = findViewById(R.id.content_recyclerView);
        novelcontent_layoutManager = new LinearLayoutManager(this);
        novelcontentView.setLayoutManager(novelcontent_layoutManager);
        novelcontentView.setClickable(true);



        chapterListView = findViewById(R.id.chapterlist_navigation_view);
        settingView = findViewById(R.id.setting_navigation_view);

        chapterListDrawer = findViewById(R.id.drawerLayout);
        chapterListViewR = findViewById(R.id.chapterlist_RecyclerView);

        chapterlist_layoutManager = new LinearLayoutManager(this);
        chapterListViewR.setLayoutManager(chapterlist_layoutManager);
        chapterListViewR.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        // Loading Dialog
        alert_builder = new AlertDialog.Builder(this);
        alert_builder.setView(R.layout.dialog_progress);
        alert_builder.setCancelable(false); // 禁止取消
        progressDialog = alert_builder.create();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setupListener(){

        chapterListDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);


            }
        });

        chapterName.setOnTouchListener((v,event)->{
            Log.d("ReadActivity", "chapter name touch");
            return true;
        });
        novelcontentView.setOnClickListener(v -> {
//            settings_bottomSheetDialog.show();
            Log.d("ReadActivity", "long click");
        });
        // 設定開關監聽事件
        switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("DarkMode", isChecked);
            applyNightMode(isChecked);
        });

        switchClock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("Clock", isChecked);
            switch_clock=isChecked;
            applyClockVisibility(isChecked);
        });

        // 字體調整按鈕
        buttonIncreaseFont.setOnClickListener(v -> {
            if (text_size < 48) {  // 限制最大字體
                text_size ++;
                updateFontSize();
            }
        });

        buttonDecreaseFont.setOnClickListener(v -> {
            if (text_size > 12) {  // 限制最小字體
                text_size --;
                updateFontSize();
            }
        });
        final GestureDetector chapterlist_gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

                float deltaX = e2.getX() - e1.getX();
                float deltaY = e1.getY() - e2.getY();
                int recyclerViewWidth = getResources().getDisplayMetrics().widthPixels;
                int recyclerViewHeight = getResources().getDisplayMetrics().heightPixels;

                // 檢查手勢是從右向左滑動
                if (e1.getX() < e2.getX()&&(Math.abs(deltaX) > (float) recyclerViewWidth / 2.5)&&(Math.abs(deltaY) < (float) recyclerViewHeight / 10)) {
                    // 右滑手勢，打開 Drawer
                    if (!chapterListDrawer.isDrawerOpen(GravityCompat.START)) {
                        chapterListDrawer.openDrawer(GravityCompat.START);
                    }
                    return true;
                }
                return false;
            }

            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                Log.d("ReadActivity", "single tap");
                int recyclerViewWidth = getResources().getDisplayMetrics().widthPixels;
                int recyclerViewHeight = getResources().getDisplayMetrics().heightPixels;
                int centerX = recyclerViewWidth / 2;
                int centerY = recyclerViewHeight / 2;
                float x = e.getRawX();  // 觸摸點的 X 座標
                float y = e.getRawY();  // 觸摸點的 Y 座標
                boolean isInCenter = Math.abs(x - centerX) < (float) recyclerViewWidth / 5 && Math.abs(y - centerY) < (float) recyclerViewHeight / 6;
                if (isInCenter) {
                    settings_bottomSheetDialog.show();
                    return true;
                }
                return false;
            }
        });


        novelcontentView.setOnTouchListener((v, event) -> {
            boolean gestureHandled = chapterlist_gestureDetector.onTouchEvent(event);
            // 判斷觸摸點是否在 RecyclerView 的中間
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isUserScrolling = true;  // 使用者開始手動滑動
            }
            return gestureHandled;
        });
        novelcontentView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                Log.d("ReadActivity", "isUserScrolling:"+isUserScrolling);
                if (!isUserScrolling||isLoading) {
//                    Log.d("ReadActivity", "returning isUserScrolling: "+isUserScrolling);
                    return;  // **忽略非用戶觸發的滑動**
                }
                // 取消舊的防抖執行

                if (updateRunnable != null) {
                    handler.removeCallbacks(updateRunnable);
                }
                updateRunnable = () -> {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                        int totalItemCount = layoutManager.getItemCount();
                        int currentPosition = contentAdapter.getChapterIndex(firstVisibleItem);
                        if (currentIndex != currentPosition) {
                            currentIndex = currentPosition;
                            CAdapter.updateIndex(currentIndex);
                            handler.postDelayed(() -> chapterName.setText(chapterList.get(currentIndex).get(0)), 50);
                        }

                        // 預載入下一章
                        if ((firstVisibleItem == (totalItemCount - 1)) && !isLoading) {
                            isLoading = true;
                            isPreLoading = true;
                            if (currentIndex == chapterList.size() - 1)
                                showToast("已是倒數第二章");
                            else {
                                isLoadingPreviousChapter = false;
                                book.updateChapter(chapterList.get(currentIndex + 1).get(1));
                                storyRead_thread = new Thread(getStory);
                                storyRead_thread.start();
                                try {
                                    storyRead_thread.join();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        // 預載入上一章
                        if (lastVisibleItem == 0 && !isLoading) {
                            isLoading = true;
                            isPreLoading = true;
                            if (currentIndex - 1 < 0)
                                showToast("已是第一章");
                            else {
                                isLoadingPreviousChapter = true;
                                book.updateChapter(chapterList.get(currentIndex - 1).get(1));
                                storyRead_thread = new Thread(getStory);
                                storyRead_thread.start();
                                try {
                                    storyRead_thread.join();
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

//                    Log.d("RecyclerView", "當前第一個可見的 Item 是：" + firstVisibleItem);
//                    Log.d("RecyclerView", "當前最後可見的 Item 是：" + lastVisibleItem);
//                    Log.d("RecyclerView", "當前總共的 Item 是：" + totalItemCount);
                    Log.d("RecyclerView", "當前Chapter Index 是：" + currentIndex);
//                    Log.d("RecyclerView", "當前書名 是：" + currentPosition);
                        int scrolled = novelcontent_layoutManager.findViewByPosition(firstVisibleItem).getTop();
                        try {
                            Log.d("ReadActivity", "saveHistory");
                            getSharedPreferences(bookID, MODE_PRIVATE).edit()
                                    .putInt("Index", currentIndex)
                                    .putInt("Scrolled", scrolled)
                                    .apply();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                handler.postDelayed(updateRunnable,30);

            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isUserScrolling = false;  // **停止滑動時重置**
                }
            }
        });



    }

    public void loadUserPref(){

        try {
            // 閱讀紀錄
            currentIndex = getSharedPreferences(bookID, MODE_PRIVATE).getInt("Index",0);
            scrolled_history = getSharedPreferences(bookID, MODE_PRIVATE).getInt("Scrolled",0);

            // 偏好設定
            text_size = settings_preferences.getInt("FontSize", 24);
            switch_clock = settings_preferences.getBoolean("Clock", true);
            switch_nightmode = settings_preferences.getBoolean("DarkMode", false);
            switchClock.setChecked(switch_clock);
            switchNightMode.setChecked(switch_nightmode);
            textFontSizeValue.setText(String.valueOf(text_size));
            applyClockVisibility(switch_clock);
//            applyNightMode(switch_darkmode);


        } catch (Exception e) {
            e.printStackTrace();
//            saveHistory();
//            saveSetting();
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
    private void updateFontSize() {
        contentAdapter.setFontSize(text_size);
        textFontSizeValue.setText(String.valueOf(text_size));
        saveSetting("FontSize", text_size);
        // 這裡可以應用到實際的閱讀內容
    }
    private void applyNightMode(boolean isEnabled) {
        // 這裡可根據需求更改為適用於整個 App 的夜間模式
        int targetNightMode = isEnabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        if (currentNightMode != targetNightMode) {
            AppCompatDelegate.setDefaultNightMode(targetNightMode);
            recreate();
        }
    }
    private void applyClockVisibility(boolean isEnabled) {
        if(isEnabled) {
            clock.setVisibility(View.VISIBLE);
        }else
            clock.setVisibility(View.GONE);

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

        System.out.println("started readactivty");
        bookID = Objects.requireNonNull(getIntent().getExtras()).getString("bookName");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_main);

        if(adOn)
            adLoad();
        settings_preferences = getSharedPreferences("user_setting", MODE_PRIVATE);

        setupView();
        setupListener();
        loadUserPref();

        book = new BookData(bookID,"0");
        contentAdapter = new ContentAdapter(novelcontentList);
        novelcontentView.setAdapter(contentAdapter);
        contentAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                runOnUiThread(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                });
            }
        });

        progressDialog.show();
//        loadChapterList.run();
        new Thread(loadChapterList).start();
//        setSaveTimer();
    }

    public void saveHistory(){
        try{
//            int scrolled = novelcontent_layoutManager.findViewByPosition(firstVisibleItem).getTop();
            getSharedPreferences(bookID, MODE_PRIVATE).edit()
                    .putInt("Index", currentIndex)
                    .putInt("Scrolled", novelcontent_layoutManager.findViewByPosition(currentIndex).getTop())
                    .apply();
        }catch (Exception e){e.printStackTrace();}
    }


    private void saveSetting(String key, boolean value) {
        SharedPreferences.Editor editor = settings_preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void saveSetting(String key, int value) {
        SharedPreferences.Editor editor = settings_preferences.edit();
        editor.putInt(key, value);
        editor.apply();
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
                Log.d("ReadActivity", "OkHttp + Jsoup 文章解析花費時間：" + (endOkHttp - startOkHttp) + "ms");

                Element novel_doc = doc.select("div.contents").first();
                novel_doc.select("div").remove();
                novel_content = novel_doc.html();
                novel_content = novel_content.replaceAll("<p>", "\n");
                novel_content = novel_content.replaceAll("</p>", "");
                novel_content = novel_content.replaceAll("明智屋中文 wWw.MinGzw.Net 沒有彈窗,更新及時", "");
                novel_content = novel_content.replaceAll("mayiwsk", "");
                novel_content = novel_content.replaceAll("←→", "");
//                saveHistory();

            } catch (SocketTimeoutException e) {
                showToast("加載失敗，請確認網路環境");
            }catch (Exception e) {
                e.printStackTrace();
                showToast("加載失敗，請確認網路環境");
            }
            System.out.println("end load story message");
            runOnUiThread(() -> {
                if (isPreLoading) {
                    if (isLoadingPreviousChapter) {
                        contentAdapter.insertPreviousChapter(currentIndex - 1, novel_content);
                    } else {
                        contentAdapter.insertNextChapter(currentIndex + 1, novel_content);
                    }
                } else {
                    contentAdapter.insertNextChapter(currentIndex, novel_content);
                    novelcontentView.postDelayed(() -> {
                        Log.d("ReadActivity", "===========scrollToPositionWithOffset");
                        novelcontent_layoutManager.scrollToPositionWithOffset(
                                novelcontent_layoutManager.findFirstVisibleItemPosition(), scrolled_history);
                    },500);
                }
            });
//            if(isPreLoading) {
//                if (!isLoadingPreviousChapter) {
////                handler.postDelayed(() -> contentAdapter.insertNextChapter(currentIndex + 1, novel_content), 100);
//                    runOnUiThread(() -> contentAdapter.insertNextChapter(currentIndex + 1, novel_content));
//                } else {
////                handler.postDelayed(() -> contentAdapter.insertPreviousChapter(currentIndex - 1, novel_content), 100);
//                    runOnUiThread(() -> contentAdapter.insertPreviousChapter(currentIndex - 1, novel_content));
//                }
//            }
//            else {
//                runOnUiThread(() -> contentAdapter.insertNextChapter(currentIndex, novel_content));
//                novelcontentView.post(() -> {
//                    Log.d("ReadActivity", "scrollToPositionWithOffset");
//                    novelcontent_layoutManager.scrollToPositionWithOffset(novelcontent_layoutManager.findFirstVisibleItemPosition(), scrolled_history);
//                });
//            }
//
//
//
            handler.postDelayed(() -> isLoading = false, 100);

            Log.d("ReadActivity", "getStory: Data added to adapter, novelcontentList size: " + novelcontentList.size());
//            handler.sendEmptyMessage(0);
        }
    };

    Runnable loadChapterList = new Runnable(){
        @Override
        public void run() {
                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");
                if(!progressDialog.isShowing())
                    progressDialog.show();
                try {

                    long startOkHttp = System.currentTimeMillis();
                    String html = fetchHtml(book.getChapterListURL());
                    long temp = System.currentTimeMillis();
                    Log.d("ReadActivity","OkHttp 章節解析花費時間：" + (temp - startOkHttp) + "ms");

                    Document doc = Jsoup.parse(html);
                    long endOkHttp = System.currentTimeMillis();
                    Log.d("ReadActivity","Jsoup 章節解析花費時間：" + (endOkHttp - temp) + "ms");

                    Elements chapterList_temp = doc.select("div.content.gclearfix > ul >li");
                    String chapter_id = chapterList_temp.get(currentIndex).select("a").attr("href").split("_")[1];

                    runOnUiThread(() -> chapterName.setText(chapterList_temp.get(currentIndex).text()));

                    book.updateChapter(chapter_id);
//                    storyRead_thread = new Thread(getStory);
//                    storyRead_thread.start();
                    getStory.run();
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
                    CAdapter.setItemClickListener(view -> {
                        progressDialog.show(); // 直接 show，不用切換到 UI Thread
                        isUserScrolling =false;
                        // 重置狀態
                        scrolled_history = 0;
                        chapterListDrawer.closeDrawer(GravityCompat.START);
                        currentIndex = chapterListViewR.getChildAdapterPosition(view);
                        book.updateChapter(chapterList.get(currentIndex).get(1));
                        contentAdapter.clearChapterList(); // 先清除 RecyclerView 的內容
                        Log.d("ReadActivity", "clicked item index:"+currentIndex);
                        Log.d("ReadActivity", "clicked item name:"+chapterList.get(currentIndex).get(0));
                        Log.d("ReadActivity", "clicked item id:"+chapterList.get(currentIndex).get(1));

                        // 改為 ExecutorService 處理異步爬蟲
                        executorService.submit(() -> {
                            try {
                                // 讀取當前章節
                                isPreLoading = false;
                                isLoadingPreviousChapter = false;
                                getStory.run(); // 直接執行 Runnable，不用手動 start()

                                // 回到 UI Thread 更新 RecyclerView
                                handler.post(() -> {
                                    Log.d("ReadActivity", "clicked item settext:"+chapterList.get(currentIndex).get(0));
                                    chapterName.setText(chapterList.get(currentIndex).get(0));
                                    CAdapter.updateIndex(currentIndex);
                                });

                                // **預加載前一章節**
                                isPreLoading = true;
                                isLoadingPreviousChapter = true;
                                book.updateChapter(chapterList.get(currentIndex - 1).get(1));
                                getStory.run();

                                // 最後確保 UI 更新完後關閉 Dialog
                                handler.post(() -> {
                                    if (progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });


                } catch (SocketTimeoutException e) {
                    showToast("加載失敗，請確認網路環境");
                }catch (Exception e){e.printStackTrace();}
            runOnUiThread(() -> chapterListViewR.setAdapter(CAdapter));
            runOnUiThread(() -> chapterListViewR.scrollToPosition(currentIndex-1));


            executorService.submit(() -> {
                try {
                    if (currentIndex > 0) {
                        Log.d("ReadActivity", "preload previous chapter start.");
                        isPreLoading = true;
                        isLoadingPreviousChapter = true;
                        book.updateChapter(chapterList.get(currentIndex - 1).get(1));
                        getStory.run();
                        Log.d("ReadActivity", "preload previous chapter done.");
                        handler.post(() -> {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        });
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            });
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

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }
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
//            saveTimer.cancel();
            this.finish();
        } else {
            isExit = true;
            //saveHistory();
            Toast.makeText(ReadActivity.this, "按下返回退出閱讀", Toast.LENGTH_SHORT).show();
            handler.sendEmptyMessageDelayed(9, 3000);// 3秒後發送消息
        }
    }


}