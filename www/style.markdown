---
title: Style Guide
layout: sub
---

## Ruby Code

* Avoid polluting the global namespace by a module hierarchy,
  i.e. <code>Iudex::Worker</code>

* Even with bin scripts, use the effectively private <code>module
  IudexBinScript</code> to contain <code>include</code>. This can seem
  a bit strange, but a valid and purposeful ruby:

  {% highlight ruby %}
  module IudexBinScript

    require 'rjack-logback'
    RJack::Logback.config_console

    require 'iudex-da'
    include Iudex

    #...
  end
  {% endhighlight %}

### Ruby Code Style

* 2 space indent, no tabs.
* 80 column width accept in exceptional circumstances.
* Use ASCII (or UTF-8, if you have to).
* Avoid spurious whitespace (more than a single consecutive empty
  line, trailing space)[^gt-cleanws]
* Liberal space around operators and internal to parens and braces, i.e.
  <code>method_call( x, y )</code>,
  <code>if ( a != b )</code>,
  <code>[ :goo, :bar ].map { |m| m.to_s }</code>
* Favor parenthesis for most method calls and less ambiguity.
* Use Unix-style line endings.
* Indent when as deep as case.
* Use RDoc and its conventions for API documentation. Don't put an
  empty line between the comment block and the def.
* Use empty lines to break up a long method into logical paragraphs.

## Java Code

* Java code is unaware of its ruby bootstrapping, or of ruby objects or
  the interpreter in general. Thus the general style of java is POJO
  with no special "configuration" awareness.
* Java is built using Maven, jar packaged, and included in Iūdex gems
  for distribution.
* However, Iudex jars can be built and utilized using standard Maven dependency
  management.
* Maven utilization is however purposely practically very simple.

### Java Code Style

* 4 space indent
* 80 column width accept in exceptional circumstances.

## Dependencies

Careful management of dependencies is core value in Iūdex. See the
overview [dependency diagram]. Avoid unconsidered dependency additions
beyond those shown.

## Versioning

Iūdex will continue to comply with [Semantic Versioning] (SemVer),
with the following narrowing constraints:

* The first "release" and current development is for 1.0.0. Per SemVer
  this requires backwards compatibility or bumping the MAJOR version in
  subsequent releases.
* No current plan to use "special" release versions aka
  alpha,beta,etc. as in my opinion this is often a distinction only
  visible in hindsight

* Releasing often and as necessary with concurrent MAJOR/MINOR release
  (stable vs less-stable lines[^jetty]

In development (and related to git):

* Topic branches should avoid including version changes, release notes, etc.
* Integration branches may include a version bump late or once it
  becomes clear if the sum of integrated changes is a MAJOR, MINOR, or
  PATCH level candidate.

[dependency diagram]: /index.html
[Semantic Versioning]: http://semver.org/

[^gt-cleanws]: See [gravitext-devtools](http://github.com/dekellum/gravitext-devtools)
               gem gt-cleanws for scripted cleanup.

[^jetty]: [Jetty](http://docs.codehaus.org/display/JETTY/Downloading+Jetty)
          is a fine example of this, with release grade branches 6.1.x
          (stable), 7.x (near-stable), and 8 (development).
