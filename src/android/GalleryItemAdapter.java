/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cordova.camera;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by dgabor on 13/09/2017.
 */

public class GalleryItemAdapter extends RecyclerView.Adapter<GalleryItemAdapter.MyViewHolder> {

    public interface OnItemClickListener {
        void galleryResponse(String img);
    }

    private OnItemClickListener listener;

    private ArrayList<String> images;
    private Context context;
    private ProgressDialog pd;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumb_image;

        public MyViewHolder(View view) {
            super(view);
            thumb_image=(ImageView) view.findViewById(R.id.custom_image);
            thumb_image.setVisibility(View.VISIBLE);
        }

        public void bind(final String img, final OnItemClickListener listener) {

            Glide.with(context)
                    .load(img)
                    .centerCrop()
                    //.placeholder(R.drawable.icono_android)
                    .into(thumb_image);

            thumb_image.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    //pd = ProgressDialog.show(context, "","...", true);
                    listener.galleryResponse(img);
                }
            });

        }

    }

    public GalleryItemAdapter(Context context, ArrayList<String> images, OnItemClickListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.mcam_gallery_item, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        holder.bind(images.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }

    public void destroyProgress(){
        if(pd!=null && pd.isShowing())
            pd.dismiss();
    }
}