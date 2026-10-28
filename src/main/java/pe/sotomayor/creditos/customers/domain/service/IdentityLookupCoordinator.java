package pe.sotomayor.creditos.customers.domain.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.concurrent.locks.ReentrantLock;

import pe.sotomayor.creditos.customers.domain.exception.IdentityLookupException;

/**
 * Serializa consultas del mismo documento en esta instancia con memoria acotada.
 *
 * @author DarwinG
 */
public final class IdentityLookupCoordinator {
    private final ReentrantLock[] locks = new ReentrantLock[256];

    /**
     * Inicializa franjas de bloqueo; nunca almacena documentos ni nombres como claves.
     */
    public IdentityLookupCoordinator() {
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
    }

    /**
     * Ejecuta consulta y cache bajo la misma franja; no abre transacciones.
     */
    public <T> T execute(String documentNumber, Supplier<T> action) {
        var lock = locks[Math.floorMod(documentNumber.hashCode(), locks.length)];
        try {
            if (!lock.tryLock(8, TimeUnit.SECONDS))
                throw IdentityLookupException.rateLimited();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw IdentityLookupException.unavailable();
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
