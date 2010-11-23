---
title: Development Guide
layout: sub
---

* toc here
{:toc}

## Git

Canonical Repository: [http://github.com/dekellum/iudex](http://github.com/dekellum/iudex)

## License

All Iūdex code and documentation is licensed under the
[Apache License, 2.0][AL2] (local [LICENSE.txt]).

To avoid any ambiguities with intellectual property and enable
potential future transition to a foundation such as the Apache
Software Foundation, contributor agreements are required (mail, or
scan to PDF and email) before merging any contributions:

* [Individual Contributor Agreement](license/icla.txt)
* [Corporate Contributor Agreement](license/cla-corporate.txt)

[AL2]: http://www.apache.org/licenses/LICENSE-2.0
[LICENSE.txt]: license/LICENSE.txt

## Ruby Code

* Avoid polluting the global namespace by always using a module
  hierarchy, i.e. <code>Iudex::Worker</code>

* Even with bin scripts, use the effectively private <code>module
  IudexBinScript</code> to contain any required <code>include</code>'s. This may seem
  a bit strange, but is valid and purposeful:

{% highlight ruby %}
module IudexBinScript

  require 'rjack-logback'
  RJack::Logback.config_console

  require 'iudex-core'
  include Iudex

  #...
end
{% endhighlight %}

### Ruby Style

* 2 space indent, no tabs.
* 80 column width accept in exceptional circumstances.
* Use ASCII, or UTF-8 if necessary.
* Avoid spurious whitespace (more than a single consecutive empty
  line, trailing space)[^gt-cleanws]
* Liberal space around operators and internal to parens and braces.
* Favor parenthesis for most method calls and less ambiguity.
* Use Unix-style line endings.
* Indent `when` as deep as `case`.
* Use RDoc and its conventions for API documentation. Don't put an
  empty line between the comment block and the def.
* Use empty lines to break up a long method into logical paragraphs.

{% highlight ruby %}
  def adjust( map )

    priority = @factors.inject( @constant ) do | p, (w,func) |
      p + ( w * send( func, map ) )
    end

    if map.last_visit
      delta = ( map.status == 304 ) ? @min_next_unmodified : @min_next
    else
      delta = 0.0
    end

    [ priority, delta ]
  end
{% endhighlight %}

## Java Code

* Java code is unaware of its ruby bootstrapping, or of ruby objects or
  the interpreter in general. Thus the general style of java is POJOs
  with no special "configuration" awareness.
* Java is built using Maven, jar packaged, and included in Iūdex gems
  for distribution.
* However, Iudex jars can be built and utilized using standard Maven dependency
  management.
* Maven utilization is however purposely practically very simple.

### Java Style

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
* Integration branches may include a version bump, late or once it
  becomes clear if the sum of integrated changes is a MAJOR, MINOR, or
  PATCH level candidate.

[dependency diagram]: /index.html
[Semantic Versioning]: http://semver.org/

[^gt-cleanws]: See [gravitext-devtools](http://github.com/dekellum/gravitext-devtools)
               gem gt-cleanws for scripted cleanup.

[^jetty]: [Jetty](http://docs.codehaus.org/display/JETTY/Downloading+Jetty)
          is a fine example of this, with release grade branches 6.1.x
          (stable), 7.x (near-stable), and 8 (development).
