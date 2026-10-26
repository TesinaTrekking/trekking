# La reserva de stock se valida dentro de una escritura serializada

La validación de disponibilidad y el guardado de una salida planificada deben formar una operación serializada dentro de la transacción. Así, dos instancias de la aplicación no pueden confirmar ambas una cantidad que individualmente parecía disponible pero que excede el stock total.
