# Walkthrough - Aviso de Privacidad y Anonimato en CENTINELA

Se ha agregado el aviso de seguridad y privacidad solicitado para los reportes de acoso y violencia.

## Cambios Realizados

### 1. Interfaz de Usuario (`activity_chat.xml`)
- Se añadió un banner superior informativo (`tvPrivacyNotice`) indicando claramente: *"🔒 Tus comentarios son anónimos. Solo la Asociación de Mujeres podrá ver tus datos por seguridad."*

### 2. Lógica Funcional (`chat.java`)
- Se incluyó un aviso formal al inicio de la lista de reportes reiterando el compromiso de privacidad y el acceso exclusivo de la Asociación de Mujeres en situaciones que requieran apoyo.

## Validación y Pruebas
- **Compilación:** Se ejecutó una compilación exitosa (`gradle_build` con `app:assembleDebug`), finalizando sin errores.
