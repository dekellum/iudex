---
title: Prioritization and Work Queues
layout: sub
---

The `VisitManager` of [iudex-core] contains the central thread pool,
work prioritizer and politeness enforcer. Prioritization aspects are
handled both via an external `WorkPollStrategy` implementation and in
internal (in memory) structure of host and visit queues.

## In-memory Visit/Host Priority Queues

The [iudex-core] module includes a prioritized visit queue and
executor with the following features:

* Per-host fetch rate limiting for politeness.

* `HostQueue(s)` containing visit orders for the same host, to be
  processed in priority order.

* A `VisitQueue` of ready and sleeping (delay for politeness)
  `HostQueue(s)`. The ready queue is prioritized by `HostQueue` topmost
  priority. The sleeping queue is ordered by least next visit time.

* A custom threaded, concurrent `VisitManager` for processing the
  `VisitQueue` while upholding host politeness constraints.

* A `ThreadPoolExecutor` executes `VisitTask(s)` which are simply
  `UniMap` orders run through a `FilterContainer`. Tasks may block if
  using blocking [HTTP][iudex-http] implementation or be short lived,
  reentrant with an asynchronous implementation.

* A pluggable `WorkPollStrategy` (see below)

* A `GenericWorkPollStrategy` including support for minimum poll
  interval, and minimum remaining ratios of orders and hosts before
  new work is polled.

* The `VisitManager` supports generations of VisitQueue(s) and visitor
  threads, for high concurrency, and avoiding over-commitment to any
  single host.

## Postgres-backed persistent WorkPollStrategy

The [iudex-da] modules provides a `WorkPoller` implementation of
`WorkPollStrategy` which obtains prioritized visit orders from a
Postgres database. Features:

* Only visit orders (urls) with `NEXT_VISIT_AFTER` the current time are
  considered.

* Visit orders are considered in descending `PRIORITY` order.

* The priority of the highest orders associated with a each host (by
  URL) is discounted by a fixed factor of per-host depth. This biases
  the work polled toward greater breadth of hosts and thus concurrency
  of execution, given the per-host politeness constraint.

* SQL 2003 Window Functions are utilized for efficient calculation of
  host depth priority adjustments.

[iudex-core]: index.html
[iudex-da]: da/index.html
[iudex-http]: http/index.html
