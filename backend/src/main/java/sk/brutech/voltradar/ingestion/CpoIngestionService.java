package sk.brutech.voltradar.ingestion;

/** Read-only provider boundary; wire models remain owned by the individual adapter. */
public interface CpoIngestionService<S, T> {
    S fetchStation(String stationId);

    T fetchTariffs();
}
