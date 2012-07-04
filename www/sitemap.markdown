---
title: Sitemap Parser
layout: sub
---

## Protocol

The [sitemap protocol] provides similar meta-data to feed formats:

~~~~
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
   <url>
      <loc>http://www.example.com/</loc>
      <lastmod>2005-01-01</lastmod>
      <changefreq>monthly</changefreq>
      <priority>0.8</priority>
   </url>
</urlset>
~~~~
{:lang="xml"}

Provide a SiteMapParser implementing ContentParser with, input:

* CONTENT_SOURCE
* URL
* etc.

Return iteration over child reference with:

* URL
* REF_PUB_DATE
* etc.

[sitemap protocol]:  http://www.sitemaps.org/protocol.php
