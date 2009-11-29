---
title: Iūdex DA
layout: default
---

# Iūdex Data Access (DA)

Provides:

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

<div style="text-align: center;">
  <object data="svg/iudex-da.svg" type="image/svg+xml">
    <p>(<a href="svg/iudex-da.svg">SVG dependency diagram.</a>)</p>
  </object>
</div>
