package com.forstersfreehold.VisibleKitteh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.CvType;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
//import com.forstersfreehold.samples.fd.FdActivity;
//import com.forstersfreehold.samples.fd.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class KittehAndroidView extends KittehBase {
    private static final String TAG = "VisibleKitteh::View";
    private Mat mYuv;
    private Mat mRgba;
    private Mat mGraySubmat;
    private Mat mIntermediateMat;
    public static float         minObjectSize = 0.5f; // Anything smaller than this (roughly) will be ignored. I dunno what "f" is yet. I think I'm going to change this to just ObjectSize and make it not setable. Either that or make it setable in the preferences window? 
    private CascadeClassifier   mCascade;
    private Scalar mRectColor = new Scalar(0, 255, 0, 255); // The color of the box we are going to draw around discovered objects.
    // TODO: Figure out how to have this set in the properties menu
    private Scalar mTitleColor = new Scalar(255, 0, 0, 255);
    private String mTitleText = "Miscreant Detector";

    public KittehAndroidView(Context context) {
        super(context);
        try {
        
        	// Open the pre-trained Haar Cascade Classifier (or boosted classifier) file. See the training section of the tutorial on how to do this.
            InputStream is = context.getResources().openRawResource(R.raw.cascade);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "cascade.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile); // What? Output?

            byte[] buffer = new byte[4096];
            int bytesRead;
            // Uh, do we really open the file, read it and re-write it back to the original file? I must be misunderstanding this.
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            // Create a Cascade Classifier object from the xml file
            mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            // Make sure we succeeded
            if (mCascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mCascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

            cascadeFile.delete();
            cascadeDir.delete();

        } catch (IOException e) {
            // e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. IO Exception: " + e);
        }
        
        
    }

    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
    	// Call the main surfacChanged method from KittehBase with the exact same values that we got.
        super.surfaceChanged(_holder, format, width, height);

        // But then.. 
        synchronized (this) {
            // initialize Mats before usage
        	// what's Yuv?
            mYuv = new Mat(getFrameHeight() + getFrameHeight() / 2, getFrameWidth(), CvType.CV_8UC1);
            mGraySubmat = mYuv.submat(0, getFrameHeight(), 0, getFrameWidth()); // Generate a greyscale version of the image. This will simplify object recognition
            mRgba = new Mat(); // An RGB version of the image will be used for human viewing
            mIntermediateMat = new Mat(); // Dunno what this is for
        }
    }

    @Override
    protected Bitmap processFrame(byte[] data) {
        mYuv.put(0, 0, data);
        
        // TODO: get rid of the view parts and replace with a 'mode' part (capture/log, door trigger)
        //      add another setting for output to screen
//        switch (KittehActivity.viewMode) {
//        case KittehActivity.VIEW_MODE_GRAY:
//            Imgproc.cvtColor(mGraySubmat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
//            break;
//        case KittehActivity.VIEW_MODE_RGBA:
//            Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
//            // TODO: This is what I would use if I wanted to tag saved images with 'Success' or 'Failure'
//            Core.putText(mRgba, "OpenCV + Android", new Point(10, 100), 3/* CV_FONT_HERSHEY_COMPLEX */, 2, new Scalar(255, 0, 0, 255), 3);
//            break;
//        case KittehActivity.VIEW_MODE_CANNY:
//            Imgproc.Canny(mGraySubmat, mIntermediateMat, 80, 100);
//            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);
//            break;
//        }
        
        // TODO: if not displaying can I just use mRgba or something instead of having ot use Imgproc?
        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV420sp2RGB, 4);
        Core.putText(mRgba, mTitleText, new Point(10, 100), 3/* CV_FONT_HERSHEY_COMPLEX */, 1.6, mTitleColor, 2);
        // TODO: This is also what I would use if I wanted to tag saved images with 'Success' or 'Failure'
        
        if (mCascade != null) {
            int height = getFrameWidth();
            int objectSize = Math.round(height * minObjectSize); // This is faceSize in the original OpenCV Face detection source. It's the rough size to expect the face to be on the screen so we can tune the size of the rectangle to draw around it.
            List<Rect> objects = new LinkedList<Rect>(); // objects is a list of OpenCV Rect objects or rectangles.This is where detectMultiScale will end up STUFFING the objects it has detected into. 
            
            // MAGIC HAPPENS Here's where we actually do the detection detecMultiscale detects objects of unspecified size and returns them as a list of rectangels
            mCascade.detectMultiScale(mGraySubmat, objects, 1.1, 2, 2 
                    , new Size(objectSize, objectSize));
            // TODO: Update this so that the rectangle is the correct size.
            // TODO: Only show the rectangle if we are in "Sow on screen preview" mode.
            for (Rect r : objects)
                Core.rectangle(mRgba, r.tl(), r.br(), mRectColor, 3);
            // TODO: trigger a door open if objects > 0 or something like that.
        }

        Bitmap bmp = Bitmap.createBitmap(getFrameWidth(), getFrameHeight(), Bitmap.Config.ARGB_8888);

        if (Utils.matToBitmap(mRgba, bmp))
            return bmp;

        bmp.recycle();
        return null;
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mYuv != null)
                mYuv.release();
            if (mRgba != null)
                mRgba.release();
            if (mGraySubmat != null)
                mGraySubmat.release();
            if (mIntermediateMat != null)
                mIntermediateMat.release();

            mYuv = null;
            mRgba = null;
            mGraySubmat = null;
            mIntermediateMat = null;
        }
    }
}
