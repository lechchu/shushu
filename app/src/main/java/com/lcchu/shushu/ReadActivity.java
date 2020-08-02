package com.lcchu.shushu;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
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

import com.google.android.material.navigation.NavigationView;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;





public class ReadActivity extends AppCompatActivity {

    String bookName;
    //search url https://tw.ttkan.co/novel/search?q="
    int currentIndex = 0;
    int scorllHistory = 0;
    int textSize = 24;

    boolean isExit=false;

    BookData book;


    ChapterListAdapter CAdapter;

    ArrayList<ArrayList<String>> chapterList = new ArrayList<ArrayList<String>>();
    ArrayList<String> chapterData = new ArrayList<String>();

    Thread chapterLoad, stroyRead;

    AlertDialog settingDialog;

//UI


    View setting_layout,story_layout;

    SmartRefreshLayout switchChapter;
    TextView tv1, temptxt, chapterName, txtsizeView;
    ImageView bookCover;
    ScrollView storyScrollView,menuScrollView;
    NavigationView chapterListView, settingView;
    RecyclerView chapterListViewR;
    DrawerLayout chapterListDrawer;
    SeekBar editfontsize;
    Switch darkmodeSwitch;
    ProgressDialog loadingDialog;

    Elements title = new Elements();
    Bitmap cover;



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

    public void setupView(){
        bookCover = (ImageView)findViewById(R.id.cover_imageView);

        setting_layout = LayoutInflater.from(ReadActivity.this).inflate(R.layout.setting, null);
        editfontsize = (SeekBar) setting_layout.findViewById(R.id.fontsize_seekbar);
        darkmodeSwitch = (Switch) setting_layout.findViewById(R.id.switch1);

        txtsizeView = (TextView)setting_layout.findViewById(R.id.fontsize_textview);
        chapterName = (TextView)findViewById(R.id.chapternameView);
        tv1 = (TextView)findViewById(R.id.textView);
        editfontsize.setProgress(textSize);
        txtsizeView.setText(String.valueOf(textSize));

        switchChapter = (SmartRefreshLayout)findViewById(R.id.loadLayout);
        switchChapter.setEnableLoadMore(true);
        switchChapter.setEnableRefresh(true);
        switchChapter.setEnableAutoLoadMore(false);



        storyScrollView = (ScrollView)findViewById(R.id.storyscroll);
        story_layout = (RelativeLayout)findViewById(R.id.story_layout);

        chapterListView = (NavigationView)findViewById(R.id.chapterlist_navigation_view);
        settingView = (NavigationView)findViewById(R.id.setting_navigation_view);

        chapterListDrawer = (DrawerLayout)findViewById(R.id.drawerLayout);
        chapterListViewR = (RecyclerView)findViewById(R.id.chapterlist_RecyclerView);

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
                    scorllHistory = 0;
                    book.updateChapter(chapterList.get(--currentIndex).get(1));
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
            public void onLoadMore(RefreshLayout refreshLayout) {
                if(currentIndex+1>=chapterList.size())
                    Toast.makeText(ReadActivity.this,"已是最後一章",Toast.LENGTH_LONG).show();
                else {
                    scorllHistory = 0;
                    book.updateChapter(chapterList.get(++currentIndex).get(1));

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
                textSize = seekBar.getProgress();
                handler.sendEmptyMessage(4);
            }
        });

        darkmodeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    //storyScrollView.setBackgroundColor(Color.parseColor("#2C3E50"));

                    story_layout.setBackground(new ColorDrawable(Color.parseColor("#2C3E50")));
                    tv1.setTextColor(Color.parseColor("#EAECEE"));
                }else {
                    //storyScrollView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                    story_layout.setBackground(new ColorDrawable(Color.parseColor("#FFFFFF")));

                    tv1.setTextColor(Color.parseColor("#666666"));
                }
                }


        });

        chapterListViewR.addOnItemTouchListener(new RecyclerItemClickListener(this, chapterListViewR ,new RecyclerItemClickListener.OnItemClickListener() {
            @Override public void onItemClick(View view, int position) {


                //Toast.makeText(MainActivity.this,String.valueOf(view),Toast.LENGTH_LONG).show();
                //關側滑
                scorllHistory = 0;
                chapterListDrawer.closeDrawer(GravityCompat.START);
                currentIndex = position;
                book.updateChapter(chapterList.get(currentIndex).get(1));
                new Thread(getStory).start();

                /*
                TextView tempTxt = (TextView) findViewById((int) chapterListViewR.getChildItemId(view));
                tempTxt.setTextColor(Color.parseColor("#FF0000"));
*/
            }

            @Override public void onLongItemClick(View view, int position) {
                // do whatever
            }
        }));


    }

    public void readCache(){

        try {
            FileInputStream fis = openFileInput(bookName+".cache");
            byte[] readBytes = new byte[fis.available()];
            fis.read(readBytes);
            String readString = new String(readBytes);
            String []data = readString.split(",");

            currentIndex = Integer.valueOf(data[0]);
            scorllHistory = Integer.valueOf(data[1]);

            fis.close();


            fis = openFileInput("user_setting.cache");
            textSize = fis.read();
            fis.close();
            handler.sendEmptyMessage(4);

        } catch (Exception e) {
            e.printStackTrace();
            saveHistory();
            saveSetting();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("努力加載中");
        loadingDialog.show();

        System.out.println("started readactivty");
        bookName = getIntent().getExtras().getString("bookName");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_main);

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
        readCache();
        t2 = System.currentTimeMillis();
        System.out.println("read cache time:"+(t2-t1));

        book = new BookData(bookName,"0");
/*
        chapterLoad = new Thread(JsonReader);

        try{
            chapterLoad.start();
            chapterLoad.join();
        }catch (Exception e){e.printStackTrace();}
*/

        new Thread(JsonReader).start();



        //new Thread(getCover).start();
        //new Thread(getStory).start();
    }

    public void saveHistory(){
        try{
            String a = String.valueOf(currentIndex)+","+String.valueOf(storyScrollView.getScrollY());

            FileOutputStream fos  = openFileOutput(bookName+".cache", Context.MODE_PRIVATE);


            fos.write(a.getBytes());
            fos.close();

        }catch (Exception e){e.printStackTrace();}
    }

    public void saveSetting(){
        try{
            FileOutputStream fos  = openFileOutput("user_setting.cache", Context.MODE_PRIVATE);
            fos.write(textSize);
            fos.close();
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
                //saveHistory();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("start send story message");
            handler.sendEmptyMessage(0);
        }
    };

    Runnable JsonReader = new Runnable(){
        @Override
        public void run() {


                //conn.header("User-Agent","Mozilla/5.0 (X11; Linux x86_64; rv:32.0) Gecko/   20100101 FireFox/32.0");

                try {

                    //TODO 加速讀章節
                    long t1,t2;


t1=System.currentTimeMillis();
                    Connection conn = Jsoup.connect(book.getChapterListURL()).ignoreContentType(true);
                    chapterList = new ArrayList<ArrayList<String>>();
                    JSONArray chapterJson = new JSONObject(conn.get().text()).getJSONArray("items");

                    book.updateChapter(chapterJson.getJSONObject(currentIndex).getString("chapter_id"));
                    System.out.println("make getStory start");
                    new Thread(getStory).start();
                    for(int i=0;i<chapterJson.length();i++){


                        chapterData = new ArrayList<String>();
                        chapterData.add(chapterJson.getJSONObject(i).getString("chapter_name"));
                        chapterData.add(chapterJson.getJSONObject(i).getString("chapter_id"));
                        chapterList.add(chapterData);

                    }
                    t2=System.currentTimeMillis();
                    System.out.println("load chapter time:"+(t2-t1));

                    //chapterMenu = chapterListView.getMenu();



                    CAdapter = new ChapterListAdapter(chapterList);


                }catch (Exception e){e.printStackTrace();}

            handler.sendEmptyMessage(1);
        }
    };

    Runnable getCover = new Runnable(){
        @Override
        public void run() {
            try {
                InputStream in = new java.net.URL(book.getCoverURL()).openStream();
                cover = BitmapFactory.decodeStream(in);
                handler.sendEmptyMessage(2);
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

            System.exit(0);// 使虛擬機停止運行並退出程序
        } else {
            isExit = true;
            saveHistory();
            Toast.makeText(ReadActivity.this, "按下返回退出閱讀", Toast.LENGTH_SHORT).show();
            handler.sendEmptyMessageDelayed(9, 3000);// 3秒後發送消息
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.in, R.anim.out);
    }

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
                        break;
                    case 2:
                        //bookCover.setImageBitmap(cover);
                        break;
                    case 3:
                        tv1.setVisibility(View.INVISIBLE);
                        break;
                    case 4:
                        tv1.setTextSize(textSize);
                        break;
                    case 5:
                        storyScrollView.scrollTo(0,scorllHistory);
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