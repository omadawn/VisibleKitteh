package com.forstersfreehold.VisibleKitteh;
//-----------------------------------------
// VisibleKitteh 
// Code for The Visible Kitteh Project
// Computer vision cat head recognition and Door control to prevent my cat from entering the house with prey in his mouth.
//
//
//
//
//
//-------------------------------------------
// History
// 20120306 Initial version cobbled together from OpenCV samples
// 20120308 Lots of TODOs and comments added throughout the code. Code renamed Initial cleanup started

// TODO: Add a "Show on screen preview" mode.
// TODO: Add a "Capture successes and failures mode
// TODO: Do I want to give the option of tagging the image with the words Success and failure separately or just a checkbox for both. Gottah figure out if I want to add a small white bar to the bottom of the image so as to not write on top of learnable data.
// TODO: Update settings so that it opens a new window instead of the dialog box.
// TODO: Add a "Prevent phone from sleeping" setting. It should be enabled by default. Maybe show a warning on first use
// TODO: Review imports and objects to see what I can remove.
// TODO: Wrap all the log.i() calls in IF statements.
// TODO: Review the TODOs in the AndroidManifext.xml file
// TODO: Write test classes
// TODO: Convert to Maven project so I can have the test classes executed automagically and stuff.
// TODO: If an object is detected create a timer so we aren't triggering every milisecond or so. Make that timer do_stuff()

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class KittehBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	private static final String TAG = "VisibleKitteh::SurfaceView";
	private Camera kCamera;
	private SurfaceHolder kHolder;
	private int kFrameWidth;
	private int kFrameHeight;
	private byte[] kFrame;
	private boolean mThreadRun;

	public KittehBase(Context context) {
		super(context);
		kHolder = getHolder(); // Returns a holder object that allows you to
								// manipulate an android 'surface' which is what
								// displays stuff on the screen.
		kHolder.addCallback(this); // Not sure why this is here yet but
									// apparently it's how you use the
									// survaceView interface
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	public int getFrameWidth() {
		return kFrameWidth;
	}

	public int getFrameHeight() {
		return kFrameHeight;
	}

	// surfaceCreated, surfaceChanged and surfaceDestroyed are three interface
	// methods that you create when using surfaceView.
	// They will be called when the surface (the display window) is initially
	// built in this case shortly after you start the application,) Modified, or
	// destroyed (when you close the application.)

	public void surfaceChanged(SurfaceHolder _holder, int format, int width,
			int height) {
		Log.i(TAG, "surfaceChanged");
		if (kCamera != null) {
			Camera.Parameters params = kCamera.getParameters();
			List<Camera.Size> sizes = params.getSupportedPreviewSizes();
			kFrameWidth = width;
			kFrameHeight = height;

			// selecting optimal camera preview size
			{
				double minDiff = Double.MAX_VALUE;
				for (Camera.Size size : sizes) {
					if (Math.abs(size.height - height) < minDiff) {
						kFrameWidth = size.width;
						kFrameHeight = size.height;
						minDiff = Math.abs(size.height - height);
					}
				}
			}

			params.setPreviewSize(getFrameWidth(), getFrameHeight());
			kCamera.setParameters(params);
			try {
				kCamera.setPreviewDisplay(null);
			} catch (IOException e) {
				Log.e(TAG, "Unable to set preview display: " + e);
			}
			kCamera.startPreview();
		}
	}

	// Let's grab ahold of the camera and create a callback
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated");
		kCamera = Camera.open();
		kCamera.setPreviewCallback(new PreviewCallback() { // This creates a
															// preview callback.
															// Android will then
															// call
															// onPreviewFrame
															// for every frame
															// that is captured
															// by the camera.
			
			// This is defining the methad that will be called as a callback
			public void onPreviewFrame(byte[] data, Camera camera) { // Which is
																		// why
																		// we
																		// need
																		// to
																		// define
																		// onPreviewFrame
																		// to
																		// define
																		// what
																		// we
																		// are
																		// going
																		// to do
																		// with
																		// these
																		// images.
				synchronized (KittehBase.this) { // is KittehBase. redundant
													// here?
					kFrame = data; // Data is where android has stuffed the
									// image that the camera has returned.
					// Now trigger the image proccessing portion of the main
					// runable thread
					KittehBase.this.notify();
				}
			}
		});
		(new Thread(this)).start();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG, "surfaceDestroyed");
		mThreadRun = false;
		if (kCamera != null) {
			synchronized (this) {
				// The gui portion of the app has shut down. Let's stop pulling input from the camera and release it. Remember in Androidland (tm) I just made that up, the gui is an 'Activity' and separate from what we would normally think of as a 'program' or an 'application'
				// I'm not 100% clear yet but taking this part out might allow
				// us to 'close' the app, which really is just getting rid of
				// the gui portion. But continue to capture from the camera,
				// write to storage (I wrote disk there for a second) and
				// generally muck about. We would have to move this into a
				// shutDown() or stopProcessing() method which we could call
				// from the GUI.
				// I would also want to muck about with the preview generating stuff so I'm not making bitmaps and writing on frames that no-one will ever see.
				kCamera.stopPreview();
				kCamera.setPreviewCallback(null);
				kCamera.release();
				kCamera = null;
			}
		}
	}

	// I really have no idea what this does. I don't quite get abstracted methods yet.
	protected abstract Bitmap processFrame(byte[] data);

	public void run() {
		mThreadRun = true;
		Log.i(TAG, "Starting processing thread");
		while (mThreadRun) {
			Bitmap bmp = null;

			synchronized (this) {
				try {
					this.wait();
					bmp = processFrame(kFrame); // processFrame is defined in KittehAndroidView and is the method that actually looks at a video fram for a detectable object. It returns a bitmap which will contain rectangles drawn around matched objects. We will then layer these ractangles over the preview video.
				} catch (InterruptedException e) {
					// This seems to be one of those rare cases where a stack trace IS actually the appropriate way to handle an exception.
					Log.i(TAG, "Thread Interrupted");
					e.printStackTrace();
				}
			}

			// If we have an image we can draw on it.
			if (bmp != null) {
				// A canvas is an additional layer that can be added on top of a
				// surface and drawn upon.
				Canvas canvas = kHolder.lockCanvas();
				if (canvas != null) {
					// bmp is a bitmap image of just rectangles which we have drawn around where detected objects are. Now we're going to hand this to drawBitmap which will render it on our canvas. Our canvas if you remember is an extra layer on top of the existing surface which is currently populated with a preview image from the camera.
					canvas.drawBitmap(bmp,
							(canvas.getWidth() - getFrameWidth()) / 2,
							(canvas.getHeight() - getFrameHeight()) / 2, null);
					kHolder.unlockCanvasAndPost(canvas);
				}
				bmp.recycle();
			}
		}
	}
}