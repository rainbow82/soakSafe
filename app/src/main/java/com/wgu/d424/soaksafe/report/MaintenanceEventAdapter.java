package com.wgu.d424.soaksafe.report;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wgu.d424.soaksafe.R;
import com.wgu.d424.soaksafe.databinding.ItemMaintenanceEventBinding;
import com.wgu.d424.soaksafe.databinding.ItemReportDetailRowBinding;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceEventAdapter extends RecyclerView.Adapter<MaintenanceEventAdapter.Holder> {

    private final List<MaintenanceEventUiModel> rows = new ArrayList<>();

    public void setRows(@NonNull List<MaintenanceEventUiModel> events) {
        rows.clear();
        rows.addAll(events);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMaintenanceEventBinding binding = ItemMaintenanceEventBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MaintenanceEventUiModel row = rows.get(position);
        holder.binding.textEventTitle.setText(row.timeLabel);

        holder.binding.layoutEventRows.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.binding.getRoot().getContext());
        for (ReportDetailLine line : row.detailLines) {
            ItemReportDetailRowBinding rowBinding = ItemReportDetailRowBinding.inflate(
                    inflater,
                    holder.binding.layoutEventRows,
                    false
            );
            rowBinding.textRowLabel.setText(line.label);
            if (line.showCheckmark) {
                rowBinding.viewRowAccent.setBackgroundResource(R.drawable.bg_report_accent_task);
                rowBinding.imageRowCheck.setVisibility(View.VISIBLE);
                rowBinding.textRowValue.setVisibility(View.GONE);
            } else {
                rowBinding.viewRowAccent.setBackgroundResource(R.drawable.bg_report_accent_chem);
                rowBinding.imageRowCheck.setVisibility(View.GONE);
                rowBinding.textRowValue.setVisibility(View.VISIBLE);
                rowBinding.textRowValue.setText(line.valueText != null ? line.valueText : "");
            }
            holder.binding.layoutEventRows.addView(rowBinding.getRoot());
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemMaintenanceEventBinding binding;

        Holder(@NonNull ItemMaintenanceEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
