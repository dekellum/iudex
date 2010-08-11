---
title: BARC
layout: sub
---

![LARC LX](/img/BARC-LARC-XV-2-c.jpg)[^cc]

The Iūdex BARC container format supports efficient block storage of
raw downloaded or post-processed content. The format was inspired by
Heritrix [ARC] and [WARC], but offers several unique
features/advantages:

* Concurrent and random-access reads by byte offset (external index).
* Single-writer sessions safe with concurrent reads.
* Per-record GZip compression (headers + body payload).
* Efficient random access channel IO.
* Minimal dependencies for read/write access.

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
header itself is 36 bytes long

Component | Description
:--------:| ------------------------------------------------------
rlength   | Length of record in hexadecimal bytes (not including header).
tt        | Type and compresssion bytes (see below).
meta      | Length of meta header block in hexadecimal bytes
rqst      | Length of request header block in hexadecimal bytes
resp      | Length of response header block in hexadecimal bytes

### Types

The following record types are currently defined:

 Type | Description
:----:| -----------
R     | Replaced (or never completed) record. Most consumers will want to ignore these records.
H     | HTML or other raw (web downloaded) content. For example, downloaded images would be suitable included as H values.
D     | A Delete record containing meta headers, but no body.

### Compression mode

The following compression modes are currently defined:

 Mode | Description
:----:| -----------
C     | Gzip compressed
P     | Plain (uncompressed)


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

    -BARC1 H
    =META=
    meta_header_1: value
    meta_header_2: value

    =RQST=
    Request-Line: GET /foo/bar.html

    =RESP=
    Status-Line: HTTP/1.1 200 OK

    =BODY=
    <html>
    ...

## Other records

Sample DELETE:

    -BARC1 D
    =META=
    url: http://foobar/framis
    uhash: 98ASDz798a782hasfd79jbz
