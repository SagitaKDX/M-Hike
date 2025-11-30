package com.example.mobilecw.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.example.mobilecw.R;
import com.example.mobilecw.database.entities.Hike;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HikeListAdapter extends RecyclerView.Adapter<HikeListAdapter.HikeViewHolder> {

    private List<Hike> hikes = new ArrayList<>();
    private final OnHikeClickListener listener;
    private OnSelectionChangedListener selectionChangedListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private boolean selectionMode = false;
    private final Set<Integer> selectedHikeIds = new HashSet<>();

    public interface OnHikeClickListener {
        void onHikeClicked(Hike hike);
    }
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public HikeListAdapter(OnHikeClickListener listener) {
        this.listener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void submitList(List<Hike> items) {
        this.hikes = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setSelectionMode(boolean enabled) {
        selectionMode = enabled;
        if (!selectionMode) {
            selectedHikeIds.clear();
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(0);
            }
        }
        notifyDataSetChanged();
    }
    
    public boolean isSelectionMode() {
        return selectionMode;
    }
    
    public List<Integer> getSelectedHikeIds() {
        return new ArrayList<>(selectedHikeIds);
    }

    @NonNull
    @Override
    public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hike_card, parent, false);
        return new HikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
        Hike hike = hikes.get(position);
        holder.bind(hike);
    }

    @Override
    public int getItemCount() {
        return hikes.size();
    }

    class HikeViewHolder extends RecyclerView.ViewHolder {

        TextView name, location, date, length, difficulty, parking;
        MaterialButton viewButton;
        CheckBox selectCheckbox;

        HikeViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.hikeNameText);
            location = itemView.findViewById(R.id.hikeLocationText);
            date = itemView.findViewById(R.id.hikeDateText);
            length = itemView.findViewById(R.id.hikeLengthText);
            difficulty = itemView.findViewById(R.id.hikeDifficultyText);
            parking = itemView.findViewById(R.id.hikeParkingText);
            viewButton = itemView.findViewById(R.id.viewButton);
            selectCheckbox = itemView.findViewById(R.id.hikeSelectCheckbox);
        }

        void bind(Hike hike) {
            name.setText(hike.getName());
            location.setText(hike.getLocation());
            if (hike.getDate() != null) {
                date.setText(dateFormat.format(hike.getDate()));
            } else {
                date.setText("N/A");
            }
            length.setText(String.format(Locale.getDefault(), "%.1f km", hike.getLength()));
            difficulty.setText(hike.getDifficulty());
            parking.setText("Parking: " + (hike.isParkingAvailable() ? "Yes" : "No"));

            if (selectionMode) {
                selectCheckbox.setVisibility(View.VISIBLE);
                boolean isSelected = selectedHikeIds.contains(hike.getHikeID());
                selectCheckbox.setChecked(isSelected);
                viewButton.setEnabled(false);
                viewButton.setAlpha(0.5f);

                View.OnClickListener toggleListener = v -> toggleSelection(hike);
                itemView.setOnClickListener(toggleListener);
                selectCheckbox.setOnClickListener(toggleListener);
                viewButton.setOnClickListener(toggleListener);
            } else {
                selectCheckbox.setVisibility(View.GONE);
                selectCheckbox.setOnClickListener(null);
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onHikeClicked(hike);
                    }
                });
                viewButton.setEnabled(true);
                viewButton.setAlpha(1f);
                viewButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onHikeClicked(hike);
                    }
                });
            }
        }
    }
    
    private void toggleSelection(Hike hike) {
        int id = hike.getHikeID();
        if (selectedHikeIds.contains(id)) {
            selectedHikeIds.remove(id);
        } else {
            selectedHikeIds.add(id);
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedHikeIds.size());
        }
        notifyDataSetChanged();
    }
}

