package pe.sotomayor.creditos.shared.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentAuditor implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {

        /*
         * Cuando implementemos JWT,
         * aquí obtendremos el ID del usuario autenticado.
         */

        return Optional.of(1L);

    }

}
