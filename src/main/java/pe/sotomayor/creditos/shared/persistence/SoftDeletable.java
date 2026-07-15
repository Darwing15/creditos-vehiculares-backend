package pe.sotomayor.creditos.shared.persistence;

import java.time.LocalDateTime;

public interface SoftDeletable {

    Boolean getDeleted();

    void setDeleted(Boolean deleted);

    LocalDateTime getDeletedAt();

    void setDeletedAt(LocalDateTime deletedAt);

}
