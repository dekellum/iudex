---
title: BARC
layout: sub
---

# Basic Archive

The Iūdex BARC container format supports efficient block storage of
raw downloaded or post-processed content. The format was inspired by
Heritrix [ARC] and [WARC], but offers several unique
features/advantages:

* Concurrent and random-access reads by byte offset (external index).
* Single-writer sessions safe with concurrent reads.
* Per-record GZip compression (headers + body payload).
* Efficient random access channel IO.
* Minimal dependencies for read/write access.

![LARC LX](/img/BARC-LARC-XV-2-c.jpg)
[^cc]

[^cc]: From [Wikipedia: BARC-LARC-XV-2.jpeg](http://en.wikipedia.org/wiki/File:BARC-LARC-XV-2.jpeg)
       (public domain)

[ARC]:  http://crawler.archive.org/articles/developer_manual/arcs.html
[WARC]: http://www.digitalpreservation.gov/formats/fdd/fdd000236.shtml

## BARC Format

A Human/machine readable header used for first record (offset zero)
and each subsequent record:

{% highlight: text %}
Magic  rlength tt meta rqst resp
BARC1 FFFFFFFF HC FFFF FFFF FFFF(CRLF)
CRLF
{% endhighlight %}

All lengths are in hexadecimal bytes, zero-padded and fixed width. The
header 36 bytes.  The rlength does not include the header itself, so
offset to next record is rlength + 36 bytes.

## iudex-barc utility

A. Show Record(s)

    iudex-barc show barc/000000.barc
    iudex-barc show -mqr -o 0xde411 barc/000015.barc

* From BARC file (all or by record offset)
* Decompresses compressed records

(stored in db as integer, so use base-10 offset)

B. HTTP fetch

    iudex-http-record [-z] [-t] url outfile

* Append or Truncate existing file
* Compress or not
* Display instead of write?

C. Copy/Concatenate record to file

    iudex-barc [-zt] copy infile outfile

* Compress target or not

(last file implicit write)

## Human-readable to/from BARC

Format:

    @BARC1 H
    =META=
    meta_header_1: value
    meta_header_2: value

    =RQST=
    Request-Line: GET /foo/bar.html

    =RESP=
    Status-Line: HTTP/1.1 200 OK

    =BODY BEGIN=
    <html>
    ...
    =BODY END=

## Other records

Sample DELETE:

    @BARC1 D
    =META=
    url: http://foobar/framis
    uhash: 98ASDz798a782hasfd79jbz

    @BARC1 R
    =META=
    url: http://foobar/framis
    referent: 98ASDz798a782hasfd79jbz
