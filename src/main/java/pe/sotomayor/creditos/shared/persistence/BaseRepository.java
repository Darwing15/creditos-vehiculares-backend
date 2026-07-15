package pe.sotomayor.creditos.shared.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Repositorio base para todas las entidades persistentes del sistema.
 *
 * <p>
 * Centraliza las operaciones comunes de acceso a datos y sirve como punto
 * de extensión para funcionalidades compartidas entre los distintos módulos.
 * </p>
 *
 * @param <T> tipo de entidad persistente administrada por el repositorio.
 *
 * @author DarwinG
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity>
        extends JpaRepository<T, Long> {

}
