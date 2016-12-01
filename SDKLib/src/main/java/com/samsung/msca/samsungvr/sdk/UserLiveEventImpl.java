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
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

class UserLiveEventImpl extends Contained.BaseImpl<UserImpl> implements UserLiveEvent {

    private enum Properties {
        ID,
        TITLE,
        PROTOCOL,
        STEREOSCOPIC_TYPE,
        DESCRIPTION,
        INGEST_URL,
        VIDEO_URL_STREAM,
        STATE,
        THUMBNAIL_URL,
        VIEWER_COUNT
    }

    static final Contained.Type sType = new Contained.Type<UserImpl, UserLiveEventImpl>(Properties.class) {

        @Override
        public void notifyCreate(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventCreated(userLiveEvent);
        }

        @Override
        public void notifyUpdate(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventUpdated(userLiveEvent);
        }

        @Override
        public void notifyDelete(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventDeleted(userLiveEvent);
        }

        @Override
        public void notifyQueried(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventQueried(userLiveEvent);
        }

        @Override
        public void notifyListQueried(Object callback, UserImpl user, List<UserLiveEventImpl> userLiveEvents) {
            ((User.Observer)callback).onUserLiveEventsQueried(user, (List<UserLiveEvent>) (List<?>) userLiveEvents);
        }

        @Override
        String getEnumName(String key) {
            return key.toUpperCase(Locale.US);
        }

        @Override
        public UserLiveEventImpl newInstance(UserImpl container, JSONObject jsonObject) {
            return new UserLiveEventImpl(container, jsonObject);
        }

        @Override
        public Object getContainedId(JSONObject jsonObject) {
            return jsonObject.optString("id");
        }

        @Override
        Object validateValue(Enum<?> key, Object newValue) {
            if (null == newValue) {
                return null;
            }

            switch ((Properties)key) {
                case TITLE:
                case ID:
                case DESCRIPTION:
                case INGEST_URL:
                case VIDEO_URL_STREAM:
                case THUMBNAIL_URL:
                    return newValue.toString();
                case VIEWER_COUNT:
                    return Long.parseLong(newValue.toString());
                case STATE:
                    return Util.enumFromString(State.class, newValue.toString());
                case PROTOCOL:
                    return Util.enumFromString(Protocol.class, newValue.toString());
                case STEREOSCOPIC_TYPE:
                    if ("top-bottom".equals(newValue.toString()))
                        return VideoStereoscopyType.TOP_BOTTOM_STEREOSCOPIC;
                    if ("left-right".equals(newValue.toString()))
                        return VideoStereoscopyType.LEFT_RIGHT_STEREOSCOPIC;
                    if ("dual-fisheye".equals(newValue.toString()))
                        return VideoStereoscopyType.DUAL_FISHEYE;
                    return VideoStereoscopyType.MONOSCOPIC;
                default:
                    Log.d("VRSDK", "unknown tag: " + key);
                    break;
            }
            return null;
        }

    };



    UserLiveEventImpl(UserImpl user, JSONObject jsonObject) throws IllegalArgumentException {
        super(sType, user, jsonObject);
    }

    UserLiveEventImpl(UserImpl container, String id, String title, Protocol protocol,
                      String description, String producerUrl, String consumerUrl,
                      VideoStereoscopyType videoStereoscopyType, State state, Long viewerCount) {

        this(container, null);

        setNoLock(Properties.ID, id);
        setNoLock(Properties.TITLE, title);
        setNoLock(Properties.PROTOCOL, protocol);
        setNoLock(Properties.DESCRIPTION, description);
        setNoLock(Properties.INGEST_URL, producerUrl);
        setNoLock(Properties.VIDEO_URL_STREAM, consumerUrl);
        setNoLock(Properties.STATE, state);
        setNoLock(Properties.VIEWER_COUNT, viewerCount);
        setNoLock(Properties.STEREOSCOPIC_TYPE, videoStereoscopyType);
    }

    UserLiveEventImpl(UserImpl container, String id, String title, Protocol protocol,
                      String description, VideoStereoscopyType videoStereoscopyType, Long viewerCount) {
        this(container, id, title, protocol,
                description, null, null,
                videoStereoscopyType, State.UNKNOWN, viewerCount);
    }

    @Override
    public boolean containedOnQueryFromServiceLocked(JSONObject jsonObject) {
        return processQueryFromServiceLocked(jsonObject);
    }

    @Override
    public void containedOnCreateInServiceLocked() {

    }

    @Override
    public void containedOnDeleteFromServiceLocked() {

    }

    @Override
    public void containedOnUpdateToServiceLocked() {

    }

    @Override
    public Object containedGetIdLocked() {
        return getLocked(Properties.ID);
    }

    @Override
    public boolean query(Result.QueryLiveEvent callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();

        WorkItemQuery workItem = workQueue.obtainWorkItem(WorkItemQuery.TYPE);
        workItem.set(this, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean delete(Result.DeleteLiveEvent callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemDelete workItem = workQueue.obtainWorkItem(WorkItemDelete.TYPE);
        workItem.set(this, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    //@Override
    public boolean update(Result.UpdateLiveEvent callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemUpdate workItem = workQueue.obtainWorkItem(WorkItemUpdate.TYPE);
        workItem.set(this, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public String getId() {
        return (String)getLocked(Properties.ID);
    }

    @Override
    public String getTitle() {
        return (String)getLocked(Properties.TITLE);
    }

    @Override
    public String getDescription() {
        return (String)getLocked(Properties.DESCRIPTION);
    }


    @Override
    public String getProducerUrl() {
        return (String)getLocked(Properties.INGEST_URL);
    }


    @Override
    public VideoStereoscopyType getVideoStereoscopyType() {
        VideoStereoscopyType val = (VideoStereoscopyType)getLocked(Properties.STEREOSCOPIC_TYPE);
        /*
         * Val will never be null in this case. Validate always returns a value for stereoscopic type.
         */
        if (val == null) {
            val = VideoStereoscopyType.MONOSCOPIC;
        }
        return val;
    }

    @Override
    public State getState() {
        return (State)getLocked(Properties.STATE);
    }

    @Override
    public Long getViewerCount() {return (Long)getLocked(Properties.VIEWER_COUNT);}

    @Override
    public Protocol getProtocol() {
        return (Protocol)getLocked(Properties.PROTOCOL);
    }

    @Override
    public User getUser() {
        return getContainer();
    }

    @Override
    public String getThumbnailUrl() {
        return (String)getLocked(Properties.THUMBNAIL_URL);
    }

    //@Override
    public boolean uploadThumbnail(ParcelFileDescriptor source, Result.UploadThumbnail callback,
                                   Handler handler, Object closure) {
        if (null == getThumbnailUrl()) {
            return false;
        }
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemUploadThumbnail workItem = workQueue.obtainWorkItem(WorkItemUploadThumbnail.TYPE);
        workItem.set(this, source, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    /*
     * Upload thumbnail
     */

    static class WorkItemUploadThumbnail extends ClientWorkItem<Result.UploadThumbnail> {


        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemUploadThumbnail newInstance(APIClientImpl apiClient) {
                return new WorkItemUploadThumbnail(apiClient);
            }
        };

        WorkItemUploadThumbnail(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private ParcelFileDescriptor mSource;
        private UserLiveEventImpl mLiveEvent;

        synchronized WorkItemUploadThumbnail set(UserLiveEventImpl liveEvent,
                         ParcelFileDescriptor source, Result.UploadThumbnail callback,
                         Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mLiveEvent = liveEvent;
            mSource = source;

            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSource = null;
            mLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemUploadThumbnail.class);

        @Override
        public void onRun() throws Exception {

            HttpPlugin.PostRequest request = null;

            try {
                UserImpl user = mLiveEvent.getContainer();

                String headers[][] = {
                        {HEADER_CONTENT_TYPE, ""},
                        {HEADER_CONTENT_LENGTH, "0"},
                        {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                        {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
                };
                request = newRequest(mLiveEvent.getThumbnailUrl(), HttpMethod.POST, headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }
                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                writeFileAsMultipartFormData(headers, 0, 1, request, mSource);
                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);
                if (isHTTPSuccess(rsp)) {
                    dispatchSuccess();
                    return;
                }
                dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
            } finally {
                destroy(request);
            }
        }
    }

    /*
     * Delete
     */

    private static class WorkItemDelete extends ClientWorkItem<Result.DeleteLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemDelete newInstance(APIClientImpl apiClient) {
                return new WorkItemDelete(apiClient);
            }
        };

        WorkItemDelete(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;

        synchronized WorkItemDelete set(UserLiveEventImpl userLiveEvent, Result.DeleteLiveEvent callback,
                                        Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUserLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemDelete.class);

        @Override
        public void onRun() throws Exception {
            User user = mUserLiveEvent.getUser();
            HttpPlugin.DeleteRequest request = null;
            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()}
            };
            try {
                String liveEventId = mUserLiveEvent.getId();

                String userId = user.getUserId();

                request = newDeleteRequest(
                        String.format(Locale.US, "user/%s/video/%s", userId, liveEventId), headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);

                if (isHTTPSuccess(rsp)) {
                    if (null != mUserLiveEvent.getContainer().containerOnDeleteOfContainedFromServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEvent)) {
                        dispatchSuccess();
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return;
                }
                String data = readHttpStream(request, "code: " + rsp);
                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);

            } finally {
                destroy(request);
            }

        }
    }


    private static class WorkItemQuery extends ClientWorkItem<Result.QueryLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemQuery newInstance(APIClientImpl apiClient) {
                return new WorkItemQuery(apiClient);
            }
        };

        WorkItemQuery(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;

        synchronized WorkItemQuery set(UserLiveEventImpl userLiveEvent, Result.QueryLiveEvent callback,
                                       Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUserLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemQuery.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;
            User user = mUserLiveEvent.getContainer();
            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()}
            };
            try {
                String liveEventId = mUserLiveEvent.getId();

                String userId = user.getUserId();
                request = newGetRequest(String.format(Locale.US, "user/%s/video/%s", userId, liveEventId),
                        headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);
                String data = readHttpStream(request, "code: " + rsp);

                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }

                Log.d(TAG, "onSuccess : " + data);
                JSONObject jsonObject = new JSONObject(data);

                if (isHTTPSuccess(rsp)) {
                    JSONObject liveEvent = jsonObject.getJSONObject("video");
                    mUserLiveEvent.getContainer().containerOnQueryOfContainedFromServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEvent, liveEvent);
                    dispatchSuccess();
                    return;
                }
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);

            } finally {
                destroy(request);
            }

        }
    }


    /*
     * Update
     */

    static class WorkItemUpdate extends ClientWorkItem<Result.UpdateLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemUpdate newInstance(APIClientImpl apiClient) {
                return new WorkItemUpdate(apiClient);
            }
        };

        WorkItemUpdate(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;

        synchronized WorkItemUpdate set(UserLiveEventImpl userLiveEvent, Result.UpdateLiveEvent callback,
                                        Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUserLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemUpdate.class);


        @Override
        public void onRun() throws Exception {
            HttpPlugin.PutRequest request = null;
            User user = mUserLiveEvent.getUser();

            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
                    {HEADER_CONTENT_TYPE, "application/json"}
            };
            try {
                String liveEventId = mUserLiveEvent.getId();
                String userId = user.getUserId();
                request = newPutRequest(String.format(Locale.US, "user/%s/video/%s", userId, liveEventId),
                        headers);

                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);

                if (isHTTPSuccess(rsp)) {
                    if (null != mUserLiveEvent.getContainer().containerOnUpdateOfContainedToServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEvent)) {
                        dispatchSuccess();
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return;
                }

                String data = readHttpStream(request, "failure");
                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }

                Log.d(TAG, "onSuccess : " + data);

                JSONObject jsonObject = new JSONObject(data);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);;

                dispatchFailure(status);

            } finally {
                destroy(request);
            }

        }
    }

}