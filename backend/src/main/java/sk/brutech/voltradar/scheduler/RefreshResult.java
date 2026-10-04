package sk.brutech.voltradar.scheduler;

import java.time.Instant;

public record RefreshResult(
        Instant executedAt,
        long durationMs,
        int locationsCount,
        int providerStationsCount,
        int connectorsCount,
        String status,
        String message
) {
    public static RefreshResult success(
            Instant executedAt,
            long durationMs,
            int locationsCount,
            int providerStationsCount,
            int connectorsCount
    ) {
        return new RefreshResult(
                executedAt,
                durationMs,
                locationsCount,
                providerStationsCount,
                connectorsCount,
                "SUCCESS",
                "Successfully refreshed and persisted charging infrastructure and live statuses."
        );
    }

    public static RefreshResult failed(
            Instant executedAt,
            long durationMs,
            String errorMessage
    ) {
        return new RefreshResult(
                executedAt,
                durationMs,
                0,
                0,
                0,
                "FAILED",
                errorMessage
        );
    }
}
