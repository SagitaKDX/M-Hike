package com.example.mobilecw.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilecw.R;
import com.example.mobilecw.activities.HikeDetailActivity;
import com.example.mobilecw.database.entities.Hike;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NearbyTrailAdapter extends RecyclerView.Adapter<NearbyTrailAdapter.TrailViewHolder> {
    
    private List<Hike> trails;
    private OnTrailClickListener listener;
    
    public interface OnTrailClickListener {
        void onTrailClick(Hike hike);
    }
    
    public NearbyTrailAdapter(List<Hike> trails, OnTrailClickListener listener) {
        this.trails = trails;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public TrailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_trail, parent, false);
        return new TrailViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TrailViewHolder holder, int position) {
        Hike hike = trails.get(position);
        holder.bind(hike);
    }
    
    @Override
    public int getItemCount() {
        return trails != null ? trails.size() : 0;
    }
    
    class TrailViewHolder extends RecyclerView.ViewHolder {
        private TextView trailName, trailDistance, trailDifficulty, trailTime;
        private TextView viewButton;
        
        public TrailViewHolder(@NonNull View itemView) {
            super(itemView);
            trailName = itemView.findViewById(R.id.trailName);
            trailDistance = itemView.findViewById(R.id.trailDistance);
            trailDifficulty = itemView.findViewById(R.id.trailDifficulty);
            trailTime = itemView.findViewById(R.id.trailTime);
            viewButton = itemView.findViewById(R.id.viewButton);
        }
        
        public void bind(Hike hike) {
            trailName.setText(hike.getName());
            trailDistance.setText(String.format("%.1f km", hike.getLength()));
            trailDifficulty.setText(hike.getDifficulty());
            
            // Calculate estimated time based on length and difficulty
            double estimatedHours = calculateEstimatedTime(hike.getLength(), hike.getDifficulty());
            int hours = (int) estimatedHours;
            int minutes = (int) ((estimatedHours - hours) * 60);
            trailTime.setText(String.format("%dh %dm", hours, minutes));
            
            viewButton.setOnClickListener(v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, HikeDetailActivity.class);
                intent.putExtra(HikeDetailActivity.EXTRA_HIKE_ID, hike.getHikeID());
                context.startActivity(intent);
            });
        }
        
        private double calculateEstimatedTime(double length, String difficulty) {
            // Average hiking speed: Easy = 4 km/h, Medium = 3 km/h, Hard = 2 km/h
            double speed;
            switch (difficulty.toLowerCase()) {
                case "easy":
                    speed = 4.0;
                    break;
                case "medium":
                    speed = 3.0;
                    break;
                case "hard":
                    speed = 2.0;
                    break;
                default:
                    speed = 3.0;
            }
            return length / speed;
        }
    }
}

