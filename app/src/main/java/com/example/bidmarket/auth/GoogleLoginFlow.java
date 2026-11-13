package com.example.bidmarket.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bidmarket.repositories.GoogleAccountRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.function.IntConsumer;

public final class GoogleLoginFlow {

    private final AppCompatActivity activity;
    private final ExecutorService io;
    private final GoogleAccountRepository repository;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final IntConsumer alEntrar;
    private final View[] controles;

    private boolean ocupado;

    public GoogleLoginFlow(
            AppCompatActivity activity,
            ExecutorService io,
            IntConsumer alEntrar,
            View... controles
    ) {
        this.activity = activity;
        this.io = io;
        this.alEntrar = alEntrar;
        this.controles = controles;
        this.repository = new GoogleAccountRepository(activity);
    }

    public void iniciar() {
        if (ocupado || !pantallaActiva()) return;

        bloquear(true);

        new GoogleAuthManager(activity).iniciar(new GoogleAuthManager.Callback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                if (!pantallaActiva()) return;

                String uid = user.getUid();

                if (!esSesionActual(uid)) {
                    fallar(null, "Vuelve a iniciar sesión con Google.");
                    return;
                }

                io.execute(() -> {
                    try {
                        Integer id = repository.buscarPerfil(uid);

                        enPantalla(() -> {
                            if (!esSesionActual(uid)) {
                                fallar(null, "La sesión de Google cambió.");
                            } else if (id != null) {
                                entrar(id);
                            } else if (user.getEmail() == null
                                    || !user.isEmailVerified()) {
                                fallar(uid, "Google no proporcionó un correo verificado.");
                            } else {
                                pedirPerfil(user);
                            }
                        });
                    } catch (IllegalStateException e) {
                        enPantalla(() -> fallar(uid, e.getMessage()));
                    } catch (RuntimeException e) {
                        Log.e("BidMarketAuth", "Error al buscar el perfil local", e);
                        enPantalla(() -> fallar(uid, "No se pudo consultar el perfil local."));
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                enPantalla(() -> fallar(null, mensaje));
            }
        });
    }

    /**
     * Devuelve true si la sesión guardada era de Google y este flujo
     * se encarga de comprobarla.
     */
    public boolean restaurarSiEsGoogle() {
        SharedPreferences prefs = preferencias();

        if (!"GOOGLE".equals(prefs.getString("auth_provider", ""))) {
            return false;
        }

        int idGuardado = prefs.getInt("usuario_id", -1);
        FirebaseUser actual = auth.getCurrentUser();

        if (actual == null || idGuardado <= 0) {
            descartarSesionGoogle();
            return true;
        }

        String uid = actual.getUid();
        bloquear(true);

        io.execute(() -> {
            try {
                Integer id = repository.buscarPerfil(uid);

                enPantalla(() -> {
                    if (esSesionActual(uid) && id != null && id == idGuardado) {
                        entrar(id);
                    } else {
                        descartarSesionGoogle();
                    }
                });
            } catch (RuntimeException e) {
                Log.w("BidMarketAuth", "No se pudo restaurar la sesión de Google", e);
                enPantalla(this::descartarSesionGoogle);
            }
        });

        return true;
    }

    private void pedirPerfil(FirebaseUser user) {
        String uid = user.getUid();
        String correo = user.getEmail();

        LinearLayout campos = new LinearLayout(activity);
        campos.setOrientation(LinearLayout.VERTICAL);
        int margen = Math.round(24 * activity.getResources().getDisplayMetrics().density);
        campos.setPadding(margen, 0, margen, 0);

        EditText nombre = new EditText(activity);
        nombre.setSingleLine(true);
        nombre.setHint("Nombre");
        nombre.setText(user.getDisplayName());

        EditText usuario = new EditText(activity);
        usuario.setSingleLine(true);
        usuario.setHint("Nombre de usuario");

        CheckBox terminos = new CheckBox(activity);
        terminos.setText("Acepto los términos y condiciones");

        campos.addView(nombre);
        campos.addView(usuario);
        campos.addView(terminos);

        AlertDialog dialogo = new AlertDialog.Builder(activity)
                .setTitle("Completa tu perfil")
                .setMessage(correo)
                .setView(campos)
                .setNegativeButton("Cancelar", (d, which) -> fallar(uid, null))
                .setPositiveButton("Crear cuenta", null)
                .setOnCancelListener(d -> fallar(uid, null))
                .create();

        dialogo.setOnShowListener(d ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String nombrePerfil = nombre.getText().toString().trim();
                    String nombreUsuario = usuario.getText().toString().trim();

                    if (nombrePerfil.isEmpty()) {
                        nombre.setError("Ingresa tu nombre");
                        return;
                    }
                    if (nombreUsuario.isEmpty()) {
                        usuario.setError("Elige un nombre de usuario");
                        return;
                    }
                    if (!terminos.isChecked()) {
                        avisar("Debes aceptar los términos para continuar.");
                        return;
                    }

                    if (!esSesionActual(uid)) {
                        dialogo.dismiss();
                        fallar(null, "Vuelve a iniciar sesión con Google.");
                        return;
                    }

                    dialogo.setCancelable(false);
                    dialogo.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                    v.setEnabled(false);

                    io.execute(() -> {
                        try {
                            int id = repository.crearPerfil(
                                    uid, correo, nombrePerfil, nombreUsuario, true
                            );

                            enPantalla(() -> {
                                dialogo.dismiss();
                                if (esSesionActual(uid)) {
                                    entrar(id);
                                } else {
                                    fallar(null, "La sesión de Google cambió.");
                                }
                            });
                        } catch (IllegalArgumentException e) {
                            enPantalla(() -> {
                                usuario.setError(e.getMessage());
                                dialogo.setCancelable(true);
                                dialogo.getButton(AlertDialog.BUTTON_NEGATIVE)
                                        .setEnabled(true);
                                v.setEnabled(true);
                            });
                        } catch (IllegalStateException e) {
                            enPantalla(() -> {
                                dialogo.dismiss();
                                fallar(uid, e.getMessage());
                            });
                        } catch (RuntimeException e) {
                            Log.e("BidMarketAuth", "Error al crear el perfil local", e);
                            enPantalla(() -> {
                                dialogo.dismiss();
                                fallar(uid, "No se pudo crear el perfil.");
                            });
                        }
                    });
                }));

        dialogo.show();
    }

    private void entrar(int usuarioId) {
        preferencias().edit()
                .putInt("usuario_id", usuarioId)
                .putString("auth_provider", "GOOGLE")
                .apply();

        alEntrar.accept(usuarioId);
    }

    private void descartarSesionGoogle() {
        auth.signOut();
        preferencias().edit()
                .remove("usuario_id")
                .remove("auth_provider")
                .apply();
        activity.findViewById(com.example.bidmarket.R.id.main)
                .setVisibility(View.VISIBLE);
        bloquear(false);
        avisar("Vuelve a iniciar sesión con Google.");
    }

    private SharedPreferences preferencias() {
        return activity.getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
    }

    private boolean esSesionActual(String uid) {
        FirebaseUser actual = auth.getCurrentUser();
        return actual != null && uid.equals(actual.getUid());
    }

    private boolean pantallaActiva() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private void enPantalla(Runnable accion) {
        activity.runOnUiThread(() -> {
            if (pantallaActiva()) accion.run();
        });
    }

    private void bloquear(boolean bloquear) {
        ocupado = bloquear;
        for (View control : controles) {
            control.setEnabled(!bloquear);
        }
    }

    private void fallar(String uid, String mensaje) {
        if (uid != null && esSesionActual(uid)) auth.signOut();
        bloquear(false);
        if (mensaje != null) avisar(mensaje);
    }

    private void avisar(String mensaje) {
        Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show();
    }
}