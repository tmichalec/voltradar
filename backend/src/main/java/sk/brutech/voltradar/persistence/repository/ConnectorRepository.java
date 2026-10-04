package sk.brutech.voltradar.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sk.brutech.voltradar.persistence.entity.ConnectorEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConnectorRepository extends JpaRepository<ConnectorEntity, UUID> {
    Optional<ConnectorEntity> findByEvseId(String evseId);
    List<ConnectorEntity> findByEvseIdIn(List<String> evseIds);
}
