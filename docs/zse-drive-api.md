# ZSE Drive public API investigation

Observed on 2026-09-28 using unauthenticated GET requests to <https://zsedrive.sk>.
Sources: [public map](https://zsedrive.sk/mapa), its Next.js JavaScript bundles
(`map-4e8c84ca4d0e76ed.js`, `761-b58246f1aa117aba.js`, `_app-e6b7c0b89b24001a.js`),
and [official tariff PDF effective 2026-05-01](https://zsedrive.sk/api/web/v1/files/downloadFiles/Cennik%20sluzby%20ZSE%20Drive_public_onepage_20260501-1775743089623.pdf).
These are observed web application endpoints, not a documented stable integration contract.

## Endpoints

| GET path | Parameters | Response |
| --- | --- | --- |
| `/api/web/v1/stations` | Viewport coordinates or `query`; `limit`; optional filters | `places`, `grid`, `list`, `count` |
| `/api/v4.7/stations/{id}` | Numeric station identifier from `list` | `station` with connectors and live states |
| `/api/v4.2/connector-types` | None | `connectorTypes` |
| `/api/v4.7/program` | `partial=true&b2c=true` | `programs`, `homePrograms`, `addendum` |

No login, API key or bearer token was needed. The client sends `Accept: application/json`,
`Accept-Language: sk` and `language: sk`, following the website's language headers.
The web code also supports `b2c=false`; the captured program fixture is the verified B2C response.

## Location queries and synchronization

This is request/response JSON, not a streaming feed. No push or change cursor was identified in the inspected map code.
The map loads data after a viewport change with a 300 ms debounce; that is not a live-status polling interval.

Viewport parameters are `gpsNorthWestLat`, `gpsNorthWestLon`, `gpsSouthEastLat`, `gpsSouthEastLon`.
The website uses `limit=40` for a viewport and `query=<text>&limit=20` for search.
Calling the map endpoint without a viewport or query returned HTTP 500 during investigation.

Array filters use `contractor[]=ZSE` and `category[]=DC` (repeat for multiple values).
The web application exposes contractor filters `ZSE`, `HOME_ROAMING`, `FOREIGN_ROAMING`,
and categories `AC`, `DC`, `UFC`, `STANDARD`, `RESIDENT`, `DRIVEX`.
The `ZSE` filter was verified live; other filter values were identified in the JavaScript.
Do not filter returned data using `contractor == "ZSE"`: own/partner stations observed in that query return `EON`.

Example around central Bratislava:

```text
GET /api/web/v1/stations?gpsNorthWestLat=48.16&gpsNorthWestLon=17.09
    &gpsSouthEastLat=48.14&gpsSouthEastLon=17.12&limit=40&contractor[]=ZSE
```

That request returned `count=16`, 16 station summaries and no clusters.
The Slovakia-sized viewport (north 49.7, west 16.8, south 47.7, east 22.6) with the ZSE filter returned
`count=609`, only 40 summaries, and clusters. Without that filter the same viewport returned `count=2913`.
Text search for Bratislava returned 181 summaries despite `limit=20`: the limit is not a guaranteed page size.
Counts and availability are observations at capture time, not constants.

Response shape:

```json
{
  "places": [],
  "grid": [{"location": {"lat": 48.13, "lon": 17.13}, "count": 117}],
  "list": [{"id": 459600, "name": "...", "contractor": "EON",
            "location": {"lat": 48.08653, "lon": 17.09846},
            "connectors": 2, "freeConnectors": 2, "exeStationType": "DriveX"}],
  "count": 609
}
```

`grid` contains aggregated markers with a location and station count, not station IDs or details.
`places` contains geocoding suggestions, not charging stations. `list` contains actual station summaries.
No verified offset/cursor pagination or nearest-first sorting was found. Observed `distance` values were null.

Recommended next ingestion layer for the requested nearby use case:

1. Query a small bounding rectangle around the user's position; refresh when the position changes materially.
2. Check that `grid` is empty and `list.size() == count`. If incomplete, subdivide the viewport with bounded
   depth/request count, merge by station ID, and explicitly retain an incomplete flag when limits are reached.
   Increasing `limit` alone is not a proven completeness strategy.
3. Persist locations and use PostGIS `ST_DWithin` and distance ordering for an actual radius/nearest query.
   A rectangular map query alone does not guarantee the closest N stations.
4. Fetch details only for selected candidates. Proposed starting policy: refresh visible candidate states every
   30–60 seconds, cache with a short TTL and record a local observation timestamp. This interval is our proposal,
   not an upstream freshness guarantee or documented rate limit. Expired data must become stale/unknown.
5. Refresh relatively static metadata less often and tariffs daily. Back off on 429/5xx; do not treat failures
   or partial viewport results as station deletions. Bound request concurrency.

The implemented client performs individual reads; scheduling, persistence, cache, recursive viewport discovery
and nearest-station ranking are separate ingestion work. It never silently returns an empty list on API failure.

## Station and connector formats

`StationSummary.connectors` is a **number**, whereas `Station.connectors` is an **array**.
Coordinates are `location.lat` and `location.lon` (WGS84). Address fields may be missing or null;
observed country codes include `SK` and `SVK`. `exeStationType` can be `AC`, `DC`, `DriveX` or null.
Keep provider values rather than treating the observed samples as an exhaustive enum.

The detail has a `station` envelope, with `id`, `remoteID`, `evseID`, `name`, `note`, `contractor`,
`openHours`, `cover`, `canUserCharge`, `location`, `rating`, `address`, `connectors`,
`exeStationType`, and `isPaidParking`. Some fields are absent in roaming details.
`openHours` was null in the sampled native stations; a non-null structured schedule has not been verified.

Connector example from station 459600:

```json
{
  "id": 1,
  "evseID": 310437,
  "state": "AVAILABLE",
  "type": {"id": 4, "name": "CCS", "socketType": "DC", "scheme": "https://..."},
  "pricing": [
    {"name": "Výkon", "value": "400 kW"},
    {"name": "Cena", "value": "0.79 €/kWh"},
    {"name": "Parkovanie", "value": "3 €/hod"},
    {"name": "Bezplatné parkovanie", "value": "60 min"}
  ],
  "nightTariff": null,
  "subscribed": false
}
```

Power is a localized display line, not a numeric `maxPower` property. Preserve raw pricing lines.
`ZseConnectorValues.maxPowerKw` conservatively parses `Výkon`/`Power` lines ending in `kW`.
Missing or unrecognized values return `Optional.empty()`, never zero.
Identifiers are scoped: use provider + station ID + connector ID; two plugs can share one EVSE
(observed CCS and CHAdeMO at station 283373). Thus available plugs are not necessarily simultaneous sessions.

The connector catalog returns CCS=4, CHAdeMO=5 and Mennekes Type 2=2.
Roaming detail 440826 returns CCS with type ID **1**. Normalize using the observed names,
not a universal numeric ID mapping. The domain intentionally normalizes only `CCS` and
`MENNEKES_TYPE_2`; CHAdeMO and unknown type names remain unmapped in domain calculations while
the raw provider type remains available in the DTO. CCS is classified by connector type even when
its advertised power is low (for example, a 24 kW roaming CCS connector).

The map JavaScript recognizes `AVAILABLE`, `BUSY`, `DISCONNECTED`, `RESERVED`.
The captured details returned `AVAILABLE`; this does not validate upstream timing or every state transition.
Normalization maps AVAILABLE to AVAILABLE and BUSY/RESERVED to OCCUPIED.
DISCONNECTED and new/null values map to UNKNOWN; disconnection alone does not prove a hardware fault.
Raw states remain in the DTO. Responses do not provide a verified upstream status timestamp.

## Programs and tariff interpretation

Program IDs are UUID strings. Monetary values and included monthly volume use `BigDecimal`.
`freeMonthlyCharging` can be null. `pricing` contains numeric uppercase properties:
`AC_CHARGING`, `DC_CHARGING`, `UFC_CHARGING`, `DRIVEX_CHARGING`, `PARKING`,
`MONTHLY_FEE`, `RFID_FEE`, `FOREIGN_RFID_ISSUE`.
Each charging category has a `*_NIGHT_TARIFF` property, either null or an object:

```json
{"name": "Nočná cena", "amount": 0.24,
 "starthour": 22, "startminute": null, "stophour": 6, "stopminute": null}
```

Observed public programs: ECO, START, PARTNER SAFE, ROAD SAFE, FLAT SAFE, PRO SAFE;
`homePrograms` separately contains HOME ECO and HOME PARTNER. Preserve `addendum`, notes and consent flags.
Unknown JSON properties are tolerated. Missing required response envelopes are rejected.

The PDF establishes semantics that the JSON alone does not encode:

- Prices include VAT; charging rates are EUR/kWh, monthly fees EUR/month, included volume kWh.
- GUEST rates (AC 0.49, DC 0.59, Ultra 0.69, DriveX 0.79) match sampled unauthenticated native details.
  GUEST was absent from the captured program list. Do not apply detail prices as the user's subscription rate.
- Ultra starts at **100 kW**, not 150 kW. DriveX is a marked site category, not just a power threshold.
- Night pricing applies according to **session start** between 22:00 and 06:00, regardless of session end.
  A calculation engine must not split a session into day/night portions merely from these fields.
- Parking is **EUR 3 per started hour** after the grace period, including while charging.
  It is not automatically a prorated per-minute fee. Grace periods: residential AC 720 min,
  other AC 180 min, DC 90 min, Ultra/DriveX 60 min; use the station's displayed grace period.
- Included volume excludes domestic and foreign roaming. Their separate rates are specified in the PDF;
  the public program response is not a complete machine-readable roaming tariff model.
- `isPaidParking` describes additional third-party site parking; it does not replace charging overstay fees.
- Account balance, remaining prepaid volume and personalized contracts are not exposed by these public reads.

## Backend usage and validation

`ZseDriveClient` is a Spring bean behind the generic `CpoIngestionService` boundary.
Configuration: `integrations.zse-drive.base-url` (`ZSE_DRIVE_BASE_URL`), `connect-timeout` (5s),
`read-timeout` (15s). No automatic retries or background polling are started.

```java
var nearby = client.fetchStations(ZseStationQuery.viewport(
        new ZseStationQuery.Bounds(48.16, 17.09, 48.14, 17.12)));
var detail = client.fetchStation("459600");
var programs = client.fetchTariffs();
var connectorTypes = client.fetchConnectorTypes();
```

Fixtures in `backend/src/test/resources/zse` are captured public responses from this investigation.
Offline tests cover actual response decoding, decimal/night prices, shared EVSEs, roaming connector IDs,
viewport parameters, encoded search text, unknown fields/states, bad input, malformed responses and HTTP failures.
Run `mvn -f backend/pom.xml test` with JDK 25. Live API access is not required by the tests.
