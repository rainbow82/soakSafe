package com.wgu.d424.soaksafe.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.wgu.d424.soaksafe.R;
import com.wgu.d424.soaksafe.data.MaintenanceRepository;
import com.wgu.d424.soaksafe.data.MaintenanceTasksCatalog;
import com.wgu.d424.soaksafe.databinding.ItemMaintenanceTaskBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaintenanceTaskAdapter extends RecyclerView.Adapter<MaintenanceTaskAdapter.Holder> {

    public interface Listener {
        void onCompletionToggled(@NonNull String taskKey, boolean nowChecked);

        void onDeleteLatest(@NonNull String taskKey);
    }

    private final Listener listener;
    private final List<MaintenanceRepository.TaskRowState> rows = new ArrayList<>();
    private final Map<String, MaintenanceTasksCatalog.Entry> catalogByKey = new HashMap<>();

    public MaintenanceTaskAdapter(@NonNull Listener listener) {
        this.listener = listener;
        for (MaintenanceTasksCatalog.Entry e : MaintenanceTasksCatalog.ENTRIES) {
            catalogByKey.put(e.key, e);
        }
    }

    public void setRows(@NonNull List<MaintenanceRepository.TaskRowState> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMaintenanceTaskBinding binding = ItemMaintenanceTaskBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MaintenanceRepository.TaskRowState state = rows.get(position);
        MaintenanceTasksCatalog.Entry entry = catalogByKey.get(state.taskKey);
        if (entry == null) {
            return;
        }
        holder.binding.textTitle.setText(entry.titleRes);
        int primary = ContextCompat.getColor(holder.itemView.getContext(), R.color.maint_text_primary);
        holder.binding.textTitle.setTextColor(primary);

        holder.binding.textAmountBadge.setVisibility(
                entry.showAmountHint ? View.VISIBLE : View.GONE
        );

        holder.binding.checkboxDone.setOnCheckedChangeListener(null);
        holder.binding.checkboxDone.setChecked(state.completedToday);
        holder.binding.checkboxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            MaintenanceRepository.TaskRowState s = rows.get(pos);
            listener.onCompletionToggled(s.taskKey, isChecked);
        });

        holder.binding.buttonDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            listener.onDeleteLatest(rows.get(pos).taskKey);
        });
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemMaintenanceTaskBinding binding;

        Holder(@NonNull ItemMaintenanceTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
