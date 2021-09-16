# Android - Sensor de proximidad y módulo HC05

Este repositorio contiene el codigo de la applicación de Android mostrada como ejemplo para la cátedra R4001 de MEdidas Electrónicas I de UTN FRBA. La idea de este codigo es mostrar las siguientes funcionalidades:

- Medición de proximidad.
- Conectividad con dispositivo bluethooth "Serial Port Profile" (especificamente un HC05).
- Lectura y escritura de datos por bluethoot.
- Permanencia de recursos en background.
- Checkeo de recursos.

El repositorio contiene además el código de Arduino, el cual sirve de interface con el HC05.

El codigo esta bien comentado para que sea facil de seguir.

### Ejemplos de la App

La app se ve asi:

![Ejemplo de pantalla de la App] (./assets/screenshot.jpg?raw=true "App")

Arriba se ve el estado de la conexion (en este caso ya conectado), debajo se muestra lo recibido por bluethoot (en este caso el texto "¡Hola!"). Más abajo hay un icono con una "X" que responde a la recepcción de las palabras clave "on" y "off". Al costado de este icono se encuentra el switch que indica si la conexion bluethoot continuará si se retia el foco a la App. Finalmente abajo se ve una lista de dispositivos bluethoot apareados, haciendo click sobre los elementos de esta lista hará que la App intente conectarse a dicho dispositivo.
