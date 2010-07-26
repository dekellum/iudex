---
title: Scalability and Fault Tolerance
layout: sub
---

## Fault Tolerance

The *primary* source of web content state is the web itself. Subject
to certain limitations, if iudex-worker BARC output or visit database
state is lost due to hardware failure, it may be recovered by bringing
a new worker online from a previous backup and allowing it to catch up
to the current state of the web. The most significant limitation to
recovering state is the length of time that feeds retain references to
content.  Since some, generally higher volume new feeds, keep articles
referenced for only a few days, daily backups and intervention times on
the order of one day should be sufficient.

### General Recommendations

* Backup visit database(s) at least once per day (or continuously via
  Postgres Warm Standby/PITR)
* Write completed BARC files to redundant storage (i.e. Amazon S3,
  HDFS, etc.)

_TODO_: Open BARC files would presumably be lost on iudex-worker failure
since they would not yet be written to redundant storage. A utility
should be written to validate BARC state against the visit database
and mark lost writes accordingly (i.e. schedule new visit.) The
alternative would be to support streaming/synchronous BARC writes to
redundant storage. But this would likely be costly.

## Scalability

### Host Partitioning

Partitioning the visit queue on URL-extracted hosts (i.e. hash of host
name modulo number of partitions) allows some content uniqueness and
host politeness concerns to be localized to a single iudex-worker
instance.

Note however that HTTP redirects are in common-usage with web feeds,
including redirects that span hosts. For example, a feed hosted at
feedburner.google.com may advertise new content at URLs of host
feedburner.google.com (for tracking, etc.). When accessed however
these URLs redirect to original publisher URLs on a different host,
i.e. "gravitext.com."

## Distribution Models

The following distribution models have implications for both
scalability and fault tolerance.

### Single database and worker

The simplest model is a single visit database and worker pair:

{% svg svg/distribute-single.svg %}

_TODO_: this is the only model currently supported.

The visit database and iudex-worker may be on the same host (lowest
latency access) or different hosts (more resources for
potentially greater throughput.)

### Centralized database with multiple workers

In this model the visit database remains a centralized single instance,
but additional iudex-worker instances are introduced:

{% svg svg/distribute-central.svg %}

When polling work, each iudex-worker selects only visit URLs with
hosts matching its unique hash offset. However, all discovered URLs
(references from feeds, etc.) are updated in the central
database. Thus the visit database itself provides the mechanism for a
feed processed by iudex-worker-1 to reference content that will be
hashed to, and eventually polled and processed by iudex-worker-2.

### Partitioned databases and workers

{% svg svg/distribute-partitioned.svg %}

Note that combinations of the multiple workers and partitioned
(share-nothing) models are also possible.
