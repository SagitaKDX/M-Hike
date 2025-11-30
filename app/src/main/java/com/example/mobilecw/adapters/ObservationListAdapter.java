package com.example.mobilecw.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilecw.R;
import com.example.mobilecw.database.entities.Observation;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ObservationListAdapter extends ListAdapter<Observation, ObservationListAdapter.ObservationViewHolder> {

    private OnObservationClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface OnObservationClickListener {
        void onEditClicked(Observation observation);
        void onDeleteClicked(Observation observation);
    }

    public ObservationListAdapter(OnObservationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Observation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Observation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Observation oldItem, @NonNull Observation newItem) {
            return oldItem.getObservationID() == newItem.getObservationID();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Observation oldItem, @NonNull Observation newItem) {
            return oldItem.getObservationText().equals(newItem.getObservationText()) &&
                   oldItem.getTime().equals(newItem.getTime());
        }
    };

    @NonNull
    @Override
    public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_observation_card, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
        Observation observation = getItem(position);
        holder.bind(observation, listener, dateFormat);
    }

    static class ObservationViewHolder extends RecyclerView.ViewHolder {
        private TextView observationTime;
        private TextView observationText;
        private TextView observationComments;
        private TextView observationLocation;
        private ImageView observationPicture;
        private ImageButton editButton;
        private ImageButton deleteButton;
        private LinearLayout commentsSection;
        private LinearLayout locationSection;
        private CardView pictureCard;

        public ObservationViewHolder(@NonNull View itemView) {
            super(itemView);
            observationTime = itemView.findViewById(R.id.observationTime);
            observationText = itemView.findViewById(R.id.observationText);
            observationComments = itemView.findViewById(R.id.observationComments);
            observationLocation = itemView.findViewById(R.id.observationLocation);
            observationPicture = itemView.findViewById(R.id.observationPicture);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            commentsSection = itemView.findViewById(R.id.commentsSection);
            locationSection = itemView.findViewById(R.id.locationSection);
            pictureCard = itemView.findViewById(R.id.pictureCard);
        }

        public void bind(Observation observation, OnObservationClickListener listener, SimpleDateFormat dateFormat) {
            // Set time
            if (observation.getTime() != null) {
                observationTime.setText(dateFormat.format(observation.getTime()));
            }

            // Set observation text
            observationText.setText(observation.getObservationText());

            // Set comments (show/hide section based on availability)
            if (observation.getComments() != null && !observation.getComments().isEmpty()) {
                observationComments.setText(observation.getComments());
                commentsSection.setVisibility(View.VISIBLE);
            } else {
                commentsSection.setVisibility(View.GONE);
            }

            // Set location (show/hide section based on availability)
            if (observation.getLocation() != null && !observation.getLocation().isEmpty()) {
                observationLocation.setText(observation.getLocation());
                locationSection.setVisibility(View.VISIBLE);
            } else {
                locationSection.setVisibility(View.GONE);
            }

            // Set picture (show/hide based on availability)
            if (observation.getPicture() != null && !observation.getPicture().isEmpty()) {
                try {
                    File imageFile = new File(observation.getPicture());
                    if (imageFile.exists()) {
                        observationPicture.setImageURI(Uri.fromFile(imageFile));
                        pictureCard.setVisibility(View.VISIBLE);
                    } else {
                        pictureCard.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    pictureCard.setVisibility(View.GONE);
                }
            } else {
                pictureCard.setVisibility(View.GONE);
            }

            // Set click listeners
            editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditClicked(observation);
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClicked(observation);
                }
            });
        }
    }
}

