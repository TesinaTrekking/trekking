# El itinerario usa un orden explícito

Los checkpoints pertenecen a un único día operativo y se ordenan por hora; cuando comparten hora, conservan su posición explícita en el itinerario. Se elige una posición persistida en vez de depender de identificadores, porque los puntos nuevos aún no tienen identificador mientras se edita y el orden elegido por el guía debe ser estable. Al editar una salida se puede guardar también en la ruta activa un checkpoint nuevo; los demás cambios locales de checkpoints permanecen en la copia de la salida.
