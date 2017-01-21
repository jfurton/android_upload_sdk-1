/*
 * Copyright (c) 2016 Samsung Electronics America
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.samsung.msca.samsungvr.sampleapp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.UserLiveEventSegment;
import com.samsung.msca.samsungvr.sdk.VR;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * See https://github.com/googlesamples/android-Camera2Video/blob/master/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.java
 */

public class PublishLiveEventFromCamFragment extends BaseFragment {


    static final String TAG = Util.getLogTag(PublishLiveEventFromCamFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private ViewGroup mCameraList;
    private TextView mIdView;
    private LayoutInflater mLayoutInflator;
    private CameraManager mCameraManager;
    private AutoFitTextureView mPreview;
    private View mStartStream, mStopStream;

    private Handler mMainHandler, mThreadHandler;
    private HandlerThread mThread;

    private UserLiveEvent mUserLiveEvent = null;

    static final String PARAM_LIVE_EVENT_ID = "paramLiveEventId";

    private final UserLiveEvent.Result.QueryLiveEvent mCallbackFindLiveEvent = new UserLiveEvent.Result.QueryLiveEvent() {

        @Override
        public void onSuccess(Object o, UserLiveEvent userLiveEvent) {
            mUserLiveEvent = userLiveEvent;
            if (hasValidViews()) {
                mIdView.setText(userLiveEvent.getId());
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            mUserLiveEvent = null;
            if (hasValidViews()) {
                Resources res = getActivity().getResources();
                String text = String.format(res.getString(R.string.failure_with_status), status);
                mIdView.setText(text);
            }
        }

        @Override
        public void onCancelled(Object o) {
            mUserLiveEvent = null;
        }

        @Override
        public void onException(Object o, Exception ex) {
            mUserLiveEvent = null;
            if (hasValidViews()) {
                Resources res = getActivity().getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mIdView.setText(text);
            }
        }
    };

    private CameraManager.AvailabilityCallback mCameraAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        private View viewForCamera(String cameraId) {
            if (!hasValidViews()) {
                return null;
            }
            for (int i = mCameraList.getChildCount() - 1; i >= 0; i -= 1) {
                View v = mCameraList.getChildAt(i);
                if (cameraId.equals(v.getTag())) {
                    return v;
                }
            }
            View result = mLayoutInflator.inflate(R.layout.camera_list_item, null, false);
            result.setTag(cameraId);
            result.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (hasValidViews()) {
                        Object tag = v.getTag();
                        if (tag instanceof String) {
                            setActiveCamera((String)tag);
                        }
                    }
                }
            });
            mCameraList.addView(result);
            TextView camIdView = (TextView)result.findViewById(R.id.cam_id);
            camIdView.setText(cameraId);
            return result;
        }

        @Override
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);

            if (DEBUG) {
                Log.d(TAG, "onCameraAvailable: " + cameraId);
            }

            View camView = viewForCamera(cameraId);
            if (null != camView) {
                camView.setEnabled(true);
            }
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
            if (DEBUG) {
                Log.d(TAG, "onCameraUnavailable: " + cameraId);
            }
            View camView = viewForCamera(cameraId);
            if (null != camView) {
                camView.setEnabled(false);
            }
        }
    };

    private CameraDevice mCameraDevice = null;
    private MediaRecorder mMediaRecorder = null;

    private CameraCaptureSession.CaptureCallback mCameraCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            if (false && DEBUG) {
                Log.d(TAG, "onCaptureStarted session: " + session + " request: " + request +
                    " timestamp: " + timestamp + " frameNumber: " + frameNumber);
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            if (DEBUG) {
                Log.d(TAG, "onCaptureProgressed session: " + session + " request: " + request + " partialResult: " + partialResult);
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if (false && DEBUG) {
                Log.d(TAG, "onCaptureCompleted session: " + session + " request: " + request + " result: " + result);
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            if (DEBUG) {
                Log.d(TAG, "onCaptureFailed session: " + session + " request: " + request + " failure: " + failure);
            }
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
            if (DEBUG) {
                Log.d(TAG, "onCaptureSequenceCompleted session: " + session + " sequenceId: " + sequenceId + " frameNumber: " + frameNumber);
            }
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
            if (DEBUG) {
                Log.d(TAG, "onCaptureSequenceAborted session: " + session + " sequenceId: " + sequenceId);
            }
        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureStateCallback = new CameraCaptureSession.StateCallback() {

        private boolean startCaptureInternal(CameraCaptureSession session) {
            try {
                CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                builder.addTarget(mPreview.getSurface());
                builder.addTarget(mMediaRecorder.getSurface());
                session.setRepeatingRequest(builder.build(), mCameraCaptureCallback, mMainHandler);

                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            super.onReady(session);
            if (DEBUG) {
                Log.d(TAG, "onReady session: " + session);
            }
            startCaptureInternal(session);
        }

        @Override
        public void onActive(CameraCaptureSession session) {
            super.onActive(session);
            if (DEBUG) {
                Log.d(TAG, "onActive session: " + session);
            }
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            super.onClosed(session);
            if (DEBUG) {
                Log.d(TAG, "onClosed session: " + session);
            }
        }

        @Override
        public void onSurfacePrepared(CameraCaptureSession session, Surface surface) {
            super.onSurfacePrepared(session, surface);
            if (DEBUG) {
                Log.d(TAG, "onSurfacePrepared session: " + session + " surface: " + surface);
            }
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            if (DEBUG) {
                Log.d(TAG, "onConfigured session: " + session);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            if (DEBUG) {
                Log.d(TAG, "onConfigureFailed session: " + session);
            }
        }
    };

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            if (DEBUG) {
                Log.d(TAG, "onClosed camera: " + camera);
            }

            if (camera == mCameraDevice) {
                stopCamStream();
            }

        }

        @Override
        public void onOpened(CameraDevice camera) {
            if (DEBUG) {
                Log.d(TAG, "onOpened camera: " + camera + " id: " + camera.getId());
            }
            mCameraDevice = camera;
            try {
                mCameraDevice.createCaptureSession(Arrays.asList(mPreview.getSurface(), mMediaRecorder.getSurface()),
                        mCameraCaptureStateCallback, mMainHandler);
            } catch (CameraAccessException ex) {
                Log.e(TAG, "onOpened", ex);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            if (DEBUG) {
                Log.d(TAG, "onDisconnected camera: " + camera);
            }

            if (camera == mCameraDevice) {
                stopCamStream();
            }

        }

        @Override
        public void onError(CameraDevice camera, int error) {
            if (DEBUG) {
                Log.d(TAG, "onError camera: " + camera);
            }

            if (camera == mCameraDevice) {
                stopCamStream();
            }
        }
    };

    private ParcelFileDescriptor[] mPipe = null;

    private Size getBestSize(Size[] allowedSizes) {
        Size result = null;

        if (null == allowedSizes) {
            result = null;
        } else {
            int len = allowedSizes.length;
            for (int i = 0; i < len; i += 1) {
                Size size = allowedSizes[i];
                if (DEBUG) {
                    Log.d(TAG, "Size at " + i + " = " + size);
                }
                if (null == result) {
                    result = size;
                } else {
                    if ((size.getHeight() * size.getWidth()) >
                            result.getHeight() * result.getWidth()) {
                        result = size;
                    }
                }
            }
        }
        Log.d(TAG, "Best size: " + result);
        return result;
    }

    private UserLiveEventSegment mSegment = null;

    private UserLiveEvent.Result.UploadSegment mUploadCallback = new UserLiveEvent.Result.UploadSegment() {

        @Override
        public void onSuccess(Object closure) {
            if (DEBUG) {
                Log.d(TAG, "uploadCallback onSuccess: " + closure);
            }
            mSegment = null;
        }

        @Override
        public void onProgress(Object closure, float progressPercent) {
            if (DEBUG) {
                Log.d(TAG, "uploadCallback onProgress: " + closure + " % " + progressPercent);
            }

        }

        @Override
        public void onFailure(Object closure, int status) {
            if (DEBUG) {
                Log.d(TAG, "uploadCallback onFailure: " + closure + " status " + status);
            }
            mSegment = null;
        }

        @Override
        public void onCancelled(Object closure) {
            if (DEBUG) {
                Log.d(TAG, "uploadCallback onCancelled: " + closure);
            }
            mSegment = null;
        }

        @Override
        public void onException(Object closure, Exception ex) {
            if (DEBUG) {
                Log.d(TAG, "uploadCallback onException: " + closure);
            }
            mSegment = null;
        }

        @Override
        public void onSegmentIdAvailable(Object closure, UserLiveEventSegment segment) {
            if (DEBUG) {
                Log.d(TAG, "uploadCallback onSegmentIdAvailable: " + closure + " segment: " + segment);
            }
            try {
                String active = getActiveCameraId();

                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(active);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size bestSize = getBestSize(map.getOutputSizes(MediaRecorder.class));
                if (null != bestSize && resetMediaRecorder(bestSize)) {
                    mPreview.setSize(bestSize);
                    mSegment = segment;
                    return;
                }
            } catch (CameraAccessException ex) {
                Log.d(TAG, "startCamStream", ex);
            }
            segment.cancelUpload(null);
        }
    };

    private boolean startCamStream() {
        stopCamStream();

        if (!hasValidViews()) {
            return false;
        }
        String active = getActiveCameraId();
        if (null == active) {
            return false;
        }
        try {
            mPipe = ParcelFileDescriptor.createPipe();
        } catch (IOException ex) {
            return false;
        }
        if (!mUserLiveEvent.uploadSegmentFromFD(mPipe[0], mUploadCallback, null, null)) {
            return false;
        }
        mStartStream.setEnabled(false);
        mStopStream.setEnabled(true);
        return true;
    }

    private void stopCamStream() {
        if (null != mMediaRecorder) {
            try {
                mMediaRecorder.stop();
            } catch (Exception ex) {
            }
        }

        if (null != mPipe && 2 == mPipe.length) {
            for (int i = 0; i < 2; i += 1) {
                if (null != mPipe[i]) {
                    try {
                        mPipe[i].close();
                    } catch (IOException ex) {
                    }
                    mPipe[i] = null;
                }
            }
            mPipe = null;
        }


        if (hasValidViews()) {
            mPreview.setSize(null);
            mStartStream.setEnabled(null != getActiveCameraId());
            mStopStream.setEnabled(false);
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }


    private String getActiveCameraId() {
        if (!hasValidViews()) {
            return null;
        }
        for (int i = mCameraList.getChildCount() - 1; i >= 0; i -= 1) {
            View v = mCameraList.getChildAt(i);
            if (!v.isSelected()) {
                continue;
            }
            Object tag = v.getTag();
            if (!(tag instanceof String)) {
                continue;
            }
            return (String)tag;
        }
        return null;
    }

    private boolean setActiveCamera(String cameraId) {
        if (!hasValidViews()) {
            return false;
        }
        String active = getActiveCameraId();
        if (cameraId == active) {
            return false;
        }

        stopCamStream();
        mStartStream.setEnabled(false);
        mStopStream.setEnabled(false);

        View match = null;

        for (int i = mCameraList.getChildCount() - 1; i >= 0; i -= 1) {
            View v = mCameraList.getChildAt(i);
            v.setSelected(false);

            Object tag = v.getTag();
            if (tag instanceof String) {
                String cameraId2 = (String)tag;
                if (cameraId2.equals(cameraId)) {
                    match = v;
                }
            }
        }
        if (null != match) {
            match.setSelected(true);
            mStartStream.setEnabled(true);
            return true;
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainHandler = new Handler(Looper.getMainLooper());
        mCameraManager = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCameraManager = null;
        mMainHandler = null;
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceTextureAvailable " + surfaceTexture + " " + width + " " + height);
            }
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCameraManager.registerAvailabilityCallback(mCameraAvailabilityCallback, null);
                }
            });
        }

        private void onSurfaceTextureSizeChangedInternal(SurfaceTexture surfaceTexture,
                                                final int width, final int height) {
            String active = getActiveCameraId();
            if (0 == width || 0 == height || null == active) {
                stopCamStream();
                return;
            }

            try {
                mCameraManager.openCamera(active, mCameraStateCallback, mMainHandler);
            } catch (CameraAccessException ex) {
                Log.d(TAG, "openCamera", ex);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(final SurfaceTexture surfaceTexture,
                                                final int width, final int height) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceTextureSizeChanged " + surfaceTexture + " " + width + " " + height);
            }
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    onSurfaceTextureSizeChangedInternal(surfaceTexture, width, height);
                }
            });
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            if (DEBUG) {
                Log.d(TAG, "onSurfaceTextureDestroyed " + surfaceTexture);
            }

            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCameraManager.unregisterAvailabilityCallback(mCameraAvailabilityCallback);
                    setActiveCamera(null);
                }
            });

            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
            if (false && DEBUG) {
                Log.d(TAG, "onSurfaceTextureUpdated " + surfaceTexture);
            }

        }

    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLayoutInflator = LayoutInflater.from(getActivity());

        View result = mLayoutInflator.inflate(R.layout.fragment_publish_live_event_from_cam, null, false);

        mCameraList = (ViewGroup)result.findViewById(R.id.camera_list);
        mIdView = (TextView)result.findViewById(R.id.live_event_id);
        mPreview = (AutoFitTextureView) result.findViewById(R.id.preview);
        mPreview.setSurfaceTextureListener(mSurfaceTextureListener);

        mStartStream = result.findViewById(R.id.start_cam_stream);

        mStopStream = result.findViewById(R.id.stop_cam_stream);

        mStartStream.setEnabled(false);
        mStopStream.setEnabled(false);

        mStartStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCamStream();
            }
        });

        mStopStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopCamStream();
            }
        });

        Bundle bundle = getArguments();
        if (null != bundle) {
            String userId = bundle.getString(LoggedInFragment.PARAM_USER);
            if (null != userId) {
                User user = VR.getUserById(userId);
                if (null != user) {
                    String liveEventId = bundle.getString(PARAM_LIVE_EVENT_ID);
                    if (null != liveEventId) {
                        Log.d(TAG, "Querying live event with id: " + liveEventId);
                        user.queryLiveEvent(liveEventId, mCallbackFindLiveEvent, null, null);
                    }
                }
            }
        }

        return result;
    }

    private boolean resetMediaRecorder(Size size) {
        if (null == mMediaRecorder) {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {

                }
            });
        } else {
            mMediaRecorder.reset();
        }

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setOutputFile(mPipe[1].getFileDescriptor());
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(size.getWidth(), size.getHeight());
        try {
            mMediaRecorder.prepare();
        } catch (IOException ex) {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyView() {

        mPreview.setSurfaceTextureListener(null);
        mPreview.destroy();
        mPreview = null;

        if (null != mMediaRecorder) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mIdView = null;
        mCameraList = null;
        mLayoutInflator = null;

        super.onDestroyView();
    }

    static PublishLiveEventFromCamFragment newFragment() {
        return new PublishLiveEventFromCamFragment();
    }


}