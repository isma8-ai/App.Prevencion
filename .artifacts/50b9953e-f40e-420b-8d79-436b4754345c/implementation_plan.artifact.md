# Plan de Implementación - Chat Anónimo (CENTINELA)

Este plan se enfoca exclusivamente en el desarrollo funcional del **Chat Anónimo** solicitado por el usuario, utilizando las pantallas existentes (`chat.java` y `activity_chat.xml`).

## Enfoque Principal: Chat Anónimo

1. **Pantalla de Chat (`chat.java` y `activity_chat.xml`):**
   - Adaptar o integrar los componentes funcionales para permitir enviar y recibir mensajes de forma anónima (por ejemplo, con un `EditText` para escribir, un botón de enviar y un listado/`ListView` o `RecyclerView` para mostrar la conversación).
   - Conectar los mensajes (opcionalmente usando Firestore o una lista local en tiempo real si es requerido para el chat anónimo).
   - Mantener el botón existente (`button12` / retorno a `MainActivity`).

---

## Tareas del Plan

1. **Actualizar el diseño y lógica de `chat.java` / `activity_chat.xml`:**
   - Agregar componentes de entrada de texto y visualización de mensajes (o programarlos dinámicamente si es necesario para respetar el layout base).
   - Implementar la lógica para enviar mensajes anónimos.
2. **Pruebas del Chat Anónimo:**
   - Verificar el envío, recepción y visualización correcta de los mensajes en la interfaz.

---

## Verificación Plan
- Compilar el proyecto para verificar que no existan errores de sintaxis en Java.
- Pruebas funcionales de envío de mensajes en el chat anónimo.
