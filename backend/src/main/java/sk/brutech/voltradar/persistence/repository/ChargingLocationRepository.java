package sk.brutech.voltradar.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sk.brutech.voltradar.persistence.entity.ChargingLocationEntity;

import java.util.List;

@Repository
public interface ChargingLocationRepository extends JpaRepository<ChargingLocationEntity, String> {

    @Query("SELECT DISTINCT l FROM ChargingLocationEntity l "
            + "LEFT JOIN FETCH l.providerStations ps "
            + "LEFT JOIN FETCH ps.connectors "
            + "LEFT JOIN FETCH l.chargerUnits "
            + "WHERE l.latitude BETWEEN :south AND :north "
            + "AND l.longitude BETWEEN :west AND :east")
    List<ChargingLocationEntity> findInBounds(
            @Param("south") double south,
            @Param("north") double north,
            @Param("west") double west,
            @Param("east") double east
    );

    @Query("SELECT DISTINCT l FROM ChargingLocationEntity l "
            + "LEFT JOIN FETCH l.providerStations ps "
            + "LEFT JOIN FETCH ps.connectors "
            + "LEFT JOIN FETCH l.chargerUnits")
    List<ChargingLocationEntity> findAllWithDetails();
}
