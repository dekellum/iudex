---
title: Data Access
layout: sub
---

## Provides:

* A PostgreSQL-based content meta-data store
* JRuby/ActiveRecord based schema definition and migration
* A politeness-aware persistent priority queue using SQL:2003
  Window Functions
* A high performance thread-safe Java JDBC-based and extensible Data Access Layer

## Postgresql Setup

    % createuser iudex
    % createdb iudex_test -O iudex
    % (iudex-)migrate #FIXME: rename?

## Dependencies

Additional external dependencies:

{% svg svg/iudex-da.svg %}
