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

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class KittehAndroidView extends KittehBase {
	private static final String TAG = "VisibleKitteh::View";
	private Mat kYUV;
	private Mat kRGB;
	private Mat kGreyScale;
	public static float kMinObjectSize = 0.5f; // Anything smaller than this (roughly) will be ignored. I dunno what "f" is yet. I think I'm going to change this to just ObjectSize and make it not setable. Either that or make it setable in the preferences window?
	private CascadeClassifier kCascade;
	private Scalar kRectColor = new Scalar(0, 255, 0, 255); // The color of the box we are going to draw around discovered objects.
	// TODO: Figure out how to have these set in the properties menu
	private Scalar kTitleColor = new Scalar(255, 0, 0, 255);
	private String kTitleText = "Miscreant Detector";

	public KittehAndroidView(Context context) {
		super(context);
		try {
			// Open the pre-trained Haar Cascade Classifier (or boosted classifier) file. See the training section of the tutorial on how to do this.
			// This is a tiny bit of a hack. Theresources we include in our apk are available as raw data but cascadeClassifier only takes a path to a file so we are creating one.
			InputStream is = context.getResources().openRawResource(
					R.raw.cascade);
			File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
			File cascadeFile = new File(cascadeDir, "cascade.xml");
			FileOutputStream os = new FileOutputStream(cascadeFile);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();

			// Create a Cascade Classifier object from the xml file
			kCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
			// Make sure we succeeded
			if (kCascade.empty()) {
				Log.e(TAG, "Failed to load cascade classifier");
				kCascade = null;
			} else
				Log.i(TAG,
						"Loaded cascade classifier from "
								+ cascadeFile.getAbsolutePath());

			cascadeFile.delete();
			cascadeDir.delete();

		} catch (IOException e) {
			// e.printStackTrace();
			Log.e(TAG, "Failed to load cascade. IO Exception: " + e);
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		// Call the main surfacChanged method from KittehBase with the exact
		// same values that we got.
		super.surfaceChanged(_holder, format, width, height);

		// But then..
		synchronized (this) {
			// initialize Mats before usage
			kYUV = new Mat(getFrameHeight() + getFrameHeight() / 2,
					getFrameWidth(), CvType.CV_8UC1);
			kGreyScale = kYUV.submat(0, getFrameHeight(), 0, getFrameWidth()); // Generate a greyscale version of the image. This will simplify object recognition
			kRGB = new Mat(); // An RGB version of the image will be used for human viewing
		}
	}

	@Override
	protected Bitmap processFrame(byte[] data) {
		kYUV.put(0, 0, data);
		// TODO: Add a 'mode' part (capture/log, door trigger)
		// TODO: if not displaying can I just use kRGB or something instead of having ot use Imgproc?
		Imgproc.cvtColor(kYUV, kRGB, Imgproc.COLOR_YUV420sp2RGB, 4);
		Core.putText(kRGB, kTitleText, new Point(10, 100),
				3/* CV_FONT_HERSHEY_COMPLEX */, 1.6, kTitleColor, 2);
		// TODO: This is also what I would use if I wanted to tag saved images with 'Success' or 'Failure'

		if (kCascade != null) {
			int height = getFrameWidth();
			int objectSize = Math.round(height * kMinObjectSize); // This is faceSize in the original OpenCV Face detection source. It's the smallest size object to detect. Anything smaller will be ignored.
			List<Rect> objects = new LinkedList<Rect>(); // objects is a list of OpenCV Rect objects or rectangles. This is where detectMultiScale will end up STUFFING the objects it has detected into.

			// MAGIC HAPPENS  Here's where we actually do the detection detecMultiscale detects objects of unspecified size and returns them as a list of rectangels
			kCascade.detectMultiScale(kGreyScale, objects, 1.1, 2, 2,
					new Size(objectSize, objectSize));
			// TODO: Update this so that the rectangle is the correct size.
			// TODO: Only show the rectangle if we are in
			// "Sow on screen preview" mode.
			for (Rect r : objects)
				Core.rectangle(kRGB, r.tl(), r.br(), kRectColor, 3);
			// TODO: trigger a door open if objects > 0 or something like that.
		}

		Bitmap bmp = Bitmap.createBitmap(getFrameWidth(), getFrameHeight(),
				Bitmap.Config.ARGB_8888);

		if (Utils.matToBitmap(kRGB, bmp))
			return bmp;

		bmp.recycle();
		return null;
	}

	@Override
	public void run() {
		super.run();

		synchronized (this) {
			// Explicitly deallocate Mats
			if (kYUV != null)
				kYUV.release();
			if (kRGB != null)
				kRGB.release();
			if (kGreyScale != null)
				kGreyScale.release();
			kYUV = null;
			kRGB = null;
			kGreyScale = null;
		}
	}
}
