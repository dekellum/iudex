---
layout: home
title: Home
keywords: Iudex Iūdex crawler feeds content processor
---

# Iūdex

Iūdex provides general purpose and extensible web feed aggregation and
web crawling facilities.  It is pronounced as the Latin
[/ˈjuː.deks/][wiki-ogg][^wik], meaning: judge or umpire, in reference
to its novel prioritization and filtering features.

Features:

* Filters
* [Prioritization](prioritization.html)
* [Data Access](da/index.html) (DA)
* [HTTP](http/index.html) abstraction and implementations
* [Redirect Handling](redirects.html)
* [Scalability and Fault Tolerance](distribution.html)
* [Basic Archive](barc.html) (BARC)
* Simhash Near Duplicate Detection
* HTML processing
* Feed processing

See also:

* [Development Guide](dev.html)

## Modules and Dependencies

Iūdex is constructed of modules with carefully controlled
dependencies. All modules are packaged as jruby gems. Many modules
include a java jar which is also made available as a maven artifact
for java dependency management.

{% svg svg/iudex.svg %}

## License

All Iūdex code and documentation is licensed under the
[Apache License, 2.0][AL2].

[wiki-ogg]: http://upload.wikimedia.org/wikipedia/commons/9/92/La-cls-iudex.ogg

[^wik]: Pronunciation and audio from
        [Wiktionary: Iudex](http://en.wiktionary.org/wiki/iudex)
        (Creative Commons Licensed)

[AL2]: http://www.apache.org/licenses/LICENSE-2.0
