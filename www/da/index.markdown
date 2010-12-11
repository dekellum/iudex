---
title: DA
layout: sub
---

# Data Access

Iūdex maintains state on all content to be processed in persisent
storage. This includes all [prioritization] data. The current
implementation makes direct use PostgreSQL including unique features:

* A PostgreSQL-based content summary data store
* A politeness-aware persistent priority queue using SQL:2003
  Window Functions (PostgreSQL 8.4)
* JRuby/ActiveRecord based schema definition and migrations
* A high performance thread-safe Java JDBC-based and extensible Data Access Layer

Great care has been taken to isolate all PostgreSQL dependencies to
the iudex-da package. This should make is possible to experement with
alternativs such as fully distributed "nosql" stores in the future.

[prioritization]: {{ page.root }}/prioritization.html

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

    % ./iudex-da/bin/iudex-migrate

## Dependencies

Additional external dependencies:

{% svg ../svg/iudex-da.svg %}
