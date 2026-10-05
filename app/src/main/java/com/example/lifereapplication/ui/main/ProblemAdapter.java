package com.example.lifereapplication.ui.main;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifereapplication.R;
import com.example.lifereapplication.data.model.Problem;

import java.util.ArrayList;
import java.util.List;

public class ProblemAdapter extends RecyclerView.Adapter<ProblemAdapter.ViewHolder> {

    private List<Problem> problems = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Problem problem);
    }

    public ProblemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setProblems(List<Problem> problems) {
        this.problems = problems != null ? problems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_problem, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Problem problem = problems.get(position);
        holder.title.setText(problem.getTitle());
        holder.description.setText(problem.getDescription());
        holder.difficulty.setText(problem.getDifficulty());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                animateClick(v, () -> listener.onItemClick(problem));
            }
        });
    }

    private void animateClick(View view, Runnable onAnimationEnd) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (onAnimationEnd != null) {
                                            onAnimationEnd.run();
                                        }
                                    }
                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return problems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView description;
        TextView difficulty;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);
            difficulty = itemView.findViewById(R.id.textDifficulty);
        }
    }
}
