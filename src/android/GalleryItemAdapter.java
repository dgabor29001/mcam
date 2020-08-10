package org.apache.cordova.camera;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by dgabor on 24/10/2017.
 */

public class GalleryItemAdapter extends RecyclerView.Adapter<GalleryItemAdapter.MyViewHolder> {

    public interface OnItemClickListener {
        void galleryResponse(String img);
    }

    private OnItemClickListener listener;

    private ArrayList<String> images;
    private Context context;
    private ProgressDialog pd;
    //private FakeR faker;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumb_image;

        public MyViewHolder(View view) {
            super(view);
            thumb_image = (ImageView) view.findViewById(FakeR.getId(context,"id", "custom_image"));
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
        //this.faker = new FakeR(context);

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(FakeR.getId(this.context,"layout", "wowcam_gallery_item"), parent, false);

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
