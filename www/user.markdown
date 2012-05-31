---
title: User Guide
layout: sub
---

* toc here
{:toc}

## Prerequisites

Before starting you need to have:

* JRuby 1.6 (tested with 1.6.7.2) – Available from [jruby.org](http://jruby.org)
* PostgreSQL (tested with 9.0.5) – Available from [postgresql.org](http://www.postgresql.org/)

## Install Gems

Install the gems from rubygems.org:

{% highlight console %}
$ gem install iudex
{% endhighlight %}

## Set Up A Database

Your Iudex application will need a PostgreSQL database for storage. The name of the database as well
as the username and password are up to you. In the examples from here on we'll use the user `crawler`
with the password `sic-semper-tyrannosaurus-rex` and a database named `crawl`.
To create a database like this you can use the commands:


{% highlight console %}
$ createuser -R -S -D -P crawler
Enter password for new role: [TYPE sic-semper-tyrannosaurus-rex HERE]
Enter it again: [TYPE sic-semper-tyrannosaurus-rex HERE]
Password: [TYPE YOUR SUPERUSER POSTGRESQL PASSWORD HERE]
$ createdb crawl -O crawler
Password: [TYPE YOUR SUPERUSER POSTGRESQL PASSWORD HERE]
$
{% endhighlight %}


## Create A Configuration File

There are a fair number of options for the configuration file. The configuration file is written in Ruby
which means that the configuration can be almost infinitely complex. An annotated example of a simple 
configuration file in included below and I saved this as `config.rb`.

{% highlight ruby linenos %}
{% include example/config.rb %}
{% endhighlight %}

## Initialize The Database

Now that you have a configuration file that Iudex can read (including the database connection information)
it's time to initialize the database. This can be done using the `iudex-migrate` command installed with
the gem:

{% highlight console %}
$ iudex-migrate -c config.rb
{% endhighlight %}

## Add A Feed To The System

You can start Iudex without any feeds but it's rather anticlimactic. Before starting the Iudex worker process
let's add a feed. Adding feeds is easy with the `iudex-da-import` script provided. Create a file with one feed
URL per line and use `iudex-da-import` like so:

### urls.txt

{% highlight text %}
{% include example/urls.txt %}
{% endhighlight %}

### Import Command

{% highlight console %}
$ iudex-da-import -c config.rb urls.txt
{% endhighlight %}

## Start The Iudex Worker

There is a script included for running a worker in the foreground. Depending on your environment this may or may not
be useful for ongoing crawls but it's very useful for testing. Starting there worker is as simple as:

{% highlight console %}
$ iudex-worker-fg -c config.rb
{% endhighlight %}

If everything worked correctly the worker should now be running. A bunch of stuff was probably printed to
screen so I've included a version of the log below. Here are some of the highlights:

* **Line 1-2**: Confirmation that we loaded the correct config file and the database is configured correctly
* **Line 4**: The message we configured in line 85 of `config.rb` (see above)
* **Line 5-79**: Cryptic messages that represent the filter chain. This is critical information when you customize more than we have.
  * *Worth nothing, the `i.f.FilterBase` on line 79 represents our filter from line 71 of `config.rb`*
* **Line 80**: Fetched the feed
* **Line 81**: Processing all of the pages referenced in the feed
* **Line 86-106**: The message we configured on line 77 of `config.rb`

{% highlight text linenos %}
{% include example/startup.log %}
{% endhighlight %}

## Output

It's not just all pretty log messages. There pages that have been fetched are stores in the `barc` directory (relative to wherever you
started `iudex-worker-fg`). The format is easily machine readable and there is a script to help out:

{% highlight console %}
$ iudex-barc -x show barc/000000.barc
{% endhighlight %}

## Going Forward

This brief introduction only gets you as far as a working system. The next step is to customize Iudex with your own content processing
or process the barc files as they are completed.


