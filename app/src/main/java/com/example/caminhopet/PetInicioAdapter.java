package com.example.caminhopet;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PetInicioAdapter extends RecyclerView.Adapter<PetInicioAdapter.PetViewHolder> {

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    private final Context context;
    private List<Pet> listaPets;
    private final OnPetClickListener listener;

    public PetInicioAdapter(Context context, List<Pet> listaPets, OnPetClickListener listener) {
        this.context = context;
        this.listaPets = listaPets;
        this.listener = listener;
    }

    public void atualizarLista(List<Pet> novaLista) {
        this.listaPets = novaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet_inicio, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = listaPets.get(position);

        holder.tvNomePetCard.setText(pet.getNome());
        String resumo = pet.getEspecie() + " • " + pet.getCor() + " • porte " + pet.getPorte().toLowerCase();
        holder.tvResumoPetCard.setText(resumo);
        holder.tvTempoPetCard.setText(pet.getTempoStatus());
        holder.tvStatusBadge.setText(pet.getStatus());

        if ("Encontrado".equalsIgnoreCase(pet.getStatus())) {
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_encontrado);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_encontrado_texto));
        } else {
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_perdido);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_perdido_texto));
        }

        // Exibe a foto real do pet se existir
        Bitmap bmp = Pet.base64ParaBitmap(pet.getFotoBase64());
        if (bmp != null && holder.ivFotoPetCard != null) {
            holder.ivFotoPetCard.setImageTintList(null);
            holder.ivFotoPetCard.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.ivFotoPetCard.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.ivFotoPetCard.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.ivFotoPetCard.setImageBitmap(bmp);
            if (holder.ivFotoPetCard.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) holder.ivFotoPetCard.getParent();
                if (parent.getChildCount() > 1) {
                    parent.getChildAt(1).setVisibility(View.GONE);
                }
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onPetClick(pet));
    }

    @Override
    public int getItemCount() {
        return listaPets.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatusBadge, tvNomePetCard, tvResumoPetCard, tvTempoPetCard;
        ImageView ivFotoPetCard;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvNomePetCard = itemView.findViewById(R.id.tvNomePetCard);
            tvResumoPetCard = itemView.findViewById(R.id.tvResumoPetCard);
            tvTempoPetCard = itemView.findViewById(R.id.tvTempoPetCard);
            ivFotoPetCard = itemView.findViewById(R.id.ivFotoPetCard);
        }
    }
}