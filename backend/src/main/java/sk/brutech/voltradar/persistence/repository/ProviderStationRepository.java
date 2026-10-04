package sk.brutech.voltradar.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sk.brutech.voltradar.domain.model.CpoProvider;
import sk.brutech.voltradar.persistence.entity.ProviderStationEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderStationRepository extends JpaRepository<ProviderStationEntity, Long> {
    Optional<ProviderStationEntity> findByProviderAndProviderStationId(CpoProvider provider, String providerStationId);
    List<ProviderStationEntity> findByProvider(CpoProvider provider);
}
