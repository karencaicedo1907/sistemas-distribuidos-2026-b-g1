# ADR-C3-02: Primary/Standby in two zones with PITR for Booking

## Status

Proposed

## Context

* Booking saves the reservations. This is the most important data in the system.
* Section 4 defines the recovery goals. The RPO is 5 minutes and the RTO is 30 minutes.
* The system needs two availability zones.

## Decision

* Use RDS for PostgreSQL from Huawei Cloud with one Primary and one Standby in two different availability zones. Also, use PITR.
* The Standby helps if one zone stops working. It can take the place of the Primary.
* PITR helps recover the database if someone makes a mistake or if some data is damaged.

## Alternatives considered

* Rejected: only PITR without a Standby. Without a Standby, it is necessary to restore a new database from a backup. This can take more time and may not meet the 30-minute RTO.
* Rejected: active-active in two zones. Both databases can save data. The unique index `uk_seat_hold_item_active_seat` works well with one writer, and with two writers the zones would need to coordinate to avoid two people booking the same seat. This is my design reason.
* Rejected: read replica in another zone. It helps with reading data while the Primary is working, but it does not take the place of the Primary to receive new writes if the Primary fails.

## Consequences

* Positive: The 30-minute RTO is possible as a design goal. PITR helps with the 5-minute RPO.
* Negative: This option costs more because it needs another database. This is related to the Cost part in Section 2.

Limitation: The 30-minute RTO and 5-minute RPO are goals. They have not been tested or measured yet.
