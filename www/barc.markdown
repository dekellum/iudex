---
title: Iudex BARC
layout: default
---

Iudex Basic Archive (BARC)
==========================

![LARC LX](/img/BARC-LARC-XV-2-c.jpg)

ARC/WARC limitations
--------------------

* Uses text based Content-Length, so not possible to perform concurrent record reads.
* Many depdencies, too complicated.

BARC Format
-----------

A Human/Machine readable header used for first record (offset zero)
and each subsequent record:

{% highlight: text %}
Magic  rlength tt meta reqt resp
BARC1 FFFFFFFF HC FFFF FFFF FFFF(CRLF)
CRLF
{% endhighlight %}

"barc" utility use cases
------------------------

A. Display(Read) Record(s)

    barc infile
    barc -o 330391 -c 10 infile

* From BARC file (By absolute index?)
* Decompress compressed

(stored in db as integer, so use base-10 offset)

B. HTTP fetch

    barc [-z] [-t] url outfile

* Append or Truncate existing file
* Compress or not
* Display instead of write?


C. Copy/Concatenate record to file

    barc [-zt] infile outfile

* Compress target or not


(last file implicit write)

Human input/output to/from BARC?
--------------------------------

Format:

    @BARC1 H
    =META=
    Meta-Header-1: value
    Meta-Header-2: value

    =RQST=
    Request

    =RESP=
    Response

    =BODY=
    <html>
    ...
    ...
    ...



Other records
-------------

Sample DELETE:

    @BARC1 D
    =META=
    URL: http://foobar/framis
    UHASH: 98ASDz798a782hasfd79jbz

    @BARC R
    =META=
    URL: http://foobar/framis
    FROM: http://foobar
