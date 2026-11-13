# BidMarket+

BidMarketPlus es una aplicación Android para publicar, comprar y vender productos de dos formas: **venta directa a precio fijo** y **subasta**. El objetivo es que el vendedor pueda crear una publicación con fotos y que el comprador pueda elegir entre comprar al precio indicado o hacer una oferta.

## Funciones principales

- **Acceso.** Registro e inicio de sesión con correo y contraseña. También se puede entrar con Google y vincular la cuenta a un perfil local.
- **Publicaciones.** Crear, editar, pausar y retirar publicaciones. Cada una tiene una categoría y entre 1 y 10 fotos tomadas con la cámara o seleccionadas de la galería. La primera foto de la lista se usa como portada.
- **Venta directa.** El comprador puede registrar una compra al precio publicado. La publicación queda marcada como vendida.
- **Subastas.** Los compradores pueden hacer ofertas durante el plazo de la subasta. Una nueva puja debe alcanzar el precio inicial y superar la mayor oferta registrada.
- **Chat y avisos.** El historial guarda conversaciones entre comprador y vendedor. En una subasta, el chat queda disponible después del cierre si la subasta no fue cancelada. Los avisos se consultan dentro de la app.
- **Mapa y sensores.** La publicación puede incluir coordenadas para mostrarlas en Google Maps. La pantalla del mapa también muestra las lecturas del acelerómetro si el dispositivo cuenta con ese sensor.

> **Alcance actual.** Las publicaciones, las compras, las pujas, los chats y los avisos se guardan en SQLite en el dispositivo. El acceso con Google usa Firebase Authentication, pero eso no sincroniza los datos entre teléfonos. La app no procesa pagos ni envía notificaciones push.

## Tecnologías y organización

El proyecto está desarrollado en **Java** con Android Studio. La interfaz usa **XML y Material Design**. Gradle administra la compilación, **SQLiteOpenHelper** administra la base de datos y **Credential Manager** conecta el acceso con Google a Firebase Authentication. Para el mapa y la ubicación se usan **Maps SDK for Android** y **Fused Location Provider**.

| Paquete | Responsabilidad |
| --- | --- |
| `activities` | Pantallas y navegación de la app. |
| `adapters` y `models` | Presentación de las tarjetas y datos del catálogo. |
| `auth` | Acceso con Google y vínculo con el perfil local. |
| `repositories` | Consultas y operaciones sobre los datos de la app. |
| `database` | Contrato, creación y migraciones de SQLite. |
| `utils` | Generación y verificación de hashes de contraseñas. |

## Instalación y ejecución

1. Clona el repositorio y abre la raíz del proyecto en Android Studio.
2. Instala el **SDK de Android API 37**, que es el SDK de compilación configurado en el proyecto. Para ejecutar la app basta un dispositivo o emulador con **API 30 o superior**. Para probar Google y Maps, usa un dispositivo o emulador con Servicios de Google Play.
3. Configura **Firebase** para el paquete `com.example.bidmarket`. Agrega la huella SHA-1 de la firma con la que ejecutarás la app, habilita Google en **Authentication → Métodos de acceso** y coloca el archivo actualizado `google-services.json` en `app/`. Puedes obtener la huella desde PowerShell con `.\gradlew.bat signingReport`. Consulta la [guía de acceso con Google en Firebase](https://firebase.google.com/docs/auth/android/google).
4. Habilita **Maps SDK for Android** en Google Cloud y agrega la clave a `local.properties`, en la raíz del proyecto. Usa una clave separada para Maps y restríngela al SDK, al nombre del paquete y a la huella de la firma. No subas `local.properties` al repositorio. Consulta la [guía de configuración de Maps](https://developers.google.com/maps/documentation/android-sdk/get-api-key).

   ```properties
   MAPS_API_KEY=TU_CLAVE_DE_MAPS
   ```

5. Sincroniza Gradle, selecciona la configuración `app` y presiona **Run**.

## Cómo probar el flujo principal

1. Entra con una cuenta de vendedor. Crea una publicación de venta directa, toma una foto y guárdala como portada.
2. Cierra sesión y entra con otra cuenta **en el mismo dispositivo**. Compra el producto y comprueba que la publicación quede vendida.
3. Crea otra publicación como subasta. Desde otra cuenta, registra una oferta durante el plazo y comprueba que la app rechace una puja que no supere la mayor oferta.

## Seguridad

- Los repositorios usan consultas parametrizadas y `ContentValues` para guardar datos en SQLite.
- Las contraseñas locales se guardan como hashes **PBKDF2**, no como texto plano.
- Las pantallas internas no se exportan a otras apps. La cámara recibe una URI de contenido mediante `FileProvider` y el inicio de sesión aplica `FLAG_SECURE` para bloquear capturas.

## Documentación técnica

- [Diagramas de navegación y flujo](docs/diagramas.md)
- [Contrato de tablas y columnas de SQLite](app/src/main/java/com/example/bidmarket/database/BidMarketContract.java)
- [Creación y migraciones de la base de datos](app/src/main/java/com/example/bidmarket/database/BidMarketDbHelper.java)
- [Código fuente con JavaDoc](app/src/main/java/com/example/bidmarket)

El modelo entidad-relación actualizado se incorporará cuando esté listo. Mientras tanto, el contrato y el helper son la referencia del esquema implementado.
