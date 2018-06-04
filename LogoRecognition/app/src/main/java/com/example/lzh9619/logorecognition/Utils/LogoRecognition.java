package com.example.lzh9619.logorecognition.Utils;

import android.os.Environment;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.ANN_MLP;
import org.opencv.objdetect.HOGDescriptor;

public class LogoRecognition {
    //singleton 模式

    private ANN_MLP ann;
    //图片特征数
    //本实验检测窗口定为64×64，就是整张图片的大小，块大小16×16，胞元8×8，步进8×8，这样一张图片就有((64-16)/8+1)*((64-16)/8+1)*(16*16/(8*8)*9=1764维特征
    private  int feature_num=1764;
    //样本种类
    private  final int class_num=5;
    private  String[] logoLabels={"雪铁龙","大众","一汽","福田","本田"};

    //特征集
    private  MatOfFloat descriptors;
    private  MatOfPoint locations;

    private static LogoRecognition INSTANCE=null;

    private LogoRecognition(String xmlPath)
    {
        ann=ANN_MLP.load(xmlPath);
    }

    public static LogoRecognition getInstance(String xmlPath)
    {
        if(INSTANCE==null)
        {
            INSTANCE=new LogoRecognition(xmlPath);
        }
        return INSTANCE;
    }

    private void getHOG(Mat image)
    {
        descriptors=new MatOfFloat();
        locations=new MatOfPoint();
        HOGDescriptor hog=new HOGDescriptor(new Size(64,64),new Size(16,16),new Size(8,8),new Size(8,8),9);
        hog.compute(image,descriptors,new Size(64,64),new Size(0,0),locations);
    }

    public String getLogo(Mat image)
    {
        Imgproc.resize(image,image,new Size(64,64));
        Imgproc.cvtColor(image,image,Imgproc.COLOR_RGB2GRAY);
        Mat sample=new Mat(1,feature_num, CvType.CV_32FC1);
        getHOG(image);
        int cur=0;
        for(float d:descriptors.toArray())
        {
            sample.put(0,cur++,d);
        }
        Mat predict=new Mat(1,class_num,CvType.CV_32FC1);
        ann.predict(sample,predict,0);
        Core.MinMaxLocResult maxLoc=Core.minMaxLoc(predict);
        int index=(int)maxLoc.maxLoc.x;
        return logoLabels[index];
    }
}
