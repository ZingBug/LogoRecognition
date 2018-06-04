package com.example.lzh9619.logorecognition;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.lzh9619.logorecognition.Utils.LogoRecognition;
import com.example.lzh9619.logorecognition.Widgets.VerticalSeekBar;
import com.example.lzh9619.logorecognition.Widgets.VerticalSeekBar2;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.URL;

//AppCompatActivity
public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private String TAG="ZingBug";
    private CameraBridgeViewBase mCVCamera;
    private Button mButton;
    private boolean cur_state=false;//ture时为识别处理，false时为正常

    private Mat mRgba;//缓存相机每帧输入的数据

    private VerticalSeekBar mSeekBar;//竖直条

    private int logoSizeRange=50;//窗口调整范围

    private String xmlPath;

    private LogoRecognition logoRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制横屏

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);//后面改成打开外面连接的标志
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mCVCamera=(CameraBridgeViewBase)findViewById(R.id.camera_view);
        mCVCamera.setCvCameraViewListener(this);

        mButton=(Button)findViewById(R.id.button_recognize);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //处理
                cur_state=true;
            }
        });

        mSeekBar=(VerticalSeekBar)findViewById(R.id.bar_size);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                logoSizeRange=progress;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i("-------------","开始滑动");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i("-------------","停止滑动");
            }
        });

        xmlPath=copyXml2Data();

    }

    BaseLoaderCallback mLoaderCallback=new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG,"OpenCV loaded successfully");
                    mCVCamera.enableView();

                    logoRecognition=LogoRecognition.getInstance(xmlPath);
                    break;
                }
                default:
                    break;
            }
        }
    };
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba=new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba=inputFrame.rgba();
        int rows=mRgba.rows()-200;
        int cols=mRgba.cols();

        int scale=6;
        //Mat logoWindow=mRgba.submat(rows/2-(int)(rows*logoSize*0.01)/2,rows/2+(int)(rows*logoSize*0.01)/2,cols/2-(int)(cols*logoSize*0.01)/2,cols/2+(int)(cols*logoSize*0.01)/2);

        Mat logoWindow=mRgba.submat(rows/2-(int)(logoSizeRange*scale)/2,rows/2+(int)(logoSizeRange*scale)/2,cols/2-(int)(logoSizeRange*scale)/2,cols/2+(int)(logoSizeRange*scale)/2);
        Imgproc.rectangle(logoWindow,new Point(1,1),new Point(logoWindow.cols(),logoWindow.rows()),new Scalar(30,144,255,255),5);
        if(cur_state)
        {
            //图片处理
            String logo=logoRecognition.getLogo(logoWindow);
            Snackbar.make(getWindow().getDecorView(), getShowText(logo), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
        logoWindow.release();
        cur_state=false;
        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if(!OpenCVLoader.initDebug())// 默认加载opencv_java.so库
        {
            Log.d(TAG,"OpenCV library not found!");
        }
        else
        {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        super.onResume();
    }

    private String copyXml2Data()
    {
        InputStream in=null;
        FileOutputStream out=null;
        String path=this.getApplicationContext().getFilesDir().getAbsolutePath()+"/car.xml";
        File file=new File(path);
        if(!file.exists())
        {
            try
            {
                in=this.getAssets().open("car.xml");
                out=new FileOutputStream(file);
                int length=-1;
                byte[] buf=new byte[1024];
                while ((length=in.read(buf))!=-1)
                {
                    out.write(buf,0,length);
                }
                out.flush();
            }
            catch (Exception e)
            {
                Log.e(TAG,e.toString());
            }
            finally {
                if(in!=null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG,e.toString());
                    }
                }
                if(in!=null)
                {
                    try
                    {
                        out.close();
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG,e.toString());
                    }
                }
            }
        }
        return path;
    }

    private String getShowText(String logo)
    {
        return "本次识别LOGO为："+logo;
    }


}
