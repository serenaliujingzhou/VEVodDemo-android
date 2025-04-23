/*
 * Copyright (C) 2024 bytedance
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Create Date : 2024/3/26
 */

package com.bytedance.volc.voddemo0320.ui.minidrama.data.remote;

import static com.bytedance.volc.voddemo0320.data.remote.AppServer.dramaApi;

import androidx.annotation.NonNull;

import com.bytedance.volc.vod.scenekit.VideoSettings;
import com.bytedance.volc.voddemo0320.data.remote.RemoteApi;
import com.bytedance.volc.voddemo0320.data.remote.model.Params;
import com.bytedance.volc.voddemo0320.data.remote.model.drama.EpisodeVideo;
import com.bytedance.volc.voddemo0320.data.remote.model.drama.GetEpisodeFeedStreamRequest;
import com.bytedance.volc.voddemo0320.data.remote.model.drama.GetEpisodeFeedStreamResponse;
import com.bytedance.volc.voddemo0320.ui.minidrama.data.remote.api.GetEpisodeRecommendApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class GetEpisodeRecommend implements GetEpisodeRecommendApi {

    private final List<Call<?>> mCalls = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void getRecommendEpisodeVideoItems(int pageIndex, int pageSize, RemoteApi.Callback<List<EpisodeVideo>> callback) {
        final RemoteApi.HandlerCallback<List<EpisodeVideo>> mainCallback = new RemoteApi.HandlerCallback<>(callback);
        final String account = VideoSettings.stringValue(VideoSettings.DRAMA_VIDEO_SCENE_ACCOUNT_ID);
        final GetEpisodeFeedStreamRequest request = new GetEpisodeFeedStreamRequest(
                account,
                pageIndex * pageSize,
                pageSize,
                Params.Value.format(),
                Params.Value.codec(),
                Params.Value.definition(),
                Params.Value.fileType(),
                Params.Value.needThumbs(),
                Params.Value.enableBarrageMask(),
                Params.Value.cdnType(),
                Params.Value.unionInfo()
        );
        Call<GetEpisodeFeedStreamResponse> call = createCall(request);
        call.enqueue(new retrofit2.Callback<GetEpisodeFeedStreamResponse>() {
            @Override
            public void onResponse(@NonNull Call<GetEpisodeFeedStreamResponse> call, @NonNull Response<GetEpisodeFeedStreamResponse> response) {
                mCalls.remove(call);
                if (response.isSuccessful()) {
                    GetEpisodeFeedStreamResponse result = response.body();
                    if (result == null) {
                        mainCallback.onError(new IOException("result = null + " + response));
                        return;
                    }
                    if (result.responseMetadata != null && result.responseMetadata.error != null) {
                        mainCallback.onError(new IOException(response + "; " + result.responseMetadata.error));
                        return;
                    }
                    mainCallback.onSuccess(result.result);
                } else {
                    mainCallback.onError(new IOException(response.toString()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<GetEpisodeFeedStreamResponse> call, @NonNull Throwable t) {
                mCalls.remove(call);
                mainCallback.onError(new IOException(t));
            }
        });
        mCalls.add(call);
    }

    protected Call<GetEpisodeFeedStreamResponse> createCall(GetEpisodeFeedStreamRequest request) {
        final int sourceType = VideoSettings.intValue(VideoSettings.COMMON_SOURCE_TYPE);
        switch (sourceType) {
            case VideoSettings.SourceType.SOURCE_TYPE_VID:
                return dramaApi().getEpisodeFeedStreamWithPlayAuthToken(request);
            case VideoSettings.SourceType.SOURCE_TYPE_URL:
            case VideoSettings.SourceType.SOURCE_TYPE_MODEL:
                return dramaApi().GetEpisodeFeedStreamWithVideoModel(request);
            default:
                throw new NullPointerException();
        }
    }

    @Override
    public void cancel() {
        synchronized (mCalls) {
            for (Call<?> call : mCalls) {
                call.cancel();
            }
            mCalls.clear();
        }
    }
}
