---
title: DA
layout: sub
---

# Data Access

* A PostgreSQL-based content summary data store
* A politeness-aware persistent priority queue using SQL:2003
  Window Functions
* JRuby/ActiveRecord based schema definition and migration
* A high performance thread-safe Java JDBC-based and extensible Data Access Layer

## Postgresql Setup

    % createuser iudex
    % createdb iudex_test -O iudex
    % iudex-migrate

## Dependencies

Additional external dependencies:

{% svg svg/iudex-da.svg %}
