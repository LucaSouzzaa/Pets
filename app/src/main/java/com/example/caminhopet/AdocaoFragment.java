package com.example.caminhopet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdocaoFragment extends Fragment {

    private PetAdocaoAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_adocao, container, false);

        RecyclerView rvPetsAdocao = view.findViewById(R.id.rvPetsAdocao);
        rvPetsAdocao.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new PetAdocaoAdapter(getContext(), Pet.listaAdocao);
        rvPetsAdocao.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        carregarPetsAdocaoDoFirebase();
    }

    private void carregarPetsAdocaoDoFirebase() {
        FirebaseFirestore.getInstance()
                .collection("pets")
                .whereEqualTo("tipoLista", "adocao")
                .get()
                .addOnSuccessListener(query -> {
                    Pet.listaAdocao.clear();
                    for (DocumentSnapshot doc : query) {
                        Pet p = doc.toObject(Pet.class);
                        if (p != null) {
                            p.setDocumentoId(doc.getId());
                            Pet.listaAdocao.add(p);
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}