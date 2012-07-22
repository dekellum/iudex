---
title: Data Access (DA)
layout: sub
---

Iūdex maintains state on all content to be processed in persisent
storage. This includes all [prioritization] data. The current
implementation makes direct use of PostgreSQL features for maximum
efficiency:

* A content metadata/summary data store
* A politeness-aware persistent priority queue using SQL:2003
  Window Functions (PostgreSQL 8.4 or higher)
* ActiveRecord based schema definition migrations
* JDBC-based custom data mapper between `UniMap` and database rows.
* Batch `SELECT FOR UPDATE`/`INSERT` with transaction isolation and
  conflict retry for maximum concurrency and throughput. One
  transaction for the current order, redirects and references.

All PostgreSQL dependencies are isolated in the iudex-da package. This
should make it possible to experiment with alternatives such as fully
distributed/partitioned _NoSQL_ stores in the future.

## Postgresql Setup

Postgresql should be setup with localhost trust access for the "iudex"
user (which need not itself have superuser credentials.) An
"iudex_test" database is used by tests and is the default for all
execution. Setup:

    % createuser iudex
    % createdb iudex_test -O iudex
    % iudex-migrate

If starting from a source tree, you will need to migrate the test
database before being able to build the iudex-da gem. The migration
can be run directly from the source tree:

    % jbundle exec ./iudex-da/bin/iudex-migrate

## Dependencies

Additional external dependencies:

{% svg ../svg/iudex-da.svg %}

[prioritization]: {{ page.root }}/prioritization.html
