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

package com.samsung.msca.samsungvr.sdk;

import android.os.Handler;

public interface UserLiveEvent {

    final class Result {

        private Result() {
        }

        public interface QueryLiveEvent extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;

        }

        public interface DeleteLiveEvent extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;

        }

        public interface UpdateLiveEvent extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;

        }

        public interface UploadThumbnail extends
                VR.Result.BaseCallback, VR.Result.SuccessCallback,
                VR.Result.ProgressCallback {

            int INVALID_LIVE_EVENT_ID = 1;

        }

    }

    enum State {
        UNKNOWN,
        LIVE_CREATED,
        LIVE_CONNECTED ,
        LIVE_DISCONNECTED,
        LIVE_FINISHED
    }

    enum Protocol {
        RTMP
    }

    enum VideoStereoscopyType {
        DEFAULT,
        MONOSCOPIC,
        TOP_BOTTOM_STEREOSCOPIC,
        LEFT_RIGHT_STEREOSCOPIC,
        DUAL_FISHEYE
    }

    boolean query(Result.QueryLiveEvent callback, Handler handler, Object closure);
    boolean delete(Result.DeleteLiveEvent callback, Handler handler, Object closure);

    String getId();
    String getTitle();
    String getDescription();
    String getProducerUrl();
    State getState();
    Long getViewerCount();
    Protocol getProtocol();
    VideoStereoscopyType getVideoStereoscopyType();
    String getThumbnailUrl();

    User getUser();

}