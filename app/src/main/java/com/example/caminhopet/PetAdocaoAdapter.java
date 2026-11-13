package com.example.caminhopet;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PetAdocaoAdapter extends RecyclerView.Adapter<PetAdocaoAdapter.AdocaoViewHolder> {

    private final Context context;
    private final List<Pet> listaAdocao;

    public PetAdocaoAdapter(Context context, List<Pet> listaAdocao) {
        this.context = context;
        this.listaAdocao = listaAdocao;
    }

    @NonNull
    @Override
    public AdocaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet_adocao, parent, false);
        return new AdocaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdocaoViewHolder holder, int position) {
        Pet pet = listaAdocao.get(position);
        String emailLogado = GerenciadorUsuarios.getEmailLogado(context);
        boolean souDonoDaPublicacao = !emailLogado.isEmpty() && emailLogado.equalsIgnoreCase(pet.getEmailAutor());

        holder.tvNomeAdocaoCard.setText(pet.getNome());
        String resumo = pet.getEspecie() + " • " + pet.getCor() + " • porte " + pet.getPorte().toLowerCase();
        holder.tvResumoAdocaoCard.setText(resumo);
        holder.tvResponsavelAdocaoCard.setText("Responsável: " + pet.getTempoStatus());
        holder.tvBadgeAdocaoCard.setText(pet.getStatus());

        // Exibe a foto real do pet no card de adoção
        Bitmap bmp = Pet.base64ParaBitmap(pet.getFotoBase64());
        if (bmp != null && holder.ivFotoCard != null) {
            holder.ivFotoCard.setImageTintList(null);
            holder.ivFotoCard.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.ivFotoCard.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            holder.ivFotoCard.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.ivFotoCard.setImageBitmap(bmp);
            if (holder.ivFotoCard.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) holder.ivFotoCard.getParent();
                if (parent.getChildCount() > 1) {
                    parent.getChildAt(1).setVisibility(View.GONE);
                }
            }
        }

        if ("Adotado".equalsIgnoreCase(pet.getStatus())) {
            holder.tvBadgeAdocaoCard.setBackgroundResource(R.drawable.bg_badge_encontrado);
            holder.tvBadgeAdocaoCard.setTextColor(ContextCompat.getColor(context, R.color.badge_encontrado_texto));

            holder.btnQueroAdotar.setText("Pet já adotado ♥");
            holder.btnQueroAdotar.setEnabled(false);
            holder.btnQueroAdotar.setAlpha(0.6f);
            holder.btnMarcarAdotado.setVisibility(View.GONE);
        } else {
            holder.tvBadgeAdocaoCard.setBackgroundResource(R.drawable.bg_badge_adocao);
            holder.tvBadgeAdocaoCard.setTextColor(ContextCompat.getColor(context, R.color.badge_adocao_texto));

            holder.btnQueroAdotar.setText("Quero adotar");
            holder.btnQueroAdotar.setEnabled(true);
            holder.btnQueroAdotar.setAlpha(1.0f);

            if (souDonoDaPublicacao) {
                holder.btnMarcarAdotado.setVisibility(View.VISIBLE);
                holder.btnMarcarAdotado.setOnClickListener(v -> {
                    pet.setStatus("Adotado");
                    if (pet.getDocumentoId() != null) {
                        FirebaseFirestore.getInstance()
                                .collection("pets")
                                .document(pet.getDocumentoId())
                                .update("status", "Adotado");
                    }
                    notifyItemChanged(position);
                    Toast.makeText(context, pet.getNome() + " marcado(a) como adotado!", Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.btnMarcarAdotado.setVisibility(View.GONE);
            }

            holder.btnQueroAdotar.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("nome_pet", pet.getNome());
                intent.putExtra("tipo_assunto", "adocao");
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return listaAdocao.size();
    }

    static class AdocaoViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadgeAdocaoCard, tvNomeAdocaoCard, tvResumoAdocaoCard, tvResponsavelAdocaoCard;
        Button btnQueroAdotar, btnMarcarAdotado;
        ImageView ivFotoCard;

        public AdocaoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBadgeAdocaoCard = itemView.findViewById(R.id.tvBadgeAdocaoCard);
            tvNomeAdocaoCard = itemView.findViewById(R.id.tvNomeAdocaoCard);
            tvResumoAdocaoCard = itemView.findViewById(R.id.tvResumoAdocaoCard);
            tvResponsavelAdocaoCard = itemView.findViewById(R.id.tvResponsavelAdocaoCard);
            btnQueroAdotar = itemView.findViewById(R.id.btnQueroAdotar);
            btnMarcarAdotado = itemView.findViewById(R.id.btnMarcarAdotado);
            ivFotoCard = buscarPrimeiraImageView(itemView);
        }

        private ImageView buscarPrimeiraImageView(View view) {
            if (view instanceof ImageView) return (ImageView) view;
            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    ImageView encontrada = buscarPrimeiraImageView(vg.getChildAt(i));
                    if (encontrada != null) return encontrada;
                }
            }
            return null;
        }
    }
}