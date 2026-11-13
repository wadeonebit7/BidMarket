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

/**
 * Coordina el acceso con Google y el perfil local de BidMarket+.
 * Busca un perfil existente, permite completarlo si falta y guarda la sesión
 * cuando la cuenta de Firebase coincide con la que se está procesando.
 */
public final class GoogleLoginFlow {

    private final AppCompatActivity activity;
    private final ExecutorService io;
    private final GoogleAccountRepository repository;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final IntConsumer alEntrar;
    private final View[] controles;

    private boolean ocupado;

    /**
     * Prepara el flujo de acceso para la pantalla que lo llama.
     *
     * @param activity pantalla que muestra el acceso y los avisos.
     * @param io ejecutor para consultar o guardar perfiles en segundo plano.
     * @param alEntrar acción que recibe el ID local cuando se completa el acceso.
     * @param controles vistas que se deshabilitan mientras se procesa el acceso.
     */
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

    /**
     * Inicia el acceso con Google si no hay otro intento en curso y la pantalla
     * sigue activa. Luego busca el perfil local asociado a la cuenta autenticada.
     */
    public void iniciar() {
        if (ocupado || !pantallaActiva()) return;

        bloquear(true);

        new GoogleAuthManager(activity).iniciar(new GoogleAuthManager.Callback() {
            /**
             * Busca el perfil de la cuenta autenticada. Si no existe y el correo está
             * verificado, solicita los datos necesarios para crearlo.
             *
             * @param user usuario autenticado en Firebase.
             */
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

            /**
             * Desbloquea los controles y muestra el error si la pantalla sigue activa.
             *
             * @param mensaje texto que explica por qué falló el acceso.
             */
            @Override
            public void onError(String mensaje) {
                enPantalla(() -> fallar(null, mensaje));
            }
        });
    }

    /**
     * Comprueba que la cuenta activa en Firebase corresponda al ID local guardado.
     * Si la sesión de Google ya no es válida, la descarta.
     *
     * @return {@code true} si la sesión guardada estaba marcada como Google y este
     *         flujo se encarga de revisarla. No indica que se haya restaurado.
     *         Devuelve {@code false} si la sesión no estaba marcada como Google.
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

    /**
     * Pide nombre, nombre de usuario y aceptación de los términos para crear
     * el perfil local. Comprueba que la cuenta de Google siga siendo la misma.
     *
     * @param user cuenta de Firebase cuyo perfil local se va a crear.
     */
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

    /**
     * Guarda el ID local y la marca de acceso con Google, y avisa a la pantalla
     * que el usuario ya puede entrar.
     *
     * @param usuarioId identificador del perfil en la base de datos local.
     */
    private void entrar(int usuarioId) {
        preferencias().edit()
                .putInt("usuario_id", usuarioId)
                .putString("auth_provider", "GOOGLE")
                .apply();

        alEntrar.accept(usuarioId);
    }

    /**
     * Cierra la sesión de Firebase, elimina los datos de sesión guardados
     * y vuelve a mostrar el formulario para que el usuario pueda intentar entrar.
     */
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

    /**
     * Obtiene las preferencias donde se guardan los datos de sesión.
     *
     * @return preferencias privadas de la aplicación usadas para la sesión.
     */
    private SharedPreferences preferencias() {
        return activity.getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
    }

    /**
     * Comprueba que la cuenta activa en Firebase sea la que se está procesando.
     *
     * @param uid identificador de Firebase de la cuenta que se quiere comprobar.
     * @return {@code true} si hay una cuenta activa y su identificador coincide.
     */
    private boolean esSesionActual(String uid) {
        FirebaseUser actual = auth.getCurrentUser();
        return actual != null && uid.equals(actual.getUid());
    }

    /**
     * Comprueba si la pantalla todavía puede recibir actualizaciones.
     *
     * @return {@code true} si la pantalla no se está cerrando ni fue destruida.
     */
    private boolean pantallaActiva() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    /**
     * Ejecuta una acción en el hilo de la interfaz si la pantalla sigue activa.
     *
     * @param accion tarea que se ejecuta en la pantalla.
     */
    private void enPantalla(Runnable accion) {
        activity.runOnUiThread(() -> {
            if (pantallaActiva()) accion.run();
        });
    }

    /**
     * Marca si hay un acceso en curso y habilita o deshabilita los controles.
     *
     * @param bloquear {@code true} para deshabilitar los controles, o {@code false}
     *         para volver a habilitarlos.
     */
    private void bloquear(boolean bloquear) {
        ocupado = bloquear;
        for (View control : controles) {
            control.setEnabled(!bloquear);
        }
    }

    /**
     * Termina el intento, desbloquea los controles y muestra un aviso si lo hay.
     * Solo cierra la sesión de Firebase si coincide con la cuenta indicada.
     *
     * @param uid identificador de la cuenta que se quiere cerrar, o {@code null} para no cerrarla.
     * @param mensaje aviso que se muestra, o {@code null} para no mostrarlo.
     */
    private void fallar(String uid, String mensaje) {
        if (uid != null && esSesionActual(uid)) auth.signOut();
        bloquear(false);
        if (mensaje != null) avisar(mensaje);
    }

    /**
     * Muestra un aviso en la pantalla.
     *
     * @param mensaje texto que se muestra al usuario.
     */
    private void avisar(String mensaje) {
        Toast.makeText(activity, mensaje, Toast.LENGTH_LONG).show();
    }
}