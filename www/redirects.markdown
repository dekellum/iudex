---
title: Redirect Processing
layout: sub
---

Iūdex 1.0 supported only an `HTTPClient` _internal_ redirect mode,
where the URL change was recorded by the `ContentFetcher`. Iūdex 1.1+
supports an _external_ redirect mode, where:

* The `HTTPClient` implementation and `ContentFetcher` filter returns
  the redirect status and `Location` header (does not follow).
* A `RedirectHandler` filter resolves the redirect `Location`, and
  applies it to a new `REVISIT_ORDER` cloned from order.
* A `Revisitor` filter adds `REVISIT_ORDER` back into the `VisitQueue`.

This approach offers fine-grain filter-based control of the entire
redirect process, including the opportunity for early termination. In
addition, it gives the `VisitQueue` and `VisitManager` exclusive
control over host politeness constraints.

## Sample Redirect Scenario

Consider the following sequence of redirects:

{% svg svg/redirect_sample.svg %}

The following gives the processing sequence with `UniMap` order states.

### First order redirect

Start the initial order:

{% svg svg/redirect_orders_0.svg %}

A 302 status is returned from the `ContentFetcher`. The
`RedirectHandler` creates a `REVISIT_ORDER`, and adjusts `LAST`,
`REFERER`, and `REFERENT` values as show below:

{% svg svg/redirect_orders_0a.svg %}

Note that `PRIORITY` is also increased by a fixed per-redirect value
(configurable, 0.5 default), representing the value of work conducted
thus far and prioritizing the redirect over existing queued work.

The `Revisitor` then removes `REVISIT_ORDER` and resubmits it to the
`VisitQueue`, terminating the filter chain.

### Second order redirect

The _revisit_ order from above then becomes the next _order_:

{% svg svg/redirect_orders_1.svg %}

A second, 301 redirect is returned and the `RedirectHandler` applied:

{% svg svg/redirect_orders_1a.svg %}

### Third order

Again, the above _revisit_ order becomes the next _order_.  Finally, a
200 status is returned and the `RedirectHandler` applies no further
changes:

{% svg svg/redirect_orders_2.svg %}

Similarly, the `Revisitor` finds no `REDIRECT_ORDER` and passes. Note
that:

* The redirect path can be walked in reverse by following `LAST` references.
* `REFERER` always points to the beginning (first redirect) of the series.
* `REFERENT` always points to the end of the series.
