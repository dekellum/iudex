---
title: Duplicate Detection
layout: sub
---

## BruteFuzzy Service Interaction

Request  --> ADD SH1

service: Find *all* matches, Add SHA1

<-- Response SH1 matches: SH1 (i.e. already present) SH2 SH3

iudex-worker:
    Fetch SH1 SH2 SH3 (index)
    Decide survivor (SH1)
    Mark SH2 and SH3 as duplicates
    Send remove request

Request  --> REMOVE SH2 SH3

(no response)

## Fault tolerance:

Duplicate all requests to two in memory instances.
Responses only from an arbititrated master?
