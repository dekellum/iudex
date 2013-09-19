---
title: Development Guide
layout: sub
---

* toc here
{:toc}

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
  hierarchy, i.e. `Iudex::Worker`

* Even with bin scripts, use the effectively private `module
  IudexBinScript` or similar to contain any required `include(s)`,
  locals. This may seem a bit strange, but it is valid and purposeful:

~~~~ ruby
module IudexBinScript

  require 'rjack-logback'
  RJack::Logback.config_console

  require 'iudex-core'
  include Iudex

  #...
end
~~~~

### Ruby Style

* 2 space indent, no tabs
* 80 column width except in exceptional circumstances
* Use ASCII, or UTF-8 if necessary
* Avoid spurious whitespace (more than a single consecutive empty
  line, trailing space)[^gt-cleanws]
* Liberal space around operators and internal to parentheses and
  braces
* Favor parentheses for multi- or complex argument method calls and
  less ambiguity
* Use Unix-style line endings
* Indent `when` as deep as `case`
* Use RDoc and its conventions for API documentation. Don't put an
  empty line between the comment block and the `def`
* Use empty lines to break up a long method into logical
  paragraphs. Then consider moving these paragraphs to new methods.

~~~~ ruby
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
~~~~

## Java Code

* Java code is unaware of its ruby bootstrapping, that an object may
  be ruby, or of the ruby interpreter in general.
* Write "plain old" Java objects (POJOs) with no special
  "configuration" awareness (beyond constructor arguments and setters
  as appropriate to keep the java code minimal.
* Java is built using simple Maven setups integrated via rjack-tarpit,
  jar packaged, and included in gems for distribution.
* The jars can be included per standard Maven dependency management
* Maven is limited to a javac (with dependencies) and jar
  packaging. Nothing fancy.
* Prefer testing in Ruby (for early accessibility, ruby test
  succinctness) unless there are compelling reasons to create java
  tests.

### Java Style

* 4 space indent
* 80 column width except in exceptional circumstances.  This is
  admittedly harder in Java, but a premium is placed on two (or 3-4)
  column code viewing on full size monitor.
* Use ASCII, or UTF-8 if necessary
* Avoid spurious whitespace (more than a single consecutive empty
  line, trailing space)
* Liberal space around operators and internal to parentheses and
  braces
* Use Unix-style line endings
* Avoid the meaning free `get` prefix for getters (jruby doesn't
  care.)  Use `set` for setters.
* Prefix member variables with '_' and use getters for external
  access.
* General class order is `public` to `private` (and thus member
  variables at the end.)

## Dependencies

Careful management of dependencies is a core value in Iūdex. See the
overview [dependency diagram]. Avoid unconsidered dependency additions
beyond those shown.

## Versioning

Iūdex will continue to comply with [Semantic Versioning] to the extent
supported with tools such as rubygems and maven, and with the
following narrowing constraints:

* The first release is is 1.0.0 at minimum. Per SemVer this requires
  backwards compatibility or bumping the MAJOR version in subsequent
  releases.
* The experiment with pre- (beta) releases in current development has
  not been particularly rewarding. We will likely avoid pre-releases
  in the future.
* Releasing often and as necessary with concurrent MAJOR/MINOR release
  (stable vs less-stable lines[^jetty]

## Git

Canonical Repository: [http://github.com/dekellum/iudex](http://github.com/dekellum/iudex)

Policies:

* Use topic branches
* Avoid including version changes or release notes, in topic
  branches. These changes should be made in release/integration
  branches.
* Integration branches should include version bumps early, but may
  change or be replaced, as it becomes clear if changes are MAJOR,
  MINOR, or PATCH level candidate.
* The master branch is reserved for tagged releases.

[dependency diagram]: /index.html
[Semantic Versioning]: http://semver.org/

[^gt-cleanws]: See [gravitext-devtools](http://github.com/dekellum/gravitext-devtools)
               gem gt-cleanws for scripted cleanup.

[^jetty]: [Jetty](http://docs.codehaus.org/display/JETTY/Downloading+Jetty)
          is a fine example of this, with release grade branches 6.1.x
          (stable), 7.x (near-stable), and 8 (development).
